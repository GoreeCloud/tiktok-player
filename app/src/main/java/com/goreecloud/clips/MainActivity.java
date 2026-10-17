package com.goreecloud.clips;

import android.app.Activity;
import java.util.concurrent.atomic.AtomicBoolean;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
import android.view.ViewConfiguration;
import android.view.Window;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.graphics.Insets;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
    private static final String HOME = "https://www.tiktok.com/";
    private static final String PREFS = "goreecloud_clips_prefs";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean downloading = new AtomicBoolean(false);
    private final Handler main = new Handler(Looper.getMainLooper());
    private SharedPreferences settings;
    private BookmarkStore bookmarks;
    private HistoryStore recent;
    private String savedQuery = "";
    private String recentQuery = "";
    private WebView browser;
    private LinearLayout shell;
    private String currentTab = "browse";
    private Dialog contextSheet;
    private Runnable pendingLongPress;
    private float pressStartX, pressStartY;
    private boolean longPressActivated;
    private String cleanWebScript = "";
    private boolean dark;
    private int background, surface, foreground, secondary, border, accent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        bookmarks = new BookmarkStore(this);
        recent = new HistoryStore(this);
        updateKeepAwake();
        dark = settings.getBoolean("dark", false);
        background = Color.parseColor(dark ? "#111522" : "#F7F8FB");
        shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(background);
        // Android 15+ enforces edge-to-edge for target SDK 35. Give the content its
        // own safe inset space instead of allowing the header under the status bar.
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() |
                        WindowInsets.Type.displayCutout());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right,
                        Math.max(bars.bottom, ime.bottom));
                return WindowInsets.CONSUMED;
            });
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(shell);
        applyColors();
        if (Build.VERSION.SDK_INT >= 30) shell.requestApplyInsets();
        cleanWebScript = loadCleanWebScript();
        buildWebView();
        String incoming = incomingUrl(getIntent());
        if (incoming != null) browser.loadUrl(incoming);
        else browser.loadUrl(HOME);
        renderBrowse();
        if (!settings.getBoolean("gesture_discovered", false)) {
            settings.edit().putBoolean("gesture_discovered", true).apply();
            browser.postDelayed(() -> toast("Press and hold a video to open GoreeCloud tools."), 950);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String incoming = incomingUrl(intent);
        if (incoming != null) {
            browser.loadUrl(incoming);
            renderBrowse();
        }
    }

    @Override protected void onDestroy() {
        cancelLongPress();
        if (contextSheet != null) {
            contextSheet.dismiss();
            contextSheet = null;
        }
        executor.shutdownNow();
        if (browser != null) {
            ViewGroup parent = (ViewGroup) browser.getParent();
            if (parent != null) parent.removeView(browser);
            browser.destroy();
        }
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (!"browse".equals(currentTab)) { renderBrowse(); return; }
        if (browser.canGoBack()) { browser.goBack(); return; }
        super.onBackPressed();
    }

    private void updateKeepAwake() {
        if (settings != null && settings.getBoolean("keep_awake", false))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void applyColors() {
        background = Color.parseColor(dark ? "#111522" : "#F7F8FB");
        surface = Color.parseColor(dark ? "#1B2130" : "#FFFFFF");
        foreground = Color.parseColor(dark ? "#F6F7FF" : "#202738");
        secondary = Color.parseColor(dark ? "#BAC3D7" : "#667185");
        border = Color.parseColor(dark ? "#374052" : "#E1E5EE");
        accent = Color.parseColor(dark ? "#A5B8FF" : "#3156CB");
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarContrastEnforced(false);
            int flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            if (getWindow().getInsetsController() != null)
                getWindow().getInsetsController().setSystemBarsAppearance(dark ? 0 : flags, flags);
        } else {
            getWindow().setStatusBarColor(background);
            getWindow().setNavigationBarColor(background);
            getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        if (shell != null) shell.setBackgroundColor(background);
    }

    private GradientDrawable rounded(int fill, int outline, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), outline);
        return d;
    }

    private int dp(int value) { return (int) (getResources().getDisplayMetrics().density * value + .5f); }

    private TextView text(String label, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private View settingsAction(String label, int unusedGlyph, Runnable run) {
        LinearLayout line = row();
        line.setPadding(dp(15), dp(9), dp(15), dp(9));
        line.setMinimumHeight(dp(52));
        line.setBackground(rounded(surface, border, 14));
        line.setClickable(true);
        line.setFocusable(true);
        line.setContentDescription(label);
        TextView description = text(label, 14, foreground, true);
        line.addView(description, new LinearLayout.LayoutParams(0, -2, 1));
        line.setOnClickListener(view -> run.run());
        return line;
    }

    private final class IconDialogBuilder extends AlertDialog.Builder {
        IconDialogBuilder() { super(MainActivity.this); }

        @Override public AlertDialog show() {
            AlertDialog dialog = super.show();
            decorateDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), false);
            decorateDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), true);
            decorateDialogButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), false);
            return dialog;
        }
    }

    private void decorateDialogButton(Button button, boolean positive) {
        if (button == null) return;
        String original = button.getText().toString();
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(48));
        button.setContentDescription(original);
        button.setTooltipText(original);
    }

    private void gap(LinearLayout container, int height) {
        View space = new View(this);
        container.addView(space, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private void reset(String subtitle) {
        shell.removeAllViews();
        shell.setBackgroundColor(background);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(18), dp(14), dp(18), dp(10));
        TextView title = text("GoreeCloud Clips", 21, foreground, true);
        TextView sub = text(subtitle, 12, secondary, false);
        top.addView(title);
        top.addView(sub);
        top.setContentDescription("GoreeCloud Clips. Long press for app navigation.");
        top.setOnLongClickListener(view -> {
            showContextSheet();
            return true;
        });
        title.setOnLongClickListener(view -> {
            showContextSheet();
            return true;
        });
        shell.addView(top);
    }

    private String loadCleanWebScript() {
        try (InputStream input = getAssets().open("clean_browsing.js");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int count;
            while ((count = input.read(chunk)) != -1) output.write(chunk, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // Never stop browsing because cosmetic suppression failed.
            return "";
        }
    }

    private void buildWebView() {
        browser = new WebView(this);
        browser.setBackgroundColor(background);
        WebSettings ws = browser.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setAllowFileAccessFromFileURLs(false);
        ws.setAllowUniversalAccessFromFileURLs(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        ws.setSupportMultipleWindows(false);
        browser.setWebChromeClient(new WebChromeClient());
        installLongPressGesture();
        browser.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                if (CleanBrowsing.shouldBlockRequest(request.getUrl().toString(),
                        request.isForMainFrame()))
                    return new WebResourceResponse("text/plain", "UTF-8",
                            new ByteArrayInputStream(new byte[0]));
                return super.shouldInterceptRequest(view, request);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                String url = request.getUrl().toString();
                if (MediaUrls.isTikTok(url)) return false;
                if ("https".equalsIgnoreCase(request.getUrl().getScheme()))
                    openExternal(url);
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (settings.getBoolean("history_enabled", true) && MediaUrls.isVideoLink(url))
                    recent.record(view.getTitle(), url);
                if (MediaUrls.isTikTok(url) && !cleanWebScript.isEmpty())
                    view.evaluateJavascript(cleanWebScript, null);
            }
        });
        browser.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (!MediaUrls.isDownloadUrl(url)) {
                toast("This site offered an unsupported download link.");
                return;
            }
            showRightsWarning(() -> queueDownload(url, "goreecloud-clip.mp4"));
        });
    }

    private void renderBrowse() {
        currentTab = "browse";
        shell.removeAllViews();
        shell.setBackgroundColor(background);
        ViewGroup oldParent = (ViewGroup) browser.getParent();
        if (oldParent != null) oldParent.removeView(browser);
        // No app toolbar, dock or floating affordance: the WebView fills
        // the entire system-bar-safe area. Native Android bars remain visible.
        shell.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void cancelLongPress() {
        if (pendingLongPress != null) {
            main.removeCallbacks(pendingLongPress);
            pendingLongPress = null;
        }
    }

    private void installLongPressGesture() {
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        final int timeout = ViewConfiguration.getLongPressTimeout();
        browser.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    cancelLongPress();
                    longPressActivated = false;
                    pressStartX = event.getX();
                    pressStartY = event.getY();
                    pendingLongPress = () -> {
                        pendingLongPress = null;
                        longPressActivated = true;
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        showContextSheet();
                    };
                    main.postDelayed(pendingLongPress, timeout);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - pressStartX) > slop ||
                            Math.abs(event.getY() - pressStartY) > slop) cancelLongPress();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelLongPress();
                    break;
                default: break;
            }
            // Normal taps, swipes, scrolling and media playback stay in WebView.
            return false;
        });
        // Consume WebView's native text/URL selection long-click so the app
        // context sheet, rather than a second system popup, is presented.
        browser.setOnLongClickListener(view -> {
            if (!longPressActivated) showContextSheet();
            cancelLongPress();
            return true;
        });
    }

    private void showContextSheet() {
        if (isFinishing() || isDestroyed() || (contextSheet != null && contextSheet.isShowing()))
            return;
        cancelLongPress();
        Dialog sheet = new Dialog(this);
        contextSheet = sheet;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(16));
        content.setBackgroundColor(surface);
        TextView heading = text("GoreeCloud Clips", 18, foreground, true);
        content.addView(heading);
        TextView hint = text("Video and app tools", 13, secondary, false);
        content.addView(hint);
        gap(content, 8);

        addSheetSection(content, "YOUR SPACE");
        addSheetItem(content, "Saved library", "Bookmarked videos and favorites", this::renderSaved);
        addSheetItem(content, "Recent history", "Previously visited video pages", this::renderRecent);
        addSheetItem(content, "Downloads", "View downloaded and queued files", this::openDownloads);
        addSheetItem(content, "Settings", "Appearance, privacy and playback", this::renderSettings);

        gap(content, 7);
        addSheetSection(content, "THIS VIDEO");
        addSheetItem(content, "Save video", "Add this page to your library", this::saveCurrent);
        addSheetItem(content, "Download video", "Permissions confirmation required",
                () -> startOrPrompt(browser.getUrl()));
        addSheetItem(content, "Share link", "Send to another app", this::shareCurrent);
        addSheetItem(content, "Copy link", "Copy the current page URL", this::copyCurrent);

        gap(content, 7);
        addSheetSection(content, "BROWSING");
        addSheetItem(content, "Home", "Return to TikTok", () -> {
            browser.loadUrl(HOME);
            renderBrowse();
        });
        addSheetItem(content, "Back", "Previous page", () -> {
            if (browser.canGoBack()) browser.goBack();
            else toast("No previous page.");
        });
        addSheetItem(content, "Forward", "Next page", () -> {
            if (browser.canGoForward()) browser.goForward();
            else toast("No next page.");
        });
        addSheetItem(content, "Refresh", "Reload the current page", browser::reload);

        // A centered, compact card with a bounded scrollable list.
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(content);
        scroll.setBackground(rounded(surface, border, 22));
        scroll.setClipToOutline(true);
        scroll.setContentDescription("GoreeCloud video context menu");
        int menuHeight = Math.min(dp(480),
                Math.round(getResources().getDisplayMetrics().heightPixels * 0.60f));
        int menuWidth = Math.min(dp(365),
                getResources().getDisplayMetrics().widthPixels - dp(40));
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, menuHeight));
        sheet.setContentView(scroll);
        Window window = sheet.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.42f);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.GoreeContextMenuWindowAnimation);
        }
        sheet.setCanceledOnTouchOutside(true);
        sheet.setOnDismissListener(dialog -> {
            if (contextSheet == sheet) contextSheet = null;
        });
        sheet.show();
        if (sheet.getWindow() != null)
            sheet.getWindow().setLayout(menuWidth, menuHeight);
    }

    private void addSheetSection(LinearLayout content, String title) {
        TextView heading = text(title, 11, secondary, true);
        heading.setLetterSpacing(0.12f);
        heading.setPadding(dp(12), dp(6), dp(12), dp(5));
        content.addView(heading);
    }

    /** DPI-aware native vector actions; each glyph accompanies readable text. */
    private int contextActionIcon(String title) {
        switch (title) {
            case "Saved library": return R.drawable.gc_ic_library;
            case "Recent history": return R.drawable.gc_ic_history;
            case "Downloads": return R.drawable.gc_ic_downloads;
            case "Settings": return R.drawable.gc_ic_settings;
            case "Save video": return R.drawable.gc_ic_save;
            case "Download video": return R.drawable.gc_ic_download_video;
            case "Share link": return R.drawable.gc_ic_share;
            case "Copy link": return R.drawable.gc_ic_copy;
            case "Home": return R.drawable.gc_ic_home;
            case "Back": return R.drawable.gc_ic_back;
            case "Forward": return R.drawable.gc_ic_forward;
            case "Refresh": return R.drawable.gc_ic_refresh;
            default: return R.drawable.gc_ic_settings;
        }
    }

    private void addSheetItem(LinearLayout container, String title, String summary,
            Runnable callback) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(11), dp(7), dp(11), dp(7));
        item.setMinimumHeight(dp(56));
        item.setBackground(rounded(dark ? Color.parseColor("#252D3E") :
                Color.parseColor("#F5F7FB"), surface, 13));
        item.setContentDescription(title + ". " + summary);
        item.setFocusable(true);
        item.setClickable(true);

        FrameLayout iconShell = new FrameLayout(this);
        GradientDrawable iconPlate = new GradientDrawable();
        iconPlate.setColor(dark ? Color.parseColor("#334268") :
                Color.parseColor("#E8EEFF"));
        iconPlate.setCornerRadius(dp(11));
        iconShell.setBackground(iconPlate);
        ImageView icon = new ImageView(this);
        icon.setImageResource(contextActionIcon(title));
        icon.setImageTintList(ColorStateList.valueOf(accent));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        iconShell.addView(icon, new FrameLayout.LayoutParams(
                dp(22), dp(22), Gravity.CENTER));
        LinearLayout.LayoutParams iconPosition = new LinearLayout.LayoutParams(
                dp(38), dp(38));
        iconPosition.rightMargin = dp(12);
        item.addView(iconShell, iconPosition);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(title, 15, foreground, true);
        TextView description = text(summary, 11, secondary, false);
        description.setSingleLine(true);
        description.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);
        labels.addView(description);
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

        item.setOnClickListener(view -> {
            if (contextSheet != null) contextSheet.dismiss();
            callback.run();
        });
        container.addView(item, new LinearLayout.LayoutParams(-1, -2));
        gap(container, 4);
    }

    private void openDownloads() {
        try {
            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
        } catch (Exception e) {
            toast("Android Downloads is not available on this device.");
        }
    }

    private void saveCurrent() {
        String url = browser.getUrl();
        if (!MediaUrls.isTikTok(url)) { toast("Open a TikTok page first."); return; }
        bookmarks.save(browser.getTitle(), url);
        toast("Saved to your local library.");
    }

    private void copyCurrent() {
        String url = browser.getUrl();
        if (!MediaUrls.isTikTok(url)) { toast("Open a TikTok page first."); return; }
        ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clip == null) { toast("Clipboard unavailable."); return; }
        clip.setPrimaryClip(ClipData.newPlainText("TikTok video URL", url));
        toast("Link copied.");
    }

    private void shareCurrent() {
        String url = browser.getUrl();
        if (!MediaUrls.isTikTok(url)) { toast("Open a TikTok page first."); return; }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(send, "Share link"));
    }

    private EditText addLibrarySearch(LinearLayout content, String hint, String value,
            java.util.function.Consumer<String> changed) {
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(foreground);
        search.setHintTextColor(secondary);
        search.setHint(hint);
        search.setContentDescription(hint);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.setBackground(rounded(surface, border, 13));
        search.setPadding(dp(14), dp(13), dp(14), dp(13));
        content.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));
        gap(content, 12);
        search.setText(value);
        search.setSelection(search.length());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                changed.accept(s.toString());
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        return search;
    }

    private LinearLayout libraryContainer(String subtitle, String title) {
        reset(subtitle);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(20));
        return layout;
    }

    private void renderSaved() {
        currentTab = "saved";
        LinearLayout content = libraryContainer("Your searchable, on-device library", "Saved");
        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        addLibrarySearch(content, "Search titles or links", savedQuery, text -> {
            savedQuery = text;
            displaySavedCards(cards);
        });
        content.addView(cards);
        displaySavedCards(cards);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void displaySavedCards(LinearLayout cards) {
        cards.removeAllViews();
        List<BookmarkStore.Item> items = LibraryTools.filter(bookmarks.all(), savedQuery);
        if (items.isEmpty()) {
            TextView empty = text(savedQuery.isEmpty()
                    ? "Nothing saved yet. Press and hold a video, then choose Save video."
                    : "No saved videos match your search.", 15, secondary, false);
            empty.setPadding(dp(14), dp(22), dp(14), dp(22));
            empty.setBackground(rounded(surface, border, 16));
            cards.addView(empty);
            return;
        }
        for (BookmarkStore.Item item : items) {
            LinearLayout card = videoCard(item);
            LinearLayout controls = row();
            addLibraryAction(controls, "Open saved video", IconButtonView.OPEN,
                    () -> { browser.loadUrl(item.url); renderBrowse(); });
            addLibraryAction(controls, "Download saved video", IconButtonView.DOWNLOAD,
                    () -> startOrPrompt(item.url));
            addLibraryAction(controls, item.favorite ? "Unfavorite video" : "Favorite video",
                    item.favorite ? IconButtonView.STARRED : IconButtonView.STAR,
                    () -> { bookmarks.toggleFavorite(item.url); displaySavedCards(cards); });
            addLibraryAction(controls, "Remove saved video", IconButtonView.TRASH,
                    () -> { bookmarks.remove(item.url); displaySavedCards(cards); });
            card.addView(controls);
            cards.addView(card);
            gap(cards, 11);
        }
    }

    private void renderRecent() {
        currentTab = "recent";
        LinearLayout content = libraryContainer("Private local viewing history", "Recent");
        if (!settings.getBoolean("history_enabled", true)) {
            TextView message = text("Recent history is off. Enable it in Settings to remember visited video pages locally.", 15, secondary, false);
            content.addView(message);
        } else {
            LinearLayout cards = new LinearLayout(this);
            cards.setOrientation(LinearLayout.VERTICAL);
            addLibrarySearch(content, "Search recent videos", recentQuery, text -> {
                recentQuery = text;
                displayRecentCards(cards);
            });
            content.addView(cards);
            displayRecentCards(cards);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void displayRecentCards(LinearLayout cards) {
        cards.removeAllViews();
        List<BookmarkStore.Item> items = LibraryTools.filter(recent.all(), recentQuery);
        if (items.isEmpty()) {
            TextView message = text(recentQuery.isEmpty()
                    ? "No recent video pages yet."
                    : "No recent videos match your search.", 15, secondary, false);
            message.setPadding(dp(14), dp(20), dp(14), dp(20));
            cards.addView(message);
            return;
        }
        for (BookmarkStore.Item item : items) {
            LinearLayout card = videoCard(item);
            LinearLayout controls = row();
            addLibraryAction(controls, "Open recent video", IconButtonView.OPEN,
                    () -> { browser.loadUrl(item.url); renderBrowse(); });
            addLibraryAction(controls, "Download recent video", IconButtonView.DOWNLOAD,
                    () -> startOrPrompt(item.url));
            addLibraryAction(controls, "Save recent video", IconButtonView.BOOKMARK,
                    () -> { bookmarks.save(item.title, item.url); toast("Added to Saved."); });
            addLibraryAction(controls, "Remove from recent", IconButtonView.TRASH,
                    () -> { recent.remove(item.url); displayRecentCards(cards); });
            card.addView(controls);
            cards.addView(card);
            gap(cards, 11);
        }
    }

    private LinearLayout videoCard(BookmarkStore.Item item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(12));
        card.setBackground(rounded(surface, border, 16));
        TextView title = text((item.favorite ? "★ " : "") + item.title, 16, foreground, true);
        title.setMaxLines(2);
        card.addView(title);
        TextView url = text(item.url, 12, secondary, false);
        url.setMaxLines(2);
        url.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(url);
        gap(card, 9);
        return card;
    }

    private void addLibraryAction(LinearLayout controls, String label, int unusedGlyph,
            Runnable run) {
        String word = label.startsWith("Download") ? "Get" :
                label.startsWith("Unfavorite") ? "Unpin" :
                label.startsWith("Favorite") ? "Pin" :
                label.startsWith("Remove") ? "Remove" :
                label.startsWith("Save") ? "Save" : "Open";
        TextView button = text(word, 12, foreground, true);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(48));
        button.setBackground(rounded(dark ? Color.parseColor("#283247") :
                Color.parseColor("#EEF2F8"), border, 12));
        button.setContentDescription(label);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(view -> run.run());
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, dp(48), 1);
        size.setMargins(dp(3), 0, dp(3), 0);
        controls.addView(button, size);
    }

    private void renderSettings() {
        currentTab = "settings";
        reset("Built-in downloads and optional providers");
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(6), dp(18), dp(25));
        content.addView(text("Direct downloads", 19, foreground, true));
        gap(content, 8);
        content.addView(text("Built-in downloader is enabled automatically. Authorized videos download to Movies / GoreeCloud Clips; no third-party service configuration is required.", 13, secondary, false));
        gap(content, 20);
        content.addView(text("Optional processing provider", 19, foreground, true));
        gap(content, 10);
        content.addView(text("Cobalt-compatible processing instance (HTTPS)", 14, foreground, true));
        gap(content, 6);
        EditText provider = new EditText(this);
        provider.setSingleLine(true);
        provider.setTextSize(14);
        provider.setTextColor(foreground);
        provider.setHintTextColor(secondary);
        provider.setHint("https://your-approved-instance.example/");
        provider.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        provider.setText(settings.getString("provider", ""));
        content.addView(provider);
        gap(content, 7);
        TextView detail = text("Only needed if a direct download fails and you choose to retry through a trusted processing server. Links are never sent to that server automatically.", 13, secondary, false);
        content.addView(detail);
        gap(content, 13);
        content.addView(settingsAction("Save provider settings", IconButtonView.SAVE, () -> {
            String value = provider.getText().toString().trim();
            if (!value.isEmpty() && !MediaUrls.isProviderUrl(value)) {
                toast("Enter a valid public HTTPS URL with no extra path.");
                return;
            }
            settings.edit().putString("provider", value).apply();
            toast(value.isEmpty() ? "Provider disabled." : "Provider saved.");
        }));
        gap(content, 22);
        content.addView(text("External downloader", 19, foreground, true));
        gap(content, 8);
        content.addView(text("You can open Cobalt in your browser to paste a link manually. This is not an automatic API integration.", 13, secondary, false));
        gap(content, 9);
        content.addView(settingsAction("Open cobalt.tools", IconButtonView.EXTERNAL, () -> openExternal("https://cobalt.tools/")));
        gap(content, 22);
        content.addView(text("Appearance", 19, foreground, true));
        content.addView(settingsAction(dark ? "Switch to light mode" : "Switch to dark mode",
                dark ? IconButtonView.SUN : IconButtonView.MOON, () -> {
            dark = !dark;
            settings.edit().putBoolean("dark", dark).apply();
            applyColors();
            renderSettings();
        }));
        gap(content, 22);
        content.addView(text("Playback", 19, foreground, true));
        gap(content, 8);
        content.addView(settingsAction(settings.getBoolean("keep_awake", false)
                ? "Allow the screen to dim" : "Keep screen awake while watching",
                IconButtonView.AWAKE, () -> {
            boolean enabled = !settings.getBoolean("keep_awake", false);
            settings.edit().putBoolean("keep_awake", enabled).apply();
            updateKeepAwake();
            renderSettings();
        }));
        gap(content, 22);
        content.addView(text("Privacy", 19, foreground, true));
        gap(content, 8);
        content.addView(text("Saved videos and optional recent history stay on this device. No analytics, account syncing, or invisible video downloads. Website cookies remain in the embedded browser.", 13, secondary, false));
        gap(content, 10);
        content.addView(settingsAction(settings.getBoolean("history_enabled", true)
                ? "Turn off recent history" : "Turn on local recent history",
                IconButtonView.HISTORY, () -> {
            boolean enabled = !settings.getBoolean("history_enabled", true);
            settings.edit().putBoolean("history_enabled", enabled).apply();
            renderSettings();
        }));
        content.addView(settingsAction("Clear recent history", IconButtonView.TRASH,
                () -> new IconDialogBuilder()
                    .setTitle("Clear recent video history?")
                    .setMessage("This removes locally stored recent links, not saved bookmarks.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        recent.clear();
                        toast("Recent history cleared.");
                    }).show()));
        gap(content, 18);
        content.addView(settingsAction("Clear saved bookmarks", IconButtonView.TRASH, () -> new IconDialogBuilder()
            .setTitle("Delete bookmarks?")
            .setMessage("This cannot be undone. Downloaded media files will not be deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d, which) -> {
                bookmarks.clear();
                toast("Bookmarks deleted.");
            }).show()));
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void promptVideoLink() { promptVideoLink(browser.getUrl()); }

    private void startOrPrompt(String url) {
        if (MediaUrls.isVideoLink(url)) showRightsWarning(() -> downloadDirect(url));
        else promptVideoLink(url);
    }

    private void promptVideoLink(String suggested) {
        EditText input = new EditText(this);
        input.setSingleLine();
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(MediaUrls.isVideoLink(suggested) ? suggested : "");
        input.setHint("https://www.tiktok.com/@creator/video/...");
        new IconDialogBuilder()
            .setTitle("Download video")
            .setMessage("Enter a TikTok video link (a short share link is also accepted).")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", (dialog, which) -> {
                String videoUrl = input.getText().toString().trim();
                if (!MediaUrls.isVideoLink(videoUrl)) {
                    toast("Enter a valid TikTok video URL.");
                    return;
                }
                showRightsWarning(() -> downloadDirect(videoUrl));
            }).show();
    }

    private void showRightsWarning(Runnable confirmed) {
        new IconDialogBuilder()
            .setTitle("Copyright and creator permissions")
            .setMessage("Only download videos that you own or are authorized to download. Respect the creator's download settings and applicable laws. Do not remove watermarks or redistribute without permission.\n\nConfirm that you have the necessary rights.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("I have permission", (dialog, which) -> confirmed.run()).show();
    }

    private void downloadDirect(String videoUrl) {
        if (!downloading.compareAndSet(false, true)) {
            toast("A video is already downloading.");
            return;
        }
        toast("Downloading directly to your device...");
        executor.execute(() -> {
            try {
                DirectDownloader.SavedFile saved = DirectDownloader.download(
                    getApplicationContext(), videoUrl, (percent, eta) -> {});
                main.post(() -> {
                    downloading.set(false);
                    toast("Saved " + saved.name + " to your video library.");
                });
            } catch (Exception problem) {
                main.post(() -> {
                    downloading.set(false);
                    String message = problem.getMessage() == null
                        ? "The downloader could not access this video."
                        : problem.getMessage();
                    AlertDialog.Builder dialog = new IconDialogBuilder()
                        .setTitle("Direct download unavailable")
                        .setMessage(message)
                        .setNegativeButton("Close", null);
                    if (MediaUrls.isProviderUrl(settings.getString("provider", ""))) {
                        dialog.setPositiveButton("Try optional provider",
                            (d, w) -> resolveAndDownload(videoUrl));
                    }
                    dialog.show();
                });
            }
        });
    }

    private void resolveAndDownload(String videoUrl) {
        String endpoint = settings.getString("provider", "");
        if (!MediaUrls.isProviderUrl(endpoint)) {
            new IconDialogBuilder()
                .setTitle("Provider not configured")
                .setMessage("Set a trusted Cobalt-compatible HTTPS instance in Settings, or use the external downloader manually.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Settings", (d, w) -> renderSettings()).show();
            return;
        }
        toast("Requesting a downloadable file…");
        executor.execute(() -> {
            try {
                CobaltClient.Resolved item = CobaltClient.resolve(endpoint, videoUrl);
                main.post(() -> queueDownload(item.url, item.filename));
            } catch (Exception error) {
                main.post(() -> new IconDialogBuilder()
                    .setTitle("Download unavailable")
                    .setMessage(error.getMessage() == null ? "The provider could not process this link." : error.getMessage())
                    .setPositiveButton("OK", null).show());
            }
        });
    }

    private void queueDownload(String url, String filename) {
        if (!MediaUrls.isDownloadUrl(url)) { toast("Blocked unsafe download URL."); return; }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(MediaUrls.safeName(filename));
            request.setDescription("Authorized media download • GoreeCloud Clips");
            request.setMimeType("video/mp4");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, MediaUrls.safeName(filename));
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("Android download service unavailable.");
            manager.enqueue(request);
            toast("Download queued in Android Downloads.");
        } catch (Exception ex) {
            toast("Could not queue download: " + ex.getClass().getSimpleName());
        }
    }

    private void openExternal(String url) {
        if (!MediaUrls.isDownloadUrl(url)) { toast("Blocked unsafe external link."); return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception error) { toast("No browser available."); }
    }

    private String incomingUrl(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return null;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null) return null;
        Matcher m = Pattern.compile("https://[^\\s]+", Pattern.CASE_INSENSITIVE).matcher(text);
        if (!m.find()) return null;
        String url = m.group().replaceAll("[,.)]+$", "");
        return MediaUrls.isTikTok(url) ? url : null;
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
