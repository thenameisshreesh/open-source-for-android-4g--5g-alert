package com.example.a5g_4galerts;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class NetworkMonitorService extends Service {

    private static final String CHANNEL_ID = "network_monitor_channel";
    private TelephonyManager telephonyManager;
    private CameraManager cameraManager;
    private Vibrator vibrator;

    private String lastNetworkStatus = "";
    private MyTelephonyCallback telephonyCallback;

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            if (ActivityCompat.checkSelfPermission(NetworkMonitorService.this,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                int type = telephonyManager.getDataNetworkType();
                handleNetworkType(type, -1);
            }
        }
    };

    private class MyTelephonyCallback extends TelephonyCallback implements TelephonyCallback.DisplayInfoListener {
        @Override
        public void onDisplayInfoChanged(TelephonyDisplayInfo displayInfo) {
            int overrideType = displayInfo.getOverrideNetworkType();
            int baseType = displayInfo.getNetworkType();
            handleNetworkType(baseType, overrideType);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        createNotificationChannel();

        // Start foreground immediately
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("5G/4G Alert Service")
                .setContentText("Monitoring started")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        startMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // service restarts if killed
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = new MyTelephonyCallback();
            telephonyManager.registerTelephonyCallback(getMainExecutor(), telephonyCallback);
        } else {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }
    }

    private void stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (telephonyCallback != null) {
                telephonyManager.unregisterTelephonyCallback(telephonyCallback);
            }
        } else {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    private void handleNetworkType(int baseType, int overrideType) {
        String status;
        if (overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED) {
            status = "5G";
        } else if (baseType == TelephonyManager.NETWORK_TYPE_NR) {
            status = "5G";
        } else if (baseType == TelephonyManager.NETWORK_TYPE_LTE) {
            status = "4G";
        } else {
            status = "Other";
        }

        if (!status.equals(lastNetworkStatus)) {
            lastNetworkStatus = status;

            if (status.equals("5G")) {
                showNetworkAlert("✅ Back to 5G", "Unlimited data active!", 1);
            } else if (status.equals("4G")) {
                showNetworkAlert("⚠ Switched to 4G", "Daily quota will be used!", 3);
            } else {
                Log.d("NetworkMonitor", "Other network type detected");
            }
        }
    }

    private void showNetworkAlert(String title, String message, int flashCount) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(2, notification);

        blinkFlash(flashCount);
        vibrate(flashCount);
    }

    private void blinkFlash(int times) {
        new Thread(() -> {
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                for (int i = 0; i < times; i++) {
                    cameraManager.setTorchMode(cameraId, true);
                    Thread.sleep(200);
                    cameraManager.setTorchMode(cameraId, false);
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void vibrate(int times) {
        long[] pattern = new long[times * 2];
        for (int i = 0; i < times * 2; i++) {
            pattern[i] = 200;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Network Monitor",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
