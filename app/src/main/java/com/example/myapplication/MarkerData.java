package com.example.myapplication;

import com.google.android.gms.maps.model.LatLng;

public class MarkerData {
    String markerID;
    LatLng position;
    String base64ImageData;

    long timeStamp;

    public MarkerData(String markerID, LatLng position, String base64ImageData, long timeStamp) {
        this.markerID = markerID;
        this.position = position;
        this.base64ImageData = base64ImageData;
        this.timeStamp =  timeStamp;
    }
}
