package com.cleanyourtv.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private TextView tvDate;
    private TextView tvFocusedTitle;
    private View ambientGlow;
    private RecyclerView rvDock;
    private RecyclerView rvGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvGreeting = findViewById(R.id.tv_greeting);
        tvDate = findViewById(R.id.tv_date);
        tvFocusedTitle = findViewById(R.id.tv_focused_app_title);
        ambientGlow = findViewById(R.id.ambient_glow);
        rvDock = findViewById(R.id.rv_dock);
        rvGrid = findViewById(R.id.rv_grid);

        setupTopBar();
        loadAppsAndPopulate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreetingAndDate();
        updateNetworkPill();
    }

    @Override
    public void onBackPressed() {
        // 作為電視桌面 Launcher，按返回鍵不應退出桌面
    }

    private void setupTopBar() {
        TextView btnInputs = findViewById(R.id.btn_inputs);
        TextView btnWifi = findViewById(R.id.btn_wifi);
        TextView btnSettings = findViewById(R.id.btn_settings);

        btnInputs.setOnClickListener(v -> showTvInputsDialog());
        btnWifi.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        btnSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
            }
        });
    }

    private void updateGreetingAndDate() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            tvGreeting.setText("早安 Good Morning");
        } else if (hour < 18) {
            tvGreeting.setText("午安 Good Afternoon");
        } else {
            tvGreeting.setText("晚安 Good Evening");
        }
        String dateStr = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        tvDate.setText(dateStr);
    }

    private void updateNetworkPill() {
        TextView btnWifi = findViewById(R.id.btn_wifi);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && cm.getActiveNetwork() != null) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    btnWifi.setText("有線網路 Ethernet");
                    return;
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    btnWifi.setText("Wi-Fi 已連線");
                    return;
                }
            }
        }
        btnWifi.setText("網路設定 Network");
    }

    private void showTvInputsDialog() {
        TvInputManager manager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        List<String> names = new ArrayList<>();
        List<String> inputIds = new ArrayList<>();

        if (manager != null) {
            List<TvInputInfo> inputs = manager.getTvInputList();
            for (TvInputInfo info : inputs) {
                CharSequence label = info.loadLabel(this);
                names.add(label != null ? label.toString() : info.getId());
                inputIds.add(info.getId());
            }
        }

        if (names.isEmpty()) {
            // 若無法直接取得底層列表，嘗試呼叫 TCL / Google TV 內建訊號源選單
            try {
                Intent intent = new Intent("com.android.tv.action.OPEN_INPUTS_PANEL");
                startActivity(intent);
                return;
            } catch (Exception ignored) {
            }
            names.add("HDMI 1 / 電視訊號源 (點擊開啟系統設定)");
            inputIds.add("");
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("選擇訊號源 (Inputs)")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    String id = inputIds.get(which);
                    if (id.isEmpty()) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } else {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    android.media.tv.TvContract.buildChannelUriForPassthroughInput(id));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        }
                    }
                })
                .show();
    }

    private void loadAppsAndPopulate() {
        PackageManager pm = getPackageManager();
        Set<String> seenPackages = new HashSet<>();
        List<AppEntry> allApps = new ArrayList<>();

        // 1. 優先抓取電視專用 Leanback Apps
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> leanbackInfos = pm.queryIntentActivities(leanbackIntent, 0);

        // 2. 同時抓取一般手機/平板相容 Apps
        Intent standardIntent = new Intent(Intent.ACTION_MAIN, null);
        standardIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> standardInfos = pm.queryIntentActivities(standardIntent, 0);

        List<ResolveInfo> combined = new ArrayList<>(leanbackInfos);
        combined.addAll(standardInfos);

        String myPkg = getPackageName();
        for (ResolveInfo info : combined) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(myPkg) || seenPackages.contains(pkg)) continue;
            seenPackages.add(pkg);

            String label = info.loadLabel(pm).toString();
            Drawable icon = info.activityInfo.loadBanner(pm);
            if (icon == null) {
                icon = info.loadIcon(pm);
            }

            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) {
                launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            }
            if (launch == null) {
                launch = new Intent(Intent.ACTION_MAIN);
                launch.setClassName(pkg, info.activityInfo.name);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            int color = sampleColor(icon);
            allApps.add(new AppEntry(label, pkg, icon, launch, color));
        }

        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));

        int columns = getResources().getInteger(R.integer.columns);
        String[] preferredDock = getResources().getStringArray(R.array.dock_apps);
        List<String> preferredList = Arrays.asList(preferredDock);

        List<AppEntry> dockApps = new ArrayList<>();
        List<AppEntry> gridApps = new ArrayList<>();

        for (String prefPkg : preferredList) {
            for (AppEntry app : allApps) {
                if (app.packageName.equals(prefPkg) && dockApps.size() < columns) {
                    dockApps.add(app);
                    break;
                }
            }
        }

        for (AppEntry app : allApps) {
            if (!dockApps.contains(app)) {
                if (dockApps.size() < columns) {
                    dockApps.add(app);
                } else {
                    gridApps.add(app);
                }
            }
        }

        AppTileAdapter.OnAppFocusListener focusListener = app -> {
            tvFocusedTitle.setText(app.label);
            ambientGlow.setBackgroundColor(app.dominantColor);
        };

        rvDock.setLayoutManager(new GridLayoutManager(this, Math.max(1, dockApps.size())));
        rvDock.setAdapter(new AppTileAdapter(this, dockApps, focusListener));

        rvGrid.setLayoutManager(new GridLayoutManager(this, columns));
        rvGrid.setAdapter(new AppTileAdapter(this, gridApps, focusListener));

        // 預設將遙控器焦點停在 Dock 第一個 App
        rvDock.post(() -> {
            if (rvDock.getChildCount() > 0) {
                rvDock.getChildAt(0).requestFocus();
            }
        });
    }

    private int sampleColor(Drawable drawable) {
        if (drawable == null) return Color.parseColor("#3306C755");
        try {
            Bitmap bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            int pixel = bmp.getPixel(8, 8);
            return Color.argb(90, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
        } catch (Exception e) {
            return Color.parseColor("#33FFFFFF");
        }
    }
}
