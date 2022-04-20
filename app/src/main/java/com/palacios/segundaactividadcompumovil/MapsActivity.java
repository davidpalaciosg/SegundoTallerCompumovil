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
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.palacios.segundaactividadcompumovil.Utilities.MyLocation;

import org.json.JSONArray;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapsActivity extends AppCompatActivity {
    public static final double RADIUS_OF_EARTH_M = 6371000;

    private EditText searchAddress;
    Button searchButton;

    //Maps
    private MapView map;
    private Marker markerLastLocation;
    private Marker longPressedMarker;
    private RoadManager roadManager;
    private Polyline roadOverlay;


    //Location
    //locationRequest with google
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private boolean settingsOK = false;
    //Saves previous Lat and Log to calculate distance between movements
    private boolean firstMovement;
    private double previousLat = 0;
    private double previousLong = 0;
    private double currentLat = 0;
    private double currentLong = 0;
    private JSONArray locations; //Saves all movements

    //Light Sensor
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener lightSensorListener;

    //Geocoder
    private Geocoder mGeocoder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);



        //Inflate
        searchAddress = findViewById(R.id.searchAddress);
        searchButton = findViewById(R.id.searchButton);
        //startTextListener();
        startSearchButton();


        //Initialize attributes
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = createLocationRequest();
        locationCallback = createLocationCallBack();
        firstMovement = false;
        locations = new JSONArray();

        //Ask Permission
        getSinglePermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        //Check if GPS is ON
        checkLocationSettings();

        //Geocoder
        mGeocoder = new Geocoder(getBaseContext());

        //Light sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        lightSensorListener = createLightEventListener();

        //MAPS
        startMap();
        map.getOverlays().add(createOverlayEvents());

        //ROAD MANAGER
        roadManager = new OSRMRoadManager(this, "ANDROID");


    }

    //MAPS

    private void startMap() {
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_maps);
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(18.0);
        updateLocationOnMap(lastLocation);

        markerLastLocation = new Marker(map);

    }

    private void updateLocationOnMap(Location location) {
        if (location != null) {
            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            IMapController mapController = map.getController();
            mapController.setCenter(startPoint);
            createMarkerLastLocation();
        }
    }

    private void updateLocationOnMap(MyLocation location) {
        if (location != null) {
            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            IMapController mapController = map.getController();
            mapController.setCenter(startPoint);
        }
    }

    //Create a marker for the last location
    private void createMarkerLastLocation() {

        //Delete last location marker
        map.getOverlays().remove(markerLastLocation);
        //Create marker
        GeoPoint markerPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
        markerLastLocation.setTitle("Tu ubicación");
        Drawable myIcon =
                getResources().getDrawable(R.drawable.ic_baseline_location_on_24,
                        this.getTheme());
        markerLastLocation.setIcon(myIcon);
        markerLastLocation.setPosition(markerPoint);
        markerLastLocation.setAnchor(Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(markerLastLocation);
    }

    private void createMarker(MyLocation location) {
        //Create marker
        GeoPoint markerPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        Marker marker = new Marker(map);
        marker.setTitle("Tu marcador");
        Drawable myIcon =
                getResources().getDrawable(R.drawable.ic_baseline_location_on_24,
                        this.getTheme());
        marker.setIcon(myIcon);
        marker.setPosition(markerPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
    }

    private Marker createMarker(GeoPoint p, String title, String desc, int iconID){
        Marker marker = null;
        if(map!=null) {
            marker = new Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                Drawable myIcon = getResources().getDrawable(iconID, this.getTheme());
                marker.setIcon(myIcon);
            }
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }
        return marker;
    }


    private void deleteMarkerFromMap(Marker marker) {
        map.getOverlays().remove(marker);
    }

    //LOCATION

    private void checkLocationSettings() {
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
                if (((ApiException) e).getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    IntentSenderRequest isr = new IntentSenderRequest.Builder(resolvable.getResolution()).build();
                    getLocationSettings.launch(isr);
                } else {
                    //locationText.setText("No GPS available");
                }
            }
        });
    }


    private LocationRequest createLocationRequest() {
        LocationRequest request = LocationRequest.create().setFastestInterval(5000).setInterval(10000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }

    private LocationCallback createLocationCallBack() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //Get previous Location
                if (lastLocation != null) {
                    previousLat = currentLat;
                    previousLong = currentLong;
                    firstMovement = true;
                }
                //Get Location from Google Services
                lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {

                    currentLong = lastLocation.getLongitude();
                    currentLat = lastLocation.getLatitude();

                    //If theres a movement, update map with the current location
                    if (currentLong != previousLong && currentLat != previousLat)
                        updateLocationOnMap(lastLocation);

                    String txt = "Latitude: " + currentLat + " ,Longitude: " + currentLong;
                    Log.i("LOCATION", txt);

                    //Detect if there exists a first movement before detect 30 meters movement
                    if (firstMovement) {
                        //Detect 30 meters movement
                        if (is30MetersForward(previousLat, previousLong, currentLat, currentLong)) {
                            Log.i("LOCATION", "MOVEMENT");
                            writeLocationObject(lastLocation);
                        }
                    }
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (settingsOK) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);//looper: cada cuanto quiere que lo haga
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

                    } else {//denied
                    }
                }
            });

    //Turn Location settings (GPS) ON
    ActivityResultLauncher<IntentSenderRequest> getLocationSettings = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.i("LOCATION", "Result from settings:" + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        settingsOK = true;
                        startLocationUpdates();
                    } else {
                        //locationText.setText("GPS is unavailable");
                    }
                }
            }
    );

    public boolean is30MetersForward(double lat1, double long1, double lat2, double long2) {
        double dist = distance(lat1, long1, lat2, long2);
        if (dist > 30)
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
        return Math.round(result * 100.0) / 100.0;
    }


    public void writeLocationObject(Location lastLocation) {
        //Create object myLocation
        MyLocation myLocation = new MyLocation();
        myLocation.setDate(new Date(System.currentTimeMillis()));
        myLocation.setLatitude(lastLocation.getLatitude());
        myLocation.setLongitude(lastLocation.getLongitude());

        //Convert location to JSON and add to locations
        locations.put(myLocation.toJSON());
        Writer output = null;
        String filename = "locations.json";
        try {
            File file = new File(getBaseContext().getExternalFilesDir(null), filename);
            Log.i("LOCATION", "Ubicacion de archivo: " + file);
            output = new BufferedWriter(new FileWriter(file));
            output.write(locations.toString());
            output.close();
            Log.i("LOCATION", "Location saved");
        } catch (Exception e) {
            //Log error
        }

    }


    //SENSOR LIGHT
    private SensorEventListener createLightEventListener() {
        return new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (map != null) {
                    if (sensorEvent.values[0] < 5000) {
                        //Log.i("MAPS", "DARK MAP " + sensorEvent.values[0]);
                        map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                    } else {
                        //Log.i("MAPS", "LIGHT MAP " + sensorEvent.values[0]);
                        map.getOverlayManager().getTilesOverlay().setColorFilter(new ColorFilter());
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
    }

    private MapEventsOverlay createOverlayEvents(){
        MapEventsOverlay overlayEventos = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                longPressOnMap(p);
                return true;
            }
        });
        return overlayEventos;
    }

    private void longPressOnMap(GeoPoint p){
        if(longPressedMarker!=null)
            map.getOverlays().remove(longPressedMarker);
        longPressedMarker = createMarker(p, "location", null, R.drawable.ic_baseline_location_on_26);
        map.getOverlays().add(longPressedMarker);

        double dist= distance(currentLat,currentLong,p.getLatitude(),p.getLongitude());
        Toast.makeText(this,"La distancia entre los 2 puntos es de: "+String.valueOf(dist), Toast.LENGTH_LONG).show();

        MyLocation loc = new MyLocation();
        loc.setLatitude(p.getLatitude());
        loc.setLongitude(p.getLongitude());

        updateLocationOnMap(loc);
        GeoPoint start = new GeoPoint(currentLat,currentLong);
        GeoPoint finish = new GeoPoint(loc.getLatitude(),loc.getLongitude());
        drawRoute(start,finish);
    }

    //GEOCODER
    public void startGeocoder() {
        String addressString = searchAddress.getText().toString();
        Log.i("GEO", "" + addressString);
        if (!addressString.isEmpty()) {
            try {
                List<Address> addresses = mGeocoder.getFromLocationName(addressString, 2);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addressResult = addresses.get(0);
                    LatLng position = new LatLng(addressResult.getLatitude(), addressResult.getLongitude());
                    if (map != null) {
                        //Agregar Marcador al mapa
                        Log.i("GEO", "Direccion: " + addressResult.toString());
                        MyLocation loc = new MyLocation();
                        loc.setLongitude(position.longitude);
                        loc.setLatitude(position.latitude);

                        createMarker(loc);

                    }
                } else {
                    Toast.makeText(MapsActivity.this, "Dirección no encontrada", Toast.LENGTH_SHORT).show();
                    Log.i("GEO", "Direccion no encontrada ");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MapsActivity.this, "La dirección esta vacía", Toast.LENGTH_SHORT).show();
            Log.i("GEO", "Direccion vacía ");
        }
    }
    public void startTextListener(){
        this.searchAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean action = false;
                Log.i("GEO","ENTRA");
                if (actionId == EditorInfo.IME_ACTION_SEND)
                {
                    Log.i("GEO","ENTRA");
                    // hide keyboard
                    //InputMethodManager inputMethodManager = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    //inputMethodManager.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    startGeocoder();
                    action = true;
                }
                return action;
            }
        });
    }
    void startSearchButton() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("GEO","ENTRA AL BOTON");
                startGeocoder();
            }
        });

    }


    @Override
    protected void onPause() {
        super.onPause();
        //LOCATION
        mFusedLocationClient.removeLocationUpdates(locationCallback);

        //MAPS
        map.onPause();

        //LIGHT SENSOR
        sensorManager.unregisterListener(lightSensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //LIGHT SENSOR
        sensorManager.registerListener(lightSensorListener, lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        //LOCATION
        startLocationUpdates();
        //MAPS
        map.onResume();
        if (lastLocation != null) {
            IMapController mapController = map.getController();
            GeoPoint startPoint = new GeoPoint(currentLong, currentLat);
            mapController.setZoom(18.0);
            mapController.setCenter(startPoint);
        }
    }
    //ROAD MANAGER
    private void drawRoute(GeoPoint start, GeoPoint finish){
        ArrayList<GeoPoint> routePoints = new ArrayList<>();
        routePoints.add(start);
        routePoints.add(finish);
        Road road = roadManager.getRoad(routePoints);
        Log.i("ROAD", "Route length: "+road.mLength+" klm");
        Log.i("ROAD", "Duration: "+road.mDuration/60+" min");
        if(map!=null){
            if(roadOverlay!=null){
                map.getOverlays().remove(roadOverlay);
            }
            roadOverlay = RoadManager.buildRoadOverlay(road);
            roadOverlay.getOutlinePaint().setColor(Color.RED);
            roadOverlay.getOutlinePaint().setStrokeWidth(10);
            map.getOverlays().add(roadOverlay);
        }
    }
}