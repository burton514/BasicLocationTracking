package com.example.zenlyrootjava;

import android.app.*;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class LocationService extends Service implements LocationListener {

    LocationManager locationManager;
    Location lastKnownLocation;

    private Handler handler = new Handler();
    private Runnable locationToastRunnable;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("ZenlyClone", "LocationService onCreate");

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Đăng ký nhận tọa độ GPS mỗi 5 giây
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0,
                    this
            );
            Log.d("ZenlyClone", "Đã đăng ký lấy vị trí mỗi 5 giây");
        } catch (SecurityException e) {
            Log.e("ZenlyClone", "Permission denied for location");
        }

        // Lấy tọa độ gần nhất nếu có
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.e("ZenlyClone", "Không thể lấy lastKnownLocation");
        }

        startForegroundServiceWithNotification();
        startRepeatingToast(); // Bắt đầu hiển thị toast mỗi 5 giây
        RootUtils.hideForegroundNotification(); // Dùng quyền root để ẩn notification foreground
    }

    private void startForegroundServiceWithNotification() {
        String channelId = "zenly_channel";
        int notificationId = 1;

        // Tạo notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Zenly Background",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Định vị đang chạy")
                .setContentText("Ứng dụng định vị ngầm")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        startForeground(notificationId, notification);
    }

    private void startRepeatingToast() {
        locationToastRunnable = new Runnable() {
            @Override
            public void run() {
                String msg;
                if (lastKnownLocation != null) {
                    double lat = lastKnownLocation.getLatitude();
                    double lon = lastKnownLocation.getLongitude();
                    msg = "Lat: " + lat + ", Lon: " + lon;
                } else {
                    msg = "Đang lấy vị trí...";
                }

                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                handler.postDelayed(this, 5000); // Lặp lại sau 5 giây
            }
        };
        handler.post(locationToastRunnable); // Chạy lần đầu
    }

    @Override
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;
        // Cập nhật vị trí mới, không cần Toast ở đây nữa vì đã có Toast lặp riêng
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ZenlyClone", "LocationService onStartCommand called");
        return START_STICKY; // Gợi ý hệ thống khởi động lại nếu bị kill
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(locationToastRunnable);
        Log.e("ZenlyClone", "LocationService destroyed (possibly killed)");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.w("ZenlyClone", "⚠️ Location provider disabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("ZenlyClone", "✅ Location provider enabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("ZenlyClone", "Provider status changed: " + provider + ", status: " + status);
    }
}

