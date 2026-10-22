package com.goreecloud.clips;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Runs only against an isolated gestureqa package on an authorized Android device.
 * Injects touches through the supported instrumentation API, never /dev/input.
 * Does not log in, clear cookies, or read user accounts.
 */
public final class MainActivityGestureTest {
    private Instrumentation instrumentation;
    private MainActivity activity;

    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity = (MainActivity) instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
    }

    @After public void close() {
        if (activity != null) {
            instrumentation.runOnMainSync(() -> {
                if (!activity.isFinishing()) activity.finish();
            });
            instrumentation.waitForIdleSync();
        }
    }

    private Object field(String name) throws Exception {
        Field f = MainActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(activity);
    }

    private static MotionEvent.PointerProperties properties(int id) {
        MotionEvent.PointerProperties p = new MotionEvent.PointerProperties();
        p.id = id;
        p.toolType = MotionEvent.TOOL_TYPE_FINGER;
        return p;
    }

    private static MotionEvent.PointerCoords coords(float x, float y) {
        MotionEvent.PointerCoords p = new MotionEvent.PointerCoords();
        p.x = x;
        p.y = y;
        p.pressure = 1f;
        p.size = 1f;
        return p;
    }

    private void send(long down, int action, boolean two, float secondX) {
        int count = two ? 2 : 1;
        MotionEvent.PointerProperties[] props = {properties(0), properties(1)};
        MotionEvent.PointerCoords[] coords = {coords(320f, 750f), coords(secondX, 750f)};
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(),
                action, count, props, coords, 0, 0, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
        try {
            instrumentation.sendPointerSync(event);
        } finally {
            event.recycle();
        }
    }

    private void release(long down) {
        send(down, MotionEvent.ACTION_POINTER_UP |
                (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), true, 700f);
        send(down, MotionEvent.ACTION_UP, false, 700f);
        instrumentation.waitForIdleSync();
    }

    @Test public void stationaryTwoFingerHoldOpensNativeTools() throws Exception {
        long down = SystemClock.uptimeMillis();
        send(down, MotionEvent.ACTION_DOWN, false, 700f);
        send(down, MotionEvent.ACTION_POINTER_DOWN |
                (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), true, 700f);
        SystemClock.sleep(875);
        instrumentation.waitForIdleSync();
        Dialog sheet = (Dialog) field("contextSheet");
        assertNotNull("Stationary two-finger hold must open GoreeCloud tools", sheet);
        assertTrue(sheet.isShowing());
        release(down);
    }

    @Test public void movingTwoFingersCancelsTools() throws Exception {
        long down = SystemClock.uptimeMillis();
        send(down, MotionEvent.ACTION_DOWN, false, 700f);
        send(down, MotionEvent.ACTION_POINTER_DOWN |
                (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), true, 700f);
        send(down, MotionEvent.ACTION_MOVE, true, 815f);
        SystemClock.sleep(875);
        instrumentation.waitForIdleSync();
        Dialog sheet = (Dialog) field("contextSheet");
        assertTrue("Pinching should not open tools", sheet == null || !sheet.isShowing());
        release(down);
    }

    @Test public void profileErrorShowsRecoveryButOtherUrlsDoNot() throws Exception {
        final WebView browser = (WebView) field("browser");
        final String profile = "https://www.tiktok.com/profile";
        instrumentation.runOnMainSync(() -> {
            browser.stopLoading();
            try {
                Field active = MainActivity.class.getDeclaredField("activeMainFrameUrl");
                active.setAccessible(true);
                active.set(activity, profile);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
            WebViewClient client = browser.getWebViewClient();
            client.onReceivedError(browser,
                    new Request("https://www.tiktok.com/other", true), null);
            client.onReceivedError(browser,
                    new Request(profile, false), null);
        });
        instrumentation.waitForIdleSync();
        assertNull("Unrelated / subresource failures must not show recovery",
                field("profileRecoveryDialog"));

        instrumentation.runOnMainSync(() -> browser.getWebViewClient().onReceivedError(
                browser, new Request(profile, true), null));
        instrumentation.waitForIdleSync();
        AlertDialog recovery = (AlertDialog) field("profileRecoveryDialog");
        assertNotNull("Main-frame profile failure should show recovery", recovery);
        assertTrue(recovery.isShowing());
        assertNotNull(recovery.getButton(AlertDialog.BUTTON_POSITIVE));
        assertNotNull(recovery.getButton(AlertDialog.BUTTON_NEUTRAL));
        assertNotNull(recovery.getButton(AlertDialog.BUTTON_NEGATIVE));
    }

    private static final class Request implements WebResourceRequest {
        private final Uri uri;
        private final boolean mainFrame;
        Request(String address, boolean mainFrame) {
            this.uri = Uri.parse(address);
            this.mainFrame = mainFrame;
        }
        @Override public Uri getUrl() { return uri; }
        @Override public boolean isForMainFrame() { return mainFrame; }
        @Override public boolean isRedirect() { return false; }
        @Override public boolean hasGesture() { return false; }
        @Override public String getMethod() { return "GET"; }
        @Override public Map<String, String> getRequestHeaders() {
            return Collections.emptyMap();
        }
    }
}
