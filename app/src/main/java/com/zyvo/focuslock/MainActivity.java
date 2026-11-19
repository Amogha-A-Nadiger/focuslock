package com.zyvo.focuslock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView statusText;
    private TextView countdownText;
    private EditText durationInput;
    private Button startBtn;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refresh;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        countdownText = findViewById(R.id.countdownText);
        durationInput = findViewById(R.id.durationInput);
        startBtn = findViewById(R.id.startBtn);

        startBtn.setOnClickListener(v -> startTimer());

        requestBatteryOptimization();

        refresh = new Runnable() {
            @Override public void run() {
                updateUI();
                handler.postDelayed(this, 500);
            }
        };
        handler.post(refresh);
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                try { startActivity(i); } catch (Exception e) { /* ignore */ }
            }
        }
    }

    private void startTimer() {
        if (Prefs.isActive(this)) {
            Toast.makeText(this, "Timer already running", Toast.LENGTH_SHORT).show();
            return;
        }
        String txt = durationInput.getText().toString().trim();
        int mins;
        try { mins = Integer.parseInt(txt); } catch (NumberFormatException e) { mins = 0; }
        if (mins < 1 || mins > 720) {
            Toast.makeText(this, "Enter 1-720 minutes", Toast.LENGTH_SHORT).show();
            return;
        }
        TimerService.start(this, mins);
        Toast.makeText(this, "Focus started for " + mins + " min", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void updateUI() {
        if (Prefs.isActive(this)) {
            long rem = Prefs.getEndTime(this) - System.currentTimeMillis();
            if (rem <= 0) {
                Prefs.setActive(this, false);
                statusText.setText("No active timer");
                countdownText.setText("00:00");
                startBtn.setEnabled(true);
                durationInput.setEnabled(true);
                return;
            }
            long sec = rem / 1000;
            long m = sec / 60;
            long s = sec % 60;
            statusText.setText("Focus session active - cannot be stopped");
            countdownText.setText(String.format("%02d:%02d", m, s));
            startBtn.setEnabled(false);
            durationInput.setEnabled(false);
        } else {
            statusText.setText("Set duration and start focus");
            countdownText.setText("--:--");
            startBtn.setEnabled(true);
            durationInput.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimerService.resumeIfNeeded(this);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(refresh);
        super.onDestroy();
    }
}