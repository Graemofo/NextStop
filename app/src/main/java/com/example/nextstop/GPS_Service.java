package com.example.nextstop;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

import static com.example.nextstop.App.CHANNEL_ID;

//https://www.youtube.com/watch?v=lvcGh2ZgHeA&ab_channel=FilipVujovic

public class GPS_Service extends Service {

    LocationListener listener;
    LocationManager manager;
    CheckDistance checkDistance;

    public static double location_latitude;
    public static double location_longitude;
    public  static double destination_latitude;
    public static double destination_longitude;
    public static double distance;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();

        checkDistance = new CheckDistance();

        destination_latitude = MainActivity.getDest_lat();
        destination_longitude = MainActivity.getDest_long();

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                location_latitude = location.getLatitude();
                location_longitude = location.getLongitude();

                distance = checkDistance.distance(destination_latitude, destination_longitude, location_latitude, location_longitude, 'K');

                Intent intent = new Intent("location_update");
                intent.putExtra("coordinates_lat", location.getLatitude());
                intent.putExtra("coordinates_long", location.getLongitude());
                sendBroadcast(intent);

            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        };
        manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String input = intent.getStringExtra("input");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Next Stop")
                .setContentText(input)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.train_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(6, notification);

        Timer t = new Timer();
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

                            }
                        };
                        manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, listener);
                        Toast.makeText(getApplicationContext(), "GPS Distance " + location_latitude+": "+location_longitude, Toast.LENGTH_LONG).show();
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
            manager.removeUpdates(listener);
        }
    }


}//end of class
