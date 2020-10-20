package com.example.nextstop;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class MainActivity extends AppCompatActivity {

    /*

    To Do list
    1. Create a list of Station Objects from JSON
    2. Get current location
    3. Create UI to accept destination (fetch possible destinations)

     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String stationsJSON = "";

        try {
            InputStream inputStream = getAssets().open("stations.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            stationsJSON = new String(buffer);
            createStationList(stationsJSON);
            //  Log.d("Json", "onCreate: "+stationsJSON);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createStationList(String json) {
        Type stationType = new TypeToken<ArrayList<Station>>() {
        }.getType();
        List<Station> stations = new Gson().fromJson(json, stationType);
        printStations(stations);
     //   Log.d("MainActivity", "createStationList: " + stations.size());
    }

    public void printStations(List<Station> stations) {

        Collections.sort(stations);
        for(Station station : stations) {
            System.out.println("Name: "+station.getStationId()+" , "+station.getStationDesc()+": Lon: "+station.getStationLongitude()+" : Lat: "+station.getStationLatitude());
        }
    }

}