package com.example.google_maps;

import androidx.fragment.app.FragmentActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.google_maps.databinding.ActivityMapsBinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private TextView feedback;
    private int markerCnt = 0;
    private String appKey = "";
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private double _lat = 51.5078;
    private double _long = 5.3978;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        feedback = findViewById(R.id.feedback);
        appKey = getString(R.string.app_key);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng ibk = new LatLng(47.4726318,11.9864509);
        mMap.addMarker(new MarkerOptions().position(ibk).title("Marker in Innsbruck"));

        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ibk,10));
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);

    }

    @Override
    public void onMapClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(String.format("Marker %d", ++markerCnt)));
        feedback.setText("New Marker created!");
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        feedback.setText(marker.getTitle());
        return true;
    }

    public void getEveryMarker(View view) {
        GetServerAsyncTask asyncTask = new GetServerAsyncTask();
        asyncTask.execute("");
    }

    public void putMarker(View view) {
        PutServerAsyncTask putTask = new PutServerAsyncTask();
        putTask.execute();
    }

    private class GetServerAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                .url(getString(R.string.url_get))
                .addHeader("Accept", "application/json")
                .addHeader("appkey", appKey)
                .get()
                .build();

            Response response = null;

            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
                response = null;
            }

            if (response != null && response.isSuccessful()) {
                try {
                    return response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return "Loading Data Failed";
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            result = result.replaceAll("long", "_long");
            result = result.replaceAll("lat", "_lat");

            Gson gson = new Gson();
            Data[] data = gson.fromJson(result, Data[].class);

            for(int i = 0; i < data.length; i++) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(data[i].get_lat(), data[i].get_long())).title(data[i].getName()));
            }
        }

    }

    private class PutServerAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            OkHttpClient client = new OkHttpClient();
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("info", new JSONObject()
                        .put("lat", _lat)
                        .put("long", _long)
                        .put("message", "test")
                        .put("name", "Alex"));

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
            Request request = new Request.Builder()
                    .url(getString(R.string.url_put))
                    .addHeader("Accept", "application/json")
                    .addHeader("appkey", appKey)
                    .put(body)
                    .build();

            Response response = null;

            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
                response = null;
            }

            if (response != null && response.isSuccessful()) {
                try {
                    return response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return "Upload failed";
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            feedback.setText(result);
            Gson gson = new Gson();
            Data data = gson.fromJson(result, Data.class);
            feedback.append("\n");
            feedback.append(String.format("TITLE: %s\n",data.getName()));
        }
    }
    class Data {
        private int id;
        private String name;
        private double _long;
        private double _lat;
        private String message;
        private String when;

        public String getName() {
            return name;
        }

        public double get_long() {
            return _long;
        }

        public double get_lat() {
            return _lat;
        }
    }

}