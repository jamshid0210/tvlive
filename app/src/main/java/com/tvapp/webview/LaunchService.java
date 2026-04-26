package com.tvapp.webview;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class LaunchService extends Service {

    private static final String CHANNEL_ID = "boot_channel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Android 8+ da ForegroundService notification kerak
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("TV WebApp")
            .setContentText("Ishga tushirilmoqda...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build();

        startForeground(1, notification);

        // 5 soniya kutib Activity ochamiz
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent launchIntent = new Intent(this, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
            stopSelf(); // Service ni yopamiz
        }, 5000);

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Boot Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
