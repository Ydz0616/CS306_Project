package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MapHandler extends Handler {

    private final MainActivity mainActivity;

    private List<MarkerData> markerDataList;

    public MapHandler(Looper looper, GoogleMap googleMapm, MainActivity mainActivity){
        super(looper);
        this.mainActivity = mainActivity;
        markerDataList = new ArrayList<>();
    }

    @Override
    public void handleMessage(@NonNull Message msg){
        switch (msg.what){
            case 1:
                renderMarkers((List<MarkerData>) msg.obj);
                break;

            default:
                super.handleMessage(msg);
        }
    }
    private void renderMarkers(List<MarkerData> markerDataList){
        // Render the markers on the map here
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (MarkerData markerData : markerDataList) {
                    LatLng position = markerData.position;
                    String base64ImageData = markerData.base64ImageData;

                    // Decode base64 string to bitmap
                    Bitmap oribitmap = decodeBase64ToBitmap(base64ImageData);
                    Bitmap bitmap = cropToSquare(oribitmap);

                    int targetWidth = 200; // Set your desired width
                    int targetHeight = 200; // Set your desired height
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
                    // Create MarkerOptions for the image
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(position).icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)).draggable(true);

                    // Add marker to the map
                    GoogleMap googleMap = mainActivity.mMap;
                    Marker marker = googleMap.addMarker(markerOptions);
                    assert marker != null;
                    marker.setTag(markerData.markerID);
                    mainActivity.markerList.add(marker);
                }
            }
        });
    }
//    TODO: render route
    private void renderRoute(){

    }

    private Bitmap decodeBase64ToBitmap(String base64ImageData) {
        byte[] decodedBytes = Base64.decode(base64ImageData, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        // Calculate the coordinates for cropping
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        // Crop the bitmap to a square
        return Bitmap.createBitmap(bitmap, x, y, size, size);
    }

}
