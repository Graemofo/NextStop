package com.example.nextstop;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class App extends Application {

    public static final String CHANNEL_ID = "gps_channel";
    public static final String ALARM_ID = "alarm_id";

    @Override
    public void onCreate() {
        super.onCreate();

        createChannel();
    }

    private void createChannel() {

        runtime_permissions();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Service_Channel";
            String desrcription = "Channel for all modes of transport ";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            CharSequence alarm = "Alarm_Channel";
            String alarmDescription = "Channel for Next Stop Alarm and Notifications";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(desrcription);

            NotificationChannel alarmChannel = new NotificationChannel(ALARM_ID, alarm, importance);
            alarmChannel.setDescription(alarmDescription);



            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            notificationManager.createNotificationChannel(alarmChannel);
        }
    }// end of onCreate

    private void runtime_permissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }
    }
}
