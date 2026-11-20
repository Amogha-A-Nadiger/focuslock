package com.zyvo.focuslock;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppPickerActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Prefs.isActive(this)) { // direct entry (recents) during a running timer
            Toast.makeText(this, "Timer active - block list is locked", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_picker);

        PackageManager pm = getPackageManager();
        // Match all: user apps, bloatware, disabled and leftover-uninstalled entries.
        // QUERY_ALL_PACKAGES (manifest) lifts the Android 11+ package-visibility filter.
        List<ApplicationInfo> apps = pm.getInstalledApplications(
            PackageManager.GET_META_DATA
              | PackageManager.GET_UNINSTALLED_PACKAGES  // MATCH_UNINSTALLED_PACKAGES pre-API 30
              | PackageManager.MATCH_DISABLED_COMPONENTS);
        List<String> labels = new ArrayList<>();
        List<String> packages = new ArrayList<>();
        for (ApplicationInfo ai : apps) {
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue; // system apps: nothing to block
            if (ai.packageName.equals(getPackageName())) continue; // skip ourselves
            String label;
            try { label = pm.getApplicationLabel(ai).toString(); } catch (Exception e) { label = ai.packageName; }
            if (label == null || label.isEmpty()) label = ai.packageName; // hidden apps may have no label
            labels.add(label);
            packages.add(ai.packageName);
        }
        // sort both lists together by app name
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) order.add(i);
        Collections.sort(order, (x, y) -> labels.get(x).compareToIgnoreCase(labels.get(y)));
        List<String> sLabels = new ArrayList<>(), sPkgs = new ArrayList<>();
        for (int i : order) { sLabels.add(labels.get(i)); sPkgs.add(packages.get(i)); }

        ListView list = findViewById(R.id.appList);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_multiple_choice, sLabels);
        list.setAdapter(adapter);

        java.util.Set<String> blocked = Prefs.getBlocked(this);
        for (int i = 0; i < sPkgs.size(); i++) list.setItemChecked(i, blocked.contains(sPkgs.get(i)));

        list.setOnItemClickListener((parent, view, pos, id) -> Prefs.toggleBlocked(this, sPkgs.get(pos)));
    }
}
