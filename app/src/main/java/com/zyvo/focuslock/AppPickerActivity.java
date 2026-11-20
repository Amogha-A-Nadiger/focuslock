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
        setContentView(R.layout.activity_picker);

        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<String> labels = new ArrayList<>();
        List<String> packages = new ArrayList<>();
        for (ApplicationInfo ai : apps) {
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue; // system internals not launchable
            if (ai.packageName.equals(getPackageName())) continue; // skip ourselves
            labels.add(pm.getApplicationLabel(ai).toString());
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
