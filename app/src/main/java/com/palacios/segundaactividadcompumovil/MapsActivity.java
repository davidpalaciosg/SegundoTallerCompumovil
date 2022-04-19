package com.palacios.segundaactividadcompumovil;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.palacios.segundaactividadcompumovil.Utilities.MyLocation;

import org.json.JSONArray;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;

public class MapsActivity extends AppCompatActivity {
    public static final double RADIUS_OF_EARTH_M=6371000;

    EditText locationText;
    MapView map;

    //locationRequest with google
    private FusedLocationProviderClient mFusedLocationClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    Location lastLocation;
    boolean settingsOK=false;

    //Saves prevoius Lat and Log to calculate distance between movements
    boolean firstMovement;
    double previousLat=0;
    double previousLong=0;
    double currentLat=0;
    double currentLong=0;
    //Saves all movements
    JSONArray locations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Initialize attributes
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest=createLocationRequest();
        locationCallback=createLocationCallBack();
        firstMovement = false;
        locations= new JSONArray();

        //Inflate
        locationText = findViewById(R.id.locationText);

        //Ask Permission
        getSinglePermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        //Check if GPS is ON
        checkLocationSettings();

        //MAPS
        startMap();

    }

    private void startMap() {
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_maps);
        map =findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(18.0);
        updateLocationOnMap(lastLocation);

    }

    private void updateLocationOnMap(Location location)
    {
        if(lastLocation!=null)
        {
            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            IMapController mapController = map.getController();
            mapController.setCenter(startPoint);
            createMarker(location);
        }
    }

    private void createMarker(Location location)
    {
        //Create marker
        GeoPoint markerPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        Marker marker = new Marker(map);
        marker.setTitle("Tu ubicación");
        Drawable myIcon =
                getResources().getDrawable(R.drawable.ic_baseline_location_on_24,
                        this.getTheme());
        marker.setIcon(myIcon);
        marker.setPosition(markerPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
    }

    private void checkLocationSettings(){
        LocationSettingsRequest.Builder builder = new
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i("LOCATION", "GPS is ON");
                settingsOK = true;
                startLocationUpdates();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(((ApiException) e).getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED){
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    IntentSenderRequest isr = new IntentSenderRequest.Builder(resolvable.getResolution()).build();
                    getLocationSettings.launch(isr);
                }else {
                    locationText.setText("No GPS available");
                }
            }
        });
    }


    private LocationRequest createLocationRequest(){
        LocationRequest request= LocationRequest.create().setFastestInterval(5000).setInterval(10000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }
    private LocationCallback createLocationCallBack(){
        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //Get previous Location
                if(lastLocation!=null) {
                    previousLat = currentLat;
                    previousLong = currentLong;
                    firstMovement=true;
                }
                //Get Location from Google Services
                lastLocation= locationResult.getLastLocation();
                if(lastLocation!=null){

                    currentLong = lastLocation.getLongitude();
                    currentLat = lastLocation.getLatitude();

                    //If theres a movement, update map with the current location
                    if(currentLong!=previousLong && currentLat!=previousLat)
                        updateLocationOnMap(lastLocation);

                    String txt = "Latitude: "+currentLat + " ,Longitude: " +currentLong;
                    Log.i("LOCATION", txt);

                    //Detect if there exists a first movement before detect 30 meters movement
                    if(firstMovement==true)
                    {
                        //Detect 30 meters movement
                        if(is30MetersForward(previousLat,previousLong, currentLat, currentLong))
                        {
                            Log.i("LOCATION", "MOVEMENT");
                            writeLocationObject(lastLocation);
                        }


                    }
                }
            }
        };
    }
    private void startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            if(settingsOK){
                mFusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null);//looper: cada cuanto quiere que lo haga
            }
        }
    }

    //Ask for permission
    ActivityResultLauncher<String> getSinglePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result == true) { //granted
                        startLocationUpdates();

                    }
                    else {//denied
                    }
                }
            });

    //Turn Location settings (GPS) ON
    ActivityResultLauncher<IntentSenderRequest> getLocationSettings = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.i("LOCATION","Result from settings:"+result.getResultCode());
                    if(result.getResultCode()==RESULT_OK){
                        settingsOK=true;
                        startLocationUpdates();
                    }else{
                        locationText.setText("GPS is unavailable");
                    }
                }
            }
    );

    public boolean is30MetersForward(double lat1, double long1, double lat2, double long2)
    {
        double dist = distance(lat1,long1,lat2,long2);
        if(dist>30)
            return true;
        return false;
    }


    public double distance(double lat1, double long1, double lat2, double long2) {
        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(long1 - long2);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double result = RADIUS_OF_EARTH_M * c;
        return Math.round(result*100.0)/100.0;
    }


    public void writeLocationObject(Location lastLocation){
        //Create object myLocation
        MyLocation myLocation = new MyLocation();
        myLocation.setDate(new Date(System.currentTimeMillis()));
        myLocation.setLatitude(lastLocation.getLatitude());
        myLocation.setLongitude(lastLocation.getLongitude());

        //Convert location to JSON and add to locations
        locations.put(myLocation.toJSON());
        Writer output=null;
        String filename= "locations.json";
        try {
            File file = new File(getBaseContext().getExternalFilesDir(null), filename);
            Log.i("LOCATION", "Ubicacion de archivo: "+file);
            output = new BufferedWriter(new FileWriter(file));
            output.write(locations.toString());
            output.close();
            Log.i("LOCATION","Location saved") ;
        } catch (Exception e) {
                //Log error
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        //Location
        mFusedLocationClient.removeLocationUpdates(locationCallback);

        //Maps
        map.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Location
        startLocationUpdates();
        //Maps
        map.onResume();

        if(lastLocation!=null)
        {
            IMapController mapController = map.getController();
            GeoPoint startPoint = new GeoPoint(currentLong, currentLat);
            mapController.setZoom(18.0);
            mapController.setCenter(startPoint);
        }

        //Change map color by theme
        UiModeManager uiManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        if(uiManager.getNightMode() == UiModeManager.MODE_NIGHT_YES )
            map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);

    }
}