package com.example.nextstop;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.R.layout.simple_spinner_dropdown_item;

public class MainActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    /*
     *
     * 1. Stop Alarm: Action Button
     * 2. Cancel Alarm: Action Button
     * 3. Move Notification Channel to App class
     * 4. Fix maps zoom
     * 5. Animate Stop Button
     * 6. Check location passed
     * 7. Add icon to edittext
     * 8. Ask Location Permisiion https://stackoverflow.com/questions/40142331/how-to-request-location-permission-at-runtime
     * */

    public static LocationManager locationManager;
    TextView distanceView;
    GoogleMap map;
    GoogleApiClient googleApiClient;
    AutoCompleteTextView editText;
    Button setButton, stopButton, stopService;
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
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;


    String stationsJSON = "";
    String luasJSON = "";
    String value = "";
    String spinner_text = "Train";
    String provider;

    Intent intent;

    public ArrayList<String> destinations = new ArrayList<>();
    public List<Station> stations_list;

    public ArrayList<String> luas_stops = new ArrayList<>();
    public List<Luas> luas_list;

    private Animation animShow, animHide; // http://findnerd.com/list/view/HideShow-a-View-with-slide-updown-animation-in-Android/2537/#:~:text=In%20android%2C%20we%20simply%20hide,the%20visibility%20of%20the%20view.

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        checkLocationPermission();
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        registerReceiver(broadcastReceiverTWO, new IntentFilter("STOP_ALERTS"));

        animShow = AnimationUtils.loadAnimation(this, R.anim.view_show);
        animHide = AnimationUtils.loadAnimation(this, R.anim.view_hide);

        setupStations();
        createNotificationChannel();
        getLocation();
        enableLoc();

        intent = new Intent(getApplicationContext(), GPS_Service.class);

        notifications = new Notifications();
        checkDistance = new CheckDistance();

        setButton = findViewById(R.id.goButton);
        stopService = findViewById(R.id.stop_service_button);
        stopButton = findViewById(R.id.stopAlarm);
        distanceView = findViewById(R.id.distanceView);
        textView = findViewById(R.id.textView2);
        spinner = findViewById(R.id.spinner);
        editText = findViewById(R.id.autoComplete);

        //hide buttons
        stopButton.setVisibility(View.INVISIBLE);
        stopService.setVisibility(View.INVISIBLE);

        //Spinner
        ArrayAdapter<CharSequence> spin_adapter = ArrayAdapter.createFromResource(this, R.array.mode, android.R.layout.simple_spinner_item);
        spin_adapter.setDropDownViewResource(simple_spinner_dropdown_item);
        spinner.setAdapter(spin_adapter);
        spinner.setOnItemSelectedListener(this);

        //Set Up Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                value = editText.getText().toString();
                closeKeyboard();
                map.clear();
                editText.setText("");
                textView.setText(value);
                if (spinner_text.equals("Train")) {
                    getDestinationLatLong(value);
                } else {
                    getLuasDestinationLatLong(value);
                }
                intent.putExtra("input", value + ":  " + full_distance + "km to destination");
                intent.putExtra("lat", dest_lat);
                intent.putExtra("long", dest_long);
                startService(intent);
                onMapReady(map);
                stopService.setVisibility(View.VISIBLE);
                stopService.startAnimation(animShow);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ringtone.stop();
                stopButton.startAnimation(animHide);
                stopButton.setVisibility(View.GONE);
                stopAlerts();
                distanceView.setText("");
                textView.setText("Arrived");
                stopService.startAnimation(animHide);
                stopService.setVisibility(View.GONE);
                intent.putExtra("STOP_SERVICE", true);
                startService(intent);
                locationManager.removeUpdates(MainActivity.this);
            }
        });

        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("STOP_SERVICE", true);
                startService(intent);
                getApplicationContext().stopService(new Intent(MainActivity.this, GPS_Service.class));
                stopService.startAnimation(animHide);
                stopService.setVisibility(View.GONE);
            }
        });

    }//end of onCreate

    BroadcastReceiver broadcastReceiverTWO = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Test Broadcast Reciever", "onReceive: Stop Alarms");
            stopAlerts();
        }
    };


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
        boolean isReal = false;
        for (Station station : stations_list) {
            if (station.getStationDesc().equals(dest)) {
                isReal = true;
                dest_lat = station.getStationLatitude();
                dest_long = station.getStationLongitude();
                full_distance = checkDistance.distance(dest_lat, dest_long, current_lat, current_long, 'K');
                Log.d("Distance", "getDestinationLatLong: " + full_distance + " " + dest_lat + dest_long + " " + current_lat + current_long);
                distanceView.setText(full_distance + " km to destination");
            }
        }
        if (!isReal) {
            Toast.makeText(getApplicationContext(), "Please enter a station from the list", Toast.LENGTH_LONG).show();
        }
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
        map.clear();
        onMapReady(map);
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
            onMapReady(map);
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
        full_distance = checkDistance.distance(dest_lat, dest_long, current_lat, current_long, 'K');
        Log.d("Main Activity", "onMapReady: " + current_lat + " " + current_long);

        LatLng destination = new LatLng(dest_lat, dest_long);
        LatLng current_location = new LatLng(current_lat, current_long);
        map.addMarker(new MarkerOptions().position(destination).title("Destination: " + value).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        map.addMarker(new MarkerOptions().position(current_location).title("You are here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        map.moveCamera(CameraUpdateFactory.newLatLng(current_location));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(current_location, 12));
        map.addCircle(new CircleOptions()
                .center(new LatLng(current_lat, current_long))
                .radius(getRadius())
                .strokeWidth(2)
                .strokeColor(Color.WHITE));

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
        stopButton.startAnimation(animShow);
        manageBlinkEffect();
        Intent activityIntent = this.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
        activityIntent.setAction("android.intent.action.MAIN");
        activityIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent pending = PendingIntent.getActivity(this, 0, activityIntent, 0);

        Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
        broadcastIntent.setAction(App.STOP_ALARM);
        broadcastIntent.putExtra("cancel", "Alarm Stopped");
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "nextStop")
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .setSmallIcon(R.drawable.location_foreground)
                .setContentTitle("Next Stop")
                .setContentText("You are pulling in to " + value)
                .addAction(R.drawable.train_foreground, "Stop Alarm", actionIntent)
                .setAutoCancel(true)
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
        stopService(intent);
        getApplicationContext().stopService(new Intent(MainActivity.this, GPS_Service.class));
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
        Log.d("MAP", "onResume: MainActivity" + value);
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
        finishAndRemoveTask();
        unregisterReceiver(broadcastReceiverTWO);

    }

    private void manageBlinkEffect() {
        ObjectAnimator anim = ObjectAnimator.ofInt(stopButton, "backgroundColor",
                Color.WHITE,
                Color.RED,
                Color.WHITE);
        anim.setDuration(1500);
        anim.setEvaluator(new ArgbEvaluator());
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.start();
    }

//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                locationManager.requestLocationUpdates(provider, 400, 1, (LocationListener) this);
//            }
//        } else {
//
//        }
//        return;
//    }


    private void enableLoc() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(MainActivity.this).addApi(LocationServices.API).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                }

                @Override
                public void onConnectionSuspended(int i) {
                    googleApiClient.connect();
                }
            }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    Log.d("Location error", "Location error " + connectionResult.getErrorCode());
                }
            }).build();
            googleApiClient.connect();
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);
            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(MainActivity.this, 101);

//                                finish();
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                    }
                }
            });
        }
    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Title")
                        .setMessage("Message")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        locationManager.requestLocationUpdates(provider, 400, 1, this);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

        }
    }


} //end of class