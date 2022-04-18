package com.palacios.segundaactividadcompumovil.Utilities;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.Writer;
import java.util.Date;

public class JsonCreator {
    private JSONArray locations;

    public JsonCreator() {
        this.locations = new JSONArray();
    }
/*
    public void writeJsonObject(Location lastLocation, View context){
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
            Log.i(“LOCATION", "Ubicacion de archivo: "+file);
            output = new BufferedWriter(new FileWriter(file));
            output.write(localizaciones.toString());
            output.close();
            Toast.makeText(getApplicationContext(), "Location saved",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
//Log error
        }

    }

 */
}
