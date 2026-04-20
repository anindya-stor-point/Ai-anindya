package org.ai.tools.aivisionguide;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "AIVisionGuideChannel";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AI Vision Guide")
                .setContentText("Guiding you in the background...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();

        int type = 0;
        if (Build.VERSION.SDK_INT >= 29) {
            type = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            // Android 14 also requires microphone if used contextually, but mediaprojection usually covers it.
            // Let's use FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION | FOREGROUND_SERVICE_TYPE_MICROPHONE (which is 128)
            type = 32 | 128; // 32 is MEDIA_PROJECTION, 128 is MICROPHONE
        }
        
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1001, notification, type);
        } else {
            startForeground(1001, notification);
        }

        // Start the native loop
        String apiKey = intent.getStringExtra("API_KEY");
        int w = intent.getIntExtra("WIDTH", 720);
        int h = intent.getIntExtra("HEIGHT", 1280);
        int dpi = intent.getIntExtra("DPI", 320);

        // We already have the intent safely stored in NativeHelper from Kivy activity
        NativeHelper.startContinuousAnalysis(this, apiKey, NativeHelper.staticDataIntent, w, h, dpi);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "AI Vision Background Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NativeHelper.stopContinuousAnalysis(this);
    }
}
