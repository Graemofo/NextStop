package com.example.nextstop;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

import static com.example.nextstop.App.CHANNEL_ID;

//https://www.youtube.com/watch?v=lvcGh2ZgHeA&ab_channel=FilipVujovic

public class GPS_Service extends Service {

    LocationListener listener;
    LocationManager manager;
    CheckDistance checkDistance;
    String CANCEL_ALARM;

    public static double location_latitude;
    public static double location_longitude;
    public static double destination_latitude;
    public static double destination_longitude;
    public static double distance = 1.0;
    public static int start_id;
    public boolean service_on;
    public boolean stop_service = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        start_id = startId;
        checkDistance = new CheckDistance();
        final String str = intent.getStringExtra("cancel");

        String input = intent.getStringExtra("input");
        destination_latitude = intent.getDoubleExtra("lat", 0.1);
        destination_longitude = intent.getDoubleExtra("long", 1.0);
        service_on = intent.getBooleanExtra("STOP_SERVICE", false);


        Log.d("Pass Bool", "onStartCommand: " + service_on);

        if (service_on) {
            stopForegroundService();
            onDestroy();
        }


        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
        broadcastIntent.setAction(App.CANCEL_ALARM);
        broadcastIntent.putExtra("cancel", "This is a broadcast message");
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .setContentTitle("Next Stop")
                .setContentText(input)
                .setLargeIcon(largerIcon)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.location_foreground)
                .setContentIntent(pendingIntent)
                .addAction(R.mipmap.ic_launcher, "Cancel Alarm", actionIntent)
                .build();

        startForeground(6, notification);

        final Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        listener = new LocationListener() {
                            @Override
                            public void onLocationChanged(@NonNull Location location) {
                                location_latitude = location.getLatitude();
                                location_longitude = location.getLongitude();
                                distance = checkDistance.distance(destination_latitude, destination_longitude, location_latitude, location_longitude, 'K');
                            }
                        };
                        manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, listener);
                      //  Toast.makeText(getApplicationContext(), "GPS Distance: " + distance, Toast.LENGTH_LONG).show();
                        Log.d("GPS_Service", "run: " + distance);
                        if (distance <= 0.5 || service_on) {
                            stopForegroundService();
                            t.cancel();

                        }
                    }
                });
            }
        }, 0, 5000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "onDestroy: ");
        if (manager != null) {
            manager.removeUpdates(this.listener);
        }
        this.stopForeground(true);
        this.stopSelf();
        // manager.removeUpdates(this.listener);
    }


    private void stopForegroundService() {
        Log.d("GPS-Service", "stopForegroundService: "+service_on);
        this.stopForeground(true);
        this.stopSelf();
        manager.removeUpdates(this.listener);

    }


}//end of class
