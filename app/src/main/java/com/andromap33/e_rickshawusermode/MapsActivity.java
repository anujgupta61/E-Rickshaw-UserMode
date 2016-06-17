package com.andromap33.e_rickshawusermode;

import android.app.Activity;
import android.provider.Settings;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener , LocationListener {

    private GoogleMap mMap;
    Boolean flag = true ;
    // user location finding
    private  long sTime =0 ,finalTime=0;
    private GoogleApiClient mGoogleApiClient;               // creating google API client
    private LocationRequest mLocationRequest;
    private final String LOG_TAG = "ACTIVITY main";
    private double latitude,longitude;
    ArrayList<Marker> markerList = new ArrayList<Marker> () ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(!checkConnection(this)) {
             showInternetNotAvailableAlert(this); // by mohit
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Check Permissions Now
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        1 );
        }

    }

    public void showInternetNotAvailableAlert(Activity activity)
    {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("NO INTERNET")
                    .setMessage("Please enable internet")
                    .setCancelable(true)
                    .setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog . cancel() ;
                        }
                    });


            AlertDialog alert = builder.create();
            alert.show();

        }
        catch(Exception e)
        {
            
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == 1) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ShowGPSSettings(MapsActivity.this) ;
                this.recreate() ;
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(getApplicationContext(), "You must grant permission to access the gps and use map ...", Toast.LENGTH_LONG).show();
            }
        }
    }

    boolean checkConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected ;
    }

    public void ShowGPSSettings(Activity activity) {
        String provider = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.contains("gps")){ //if gps is disabled
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("NO GPS")
                        .setMessage("Please select High Accuracy Location Mode")
                        .setCancelable(true)
                        .setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog . cancel() ;
                            }
                        })
                        .setNegativeButton("GPS Settings",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) ;
                            }
                        });


                AlertDialog alert = builder.create();
                alert.show();

            }
            catch(Exception e)
            {
                
            }
        }        
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        ShowGPSSettings(MapsActivity.this) ;
        mGoogleApiClient.connect();
    }

    @Override
    protected  void onStop()
    {   
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        mLocationRequest = LocationRequest.create();

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationRequest.setInterval(5000);

        try
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest , this);
        }
        catch (SecurityException e)
        {
            Log.v(LOG_TAG,"ERROR FETCH USER LOCATION");
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been suspended .");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_TAG, location.toString());
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        if (flag) {
            LatLng pu = new LatLng(latitude, longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pu, 15.5f));
            flag = false ;
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
        }
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new FetchTheLocation(), 0, 5000);
    }

    class FetchTheLocation extends TimerTask {

            public void run() {
                ShowGPSSettings(MapsActivity.this) ;
            class wrapper {
                int d_id;
                double lat, lng;
            }

            class getthedata extends AsyncTask<String, Void, wrapper[]> {

                wrapper[] w;

                @Override
                protected wrapper[] doInBackground(String... params) {
                    URL url;
                    HttpURLConnection urlConnection = null;
                    try {
                        url = new URL("http://andromap33.orgfree.com/json.php");

                        urlConnection = (HttpURLConnection) url
                                .openConnection();

                        InputStream in = urlConnection.getInputStream();

                        InputStreamReader isw = new InputStreamReader(in);

                        int data = isw.read();
                        String json_str = "{ location: ";
                        while (data != -1) {
                            char current = (char) data;
                            data = isw.read();
                            json_str = json_str + current;
                        }
                        json_str = json_str + "}";
                        final JSONObject obj = new JSONObject(json_str);
                        final JSONArray geodata = obj.getJSONArray("location");
                        final int n = geodata.length();
                        if(n == 0)
                            return null ;
                        w = new wrapper[n];
                        for (int i = 0; i < n; i++) {
                            final JSONObject location = geodata.getJSONObject(i);
                            w[i] = new wrapper();
                            w[i].d_id = location.getInt("driver_id");
                            w[i].lat = location.getDouble("latitude");
                            w[i].lng = location.getDouble("longitude");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    }
                    return w;
                }

                @Override
                protected void onPostExecute(wrapper[] w) {
                    super.onPostExecute(w);
                    for (int i = 0; i < markerList . size() ; i ++) {
                        markerList . get(i) . remove() ;   
                    }
                    markerList . clear() ;
                    if (w != null) {          
                        for (int i = 0; i < w.length; i ++) {   
                            Marker temp = mMap.addMarker(new MarkerOptions() . position(new LatLng(w[i] . lat , w[i] . lng)).title(w[i] . d_id + ""));
                            markerList . add(temp) ;                         
                        }
                    }
                }
            }
            getthedata get = new getthedata();
            get.execute();
        }
    }
}