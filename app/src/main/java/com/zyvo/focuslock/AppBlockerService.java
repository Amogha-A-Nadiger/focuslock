package com.zyvo.focuslock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class AppBlockerService extends AccessibilityService {
    private String lastBlockedPkg = null;
    private long lastBlockTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!Prefs.isActive(this)) {
            lastBlockedPkg = null;
            return;
        }
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();
        if (pkg.equals(getPackageName())) return; // don't block ourselves
        if (!Prefs.isBlocked(this, pkg)) return;

        // Debounce: don't re-launch if we just blocked this app
        long now = System.currentTimeMillis();
        if (pkg.equals(lastBlockedPkg) && (now - lastBlockTime) < 2000) return;
        lastBlockedPkg = pkg;
        lastBlockTime = now;

        // Redirect to blocked screen
        Intent i = new Intent(this, BlockedAppsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(i);
    }

    @Override
    public void onInterrupt() { }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }
}