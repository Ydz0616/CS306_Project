package com.example.myapplication;

import com.google.android.gms.maps.model.LatLng;

public class MarkerData {
    String markerID;
    LatLng position;
    String base64ImageData;

    public MarkerData(String markerID, LatLng position, String base64ImageData) {
        this.markerID = markerID;
        this.position = position;
        this.base64ImageData = base64ImageData;
    }
}
