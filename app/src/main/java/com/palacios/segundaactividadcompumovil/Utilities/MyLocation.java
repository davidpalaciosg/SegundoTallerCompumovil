package com.palacios.segundaactividadcompumovil.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MyLocation {
    private Date date;
    private double latitude;
    private double longitude;

    public MyLocation() {
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public JSONObject toJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("latitude", getLatitude());
            obj.put("longitude", getLongitude());
            obj.put("date", getDate());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
