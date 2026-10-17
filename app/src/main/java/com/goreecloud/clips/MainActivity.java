package com.goreecloud.clips;

import android.app.Activity;
import java.util.concurrent.atomic.AtomicBoolean;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
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
    private WebView browser;
    private LinearLayout shell;
    private String currentTab = "browse";
    private boolean dark;
    private int background, surface, foreground, secondary, border, accent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        bookmarks = new BookmarkStore(this);
        dark = settings.getBoolean("dark", false);
        applyColors();
        shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        setContentView(shell);
        buildWebView();
        String incoming = incomingUrl(getIntent());
        if (incoming != null) browser.loadUrl(incoming);
        else browser.loadUrl(HOME);
        renderBrowse();
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

    private void applyColors() {
        background = Color.parseColor(dark ? "#111522" : "#F7F8FB");
        surface = Color.parseColor(dark ? "#1B2130" : "#FFFFFF");
        foreground = Color.parseColor(dark ? "#F6F7FF" : "#202738");
        secondary = Color.parseColor(dark ? "#BAC3D7" : "#667185");
        border = Color.parseColor(dark ? "#374052" : "#E1E5EE");
        accent = Color.parseColor(dark ? "#A5B8FF" : "#3156CB");
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
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

    private TextView action(String label, Runnable run) {
        TextView b = text(label, 14, foreground, true);
        b.setBackground(rounded(surface, border, 13));
        b.setPadding(dp(13), dp(11), dp(13), dp(11));
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(v -> run.run());
        return b;
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
        shell.addView(top);
    }

    private void addBottom() {
        LinearLayout bottom = row();
        bottom.setPadding(dp(12), dp(9), dp(12), dp(13));
        bottom.setBackgroundColor(surface);
        addTab(bottom, "Explore", "browse", this::renderBrowse);
        addTab(bottom, "Saved", "saved", this::renderSaved);
        addTab(bottom, "Settings", "settings", this::renderSettings);
        shell.addView(bottom);
    }

    private void addTab(LinearLayout bar, String label, String tab, Runnable run) {
        TextView item = text(label, 14, currentTab.equals(tab) ? accent : secondary, currentTab.equals(tab));
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(7), dp(10), dp(7), dp(10));
        item.setOnClickListener(v -> run.run());
        bar.addView(item, new LinearLayout.LayoutParams(0, dp(44), 1));
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
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                String url = request.getUrl().toString();
                if (MediaUrls.isTikTok(url)) return false;
                if ("https".equalsIgnoreCase(request.getUrl().getScheme()))
                    openExternal(url);
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if ("browse".equals(currentTab)) {
                    // Refresh the current page indicator without recreating the WebView.
                    TextView indicator = shell.findViewWithTag("current_link");
                    if (indicator != null) indicator.setText(url == null ? "" : url);
                }
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
        reset("TikTok browsing • native library and download tools");
        LinearLayout controls = row();
        controls.setPadding(dp(12), dp(6), dp(12), dp(10));
        addControl(controls, "⌂", () -> browser.loadUrl(HOME));
        addControl(controls, "＋ Save", this::saveCurrent);
        addControl(controls, "↓ Download", () -> startOrPrompt(browser.getUrl()));
        addControl(controls, "↗ Share", this::shareCurrent);
        shell.addView(controls);
        TextView indicator = text(browser.getUrl() == null ? HOME : browser.getUrl(), 11, secondary, false);
        indicator.setTag("current_link");
        indicator.setMaxLines(1);
        indicator.setEllipsize(android.text.TextUtils.TruncateAt.END);
        indicator.setPadding(dp(17), 0, dp(17), dp(7));
        shell.addView(indicator);
        ViewGroup parent = (ViewGroup) browser.getParent();
        if (parent != null) parent.removeView(browser);
        shell.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottom();
    }

    private void addControl(LinearLayout row, String label, Runnable run) {
        TextView b = action(label, run);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(b, lp);
    }

    private void saveCurrent() {
        String url = browser.getUrl();
        if (!MediaUrls.isTikTok(url)) { toast("Open a TikTok page first."); return; }
        bookmarks.save(browser.getTitle(), url);
        toast("Saved to your local library.");
    }

    private void shareCurrent() {
        String url = browser.getUrl();
        if (!MediaUrls.isTikTok(url)) { toast("Open a TikTok page first."); return; }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(send, "Share link"));
    }

    private void renderSaved() {
        currentTab = "saved";
        reset("Your personal, on-device bookmarks");
        ScrollView scroller = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(20));
        List<BookmarkStore.Item> items = bookmarks.all();
        if (items.isEmpty()) {
            TextView help = text("No saved links yet.\n\nBrowse a video and tap + Save. Bookmarks stay on this device until you remove them.", 15, secondary, false);
            help.setPadding(dp(18), dp(28), dp(18), dp(28));
            help.setBackground(rounded(surface, border, 17));
            content.addView(help);
        }
        for (BookmarkStore.Item item : items) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            card.setBackground(rounded(surface, border, 16));
            TextView name = text(item.title, 16, foreground, true);
            name.setMaxLines(2);
            card.addView(name);
            TextView url = text(item.url, 12, secondary, false);
            url.setMaxLines(2);
            card.addView(url);
            gap(card, 9);
            LinearLayout controls = row();
            TextView open = action("Open", () -> { browser.loadUrl(item.url); renderBrowse(); });
            TextView download = action("Download", () -> startOrPrompt(item.url));
            TextView remove = action("Remove", () -> { bookmarks.remove(item.url); renderSaved(); });
            for (TextView button : new TextView[] { open, download, remove }) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(43), 1);
                lp.setMargins(dp(2), 0, dp(2), 0);
                controls.addView(button, lp);
            }
            card.addView(controls);
            content.addView(card);
            gap(content, 11);
        }
        scroller.addView(content);
        shell.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottom();
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
        content.addView(action("Save provider settings", () -> {
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
        content.addView(action("Open cobalt.tools", () -> openExternal("https://cobalt.tools/")));
        gap(content, 22);
        content.addView(text("Appearance", 19, foreground, true));
        Switch toggle = new Switch(this);
        toggle.setText("Dark mode");
        toggle.setTextColor(foreground);
        toggle.setChecked(dark);
        toggle.setOnCheckedChangeListener((v, checked) -> {
            dark = checked;
            settings.edit().putBoolean("dark", checked).apply();
            applyColors();
            renderSettings();
        });
        content.addView(toggle);
        gap(content, 22);
        content.addView(text("Privacy", 19, foreground, true));
        gap(content, 8);
        content.addView(text("Bookmarks are stored on-device. No analytics, account syncing, or invisible video downloads. Website data and cookies are controlled by the embedded browser.", 13, secondary, false));
        gap(content, 18);
        content.addView(action("Clear saved bookmarks", () -> new AlertDialog.Builder(this)
            .setTitle("Delete bookmarks?")
            .setMessage("This cannot be undone. Downloaded media files will not be deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d, which) -> {
                for (BookmarkStore.Item item : bookmarks.all()) bookmarks.remove(item.url);
                toast("Bookmarks deleted.");
            }).show()));
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addBottom();
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
        new AlertDialog.Builder(this)
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
        new AlertDialog.Builder(this)
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
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this)
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
            new AlertDialog.Builder(this)
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
                main.post(() -> new AlertDialog.Builder(this)
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
