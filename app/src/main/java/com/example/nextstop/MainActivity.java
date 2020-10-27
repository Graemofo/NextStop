package com.example.nextstop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.os.Bundle;
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
import androidx.core.app.ActivityCompat;
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

import static android.R.layout.simple_spinner_dropdown_item;

public class MainActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    /*
API KEY AIzaSyDQZV9qz4b5pj6PeD361ntTnxx6zZQbSlc
    To Do list
    1. Create a list of Station Objects from JSON
    2. Get current location (lat - lon)
    3. Create UI to accept destination (fetch possible destinations) Auto Complete
    4. Get destination location (lat - lon)
    5. Send alert when within 500m

     */

    LocationManager locationManager;
    TextView distanceView;
    GoogleMap map;
    AutoCompleteTextView editText;
    Button goButton;
    TextView textView;
    Spinner spinner;
    Ringtone ringtone;

    double dest_lat;
    double dest_long;
    double current_lat;
    double current_long;

    double full_distance;
    String stationsJSON = "";
    String luasJSON = "";
    String value = "";
    String spinner_text = "Train";

    public ArrayList<String> destinations = new ArrayList<>();
    public List<Station> stations_list;

    public ArrayList<String> luas_stops = new ArrayList<>();
    public List<Luas> luas_list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        goButton = findViewById(R.id.goButton);
        distanceView = findViewById(R.id.distanceView);
        textView = findViewById(R.id.textView2);
        spinner = findViewById(R.id.spinner);
        editText = findViewById(R.id.autoComplete);

        //Spinner
        ArrayAdapter<CharSequence> spin_adapter = ArrayAdapter.createFromResource(this, R.array.mode, android.R.layout.simple_spinner_item);
        spin_adapter.setDropDownViewResource(simple_spinner_dropdown_item);
        spinner.setAdapter(spin_adapter);
        spinner.setOnItemSelectedListener(this);

        setupStations();


        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }

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

                onMapReady(map);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }//end of onCreate

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
        printStations(stations);
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

        full_distance = distance(dest_lat, dest_long, current_lat, current_long, 'K');
        Log.d("Distance", "getDestinationLatLong: " + full_distance + " " + dest_lat + dest_long + " " + current_lat + current_long);
        // Toast.makeText(this, "Distance " + full_distance + "km \n", Toast.LENGTH_LONG).show();
        distanceView.setText((double) full_distance + " km to destination");
    }

    public void getLuasDestinationLatLong(String dest) {
        for (Luas station : luas_list) {
            if (station.get__text().equals(dest)) {
                dest_lat = station.get_lat();
                dest_long = station.get_long();
            }
        }
        full_distance = distance(dest_lat, dest_long, current_lat, current_long, 'K');
        Log.d("Distance", "getDestinationLatLong: " + full_distance + " " + dest_lat + dest_long + " " + current_lat + current_long);
        // Toast.makeText(this, "Distance " + full_distance + "km \n", Toast.LENGTH_LONG).show();
        distanceView.setText((double) full_distance + " km to destination");
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {

        current_lat = location.getLatitude();
        current_long = location.getLongitude();
        //   Toast.makeText(this, "Distance Changed \n" + location.getLatitude() + " : " + location.getLongitude() + " " + current_lat + " " + current_long, Toast.LENGTH_LONG).show();
        double newDistance = distance(current_lat, current_long, dest_lat, dest_long, 'K');
        if (!value.equals("")) {
            distanceView.setText((double) newDistance + " km to destination");
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //   Toast.makeText(this, "Changed Status ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    public double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == 'K') {
            dist = dist * 1.609344;
        } else if (unit == 'N') {
            dist = dist * 0.8684;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        df.format(dist);
        return Double.parseDouble((df.format(dist)));
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

// 53.4478761,-6.1468557

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
       // Toast.makeText(this, "Mode: " + spinner_text, Toast.LENGTH_LONG).show();
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
} //end of class