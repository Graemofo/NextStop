package com.example.nextstop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

import static android.R.layout.simple_spinner_dropdown_item;

public class MainActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    /*
    *
    * 1. Stop Alarm: Action Button
    * 2. Cancel Alarm: Action Button
    * 3. Move Notification Channel to App class */

    LocationManager locationManager;
    TextView distanceView;
    GoogleMap map;
    AutoCompleteTextView editText;
    Button goButton, stopButton;
    TextView textView;
    Spinner spinner;
    Ringtone ringtone;
    Notifications notifications;
    CheckDistance checkDistance;
    Vibrator vibrator;
    NotificationManagerCompat notificationManagerCompat;
    BroadcastReceiver broadcastReceiver;
    SupportMapFragment mapFragment;

    static double dest_lat;
    static double dest_long;
    static double current_lat;
    static double current_long;

    double full_distance;
    double newDistance;
    boolean oneTime = true;
    boolean atDestination = false;

    String stationsJSON = "";
    String luasJSON = "";
    String value = "";
    String spinner_text = "Train";

    Intent intent;

    public ArrayList<String> destinations = new ArrayList<>();
    public List<Station> stations_list;

    public ArrayList<String> luas_stops = new ArrayList<>();
    public List<Luas> luas_list;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupStations();
        createNotificationChannel();

        final Button stopService = findViewById(R.id.stop_service_button);
        intent = new Intent(getApplicationContext(), GPS_Service.class);

        notifications = new Notifications();
        checkDistance = new CheckDistance();

        goButton = findViewById(R.id.goButton);
        stopButton = findViewById(R.id.stopAlarm);
        distanceView = findViewById(R.id.distanceView);
        textView = findViewById(R.id.textView2);
        spinner = findViewById(R.id.spinner);
        editText = findViewById(R.id.autoComplete);

        stopButton.setVisibility(View.INVISIBLE);


        //Spinner
        ArrayAdapter<CharSequence> spin_adapter = ArrayAdapter.createFromResource(this, R.array.mode, android.R.layout.simple_spinner_item);
        spin_adapter.setDropDownViewResource(simple_spinner_dropdown_item);
        spinner.setAdapter(spin_adapter);
        spinner.setOnItemSelectedListener(this);

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeKeyboard();
                map.clear();
                value = editText.getText().toString();
                editText.setText("");
                textView.setText(value);
                if (spinner_text.equals("Train")) {
                    getDestinationLatLong(value);
                } else {
                    getLuasDestinationLatLong(value);
                }
                intent.putExtra("input", value+ ":  "+full_distance+"km to destination");
                startService(intent);
                // ringAlarm();
                onMapReady(map);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ringtone.stop();
                stopButton.setVisibility(View.INVISIBLE);
                stopAlerts();
            }
        });

        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             //   stopService(intent);
                getApplicationContext().stopService(new Intent(MainActivity.this, GPS_Service.class));
            }
        });

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

    }//end of onCreate

    public static double getDest_lat() {
        return dest_lat;
    }

    public static double getDest_long() {
        return dest_long;
    }

    public void setupStations() {
        try {
            InputStream inputStream = getAssets().open("stations.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            stationsJSON = new String(buffer);
            stations_list = createStationList(stationsJSON);  // List of Station objects
            destinations = createDestinations(stations_list); // List of Station names

            InputStream luasInputStream = getAssets().open("luas.json");
            int inputSize = luasInputStream.available();
            byte[] luasBuffer = new byte[inputSize];
            luasInputStream.read(luasBuffer);
            luasJSON = new String(luasBuffer);
            luas_list = createLuasList(luasJSON);           // List of Luas objects
            luas_stops = createLuasDestinations(luas_list); // List of luas Station names

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("MissingPermission")
    public void getLocation() {
        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, MainActivity.this);
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            current_lat = locationGPS.getLatitude();
            current_long = locationGPS.getLongitude();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Station> createStationList(String json) {
        Type stationType = new TypeToken<ArrayList<Station>>() {
        }.getType();
        List<Station> stations = new Gson().fromJson(json, stationType);
        stations_list = stations;

        return stations;
    }

    public List<Luas> createLuasList(String json) {
        Type luasType = new TypeToken<ArrayList<Luas>>() {
        }.getType();
        List<Luas> luasStations = new Gson().fromJson(json, luasType);
        luas_list = luasStations;

        return luasStations;
    }

    public void printStations(List<Station> stations) {
        Collections.sort(stations);
        for (Station station : stations) {
            destinations.add(station.getStationDesc());
            System.out.println("Name: " + station.getStationId() + " , " + station.getStationDesc() + ": Lon: " + station.getStationLongitude() + " : Lat: " + station.getStationLatitude());
        }
    }

    public ArrayList<String> createDestinations(List<Station> stations) {
        for (Station station : stations) {
            destinations.add(station.getStationDesc());
        }
        return destinations;
    }

    public ArrayList<String> createLuasDestinations(List<Luas> stations) {
        for (Luas station : stations) {
            luas_stops.add(station.get__text());
        }
        return luas_stops;
    }

    public void getDestinationLatLong(String dest) {
        for (Station station : stations_list) {
            if (station.getStationDesc().equals(dest)) {
                dest_lat = station.getStationLatitude();
                dest_long = station.getStationLongitude();
            }
        }
        full_distance = checkDistance.distance(dest_lat, dest_long, current_lat, current_long, 'K');
        Log.d("Distance", "getDestinationLatLong: " + full_distance + " " + dest_lat + dest_long + " " + current_lat + current_long);
        distanceView.setText(full_distance + " km to destination");
    }

    public void getLuasDestinationLatLong(String dest) {
        for (Luas station : luas_list) {
            if (station.get__text().equals(dest)) {
                dest_lat = station.get_lat();
                dest_long = station.get_long();
            }
        }
        full_distance = checkDistance.distance(dest_lat, dest_long, current_lat, current_long, 'K');
        Log.d("Distance", "getDestinationLatLong: " + full_distance + " " + dest_lat + dest_long + " " + current_lat + current_long);
        distanceView.setText(full_distance + " km to destination");
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onLocationChanged(@NonNull Location location) {

        double threshold = 0.50;
        current_lat = location.getLatitude();
        current_long = location.getLongitude();
        newDistance = checkDistance.distance(dest_lat, dest_long, current_lat, current_long, 'K');
        if (!value.equals("")) {
            distanceView.setText(newDistance + " km to destination");
        }
        if (newDistance <= threshold && oneTime) {
            oneTime = false;
            stopButton.setVisibility(View.VISIBLE);
            stopService(intent);
            ringAlarm();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        getLocation();
        map = googleMap;
        MapStyleOptions mapStyleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style);
        map.setMapStyle(mapStyleOptions);

        Log.d("TAG", "onMapReady: " + current_lat + " " + current_long);

        LatLng destination = new LatLng(dest_lat, dest_long);
        LatLng current_location = new LatLng(current_lat, current_long);
        map.addMarker(new MarkerOptions().position(destination).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
        map.addMarker(new MarkerOptions().position(current_location).title("You are here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
        map.moveCamera(CameraUpdateFactory.newLatLng(current_location));
        map.animateCamera(CameraUpdateFactory.zoomTo(12.0f));
        map.addCircle(new CircleOptions()
                .center(new LatLng(current_lat, current_long))
                .radius(getRadius())
                .strokeColor(Color.CYAN));
    }

    public int getRadius() {
        return (int) (full_distance * 1000);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        ((TextView) adapterView.getChildAt(0)).setTextColor(Color.WHITE);
        spinner_text = adapterView.getItemAtPosition(i).toString();
        if (spinner_text.equals("Train")) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, destinations);
            editText.setAdapter(adapter);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, luas_stops);
            editText.setAdapter(adapter);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void ringAlarm() {
        stopButton.setVisibility(View.VISIBLE);
        Intent activityIntent = this.getPackageManager()
                .getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
        PendingIntent pending = PendingIntent.getActivity(this, 0, activityIntent, 0);

        Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
        broadcastIntent.putExtra("cancel", "Alarm Stopped");
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "nextStop")
                .setSmallIcon(R.drawable.train_foreground)
                .setContentTitle("Next Stop")
                .setContentText("You are pulling in to " + value)
                .addAction(R.drawable.train_foreground, "Stop Alarm", actionIntent)
                .setAutoCancel(true)
                .setColor(Color.WHITE)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending);
        notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(6, builder.build());


        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(5000);


        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();

    }

    public void stopAlerts() {
        if (ringtone != null) {
            ringtone.stop();
            notificationManagerCompat.cancelAll();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "NextStopChannel";
            String desrcription = "Channel for all modes of transport ";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("nextStop", name, importance);
            channel.setDescription(desrcription);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MAP", "onResume: MainActivity"+value);
        if (map != null) {
            map.clear();
            textView.setText(value);
            if (spinner_text.equals("Train")) {
                getDestinationLatLong(value);
            } else {
                getLuasDestinationLatLong(value);
            }
            onMapReady(map);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy: ");
        getApplicationContext().stopService(new Intent(this, GPS_Service.class));
    }
} //end of class