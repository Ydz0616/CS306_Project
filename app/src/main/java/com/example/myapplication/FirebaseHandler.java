package com.example.myapplication;


import android.graphics.Bitmap;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FirebaseHandler extends Handler {
    private final DatabaseReference databaseReference;
    private final MainActivity mainactivity;
    private List<MarkerData> markerDataList;

    public FirebaseHandler(Looper looper, DatabaseReference databaseReference, MainActivity mainActivity){
        super(looper);
        this.databaseReference = databaseReference;
        this.mainactivity = mainActivity;
        markerDataList = new ArrayList<>();

        downloadData();
    }

    @Override
    public void handleMessage(@NonNull Message msg){
        switch (msg.what) {
            case 1:
                if (!markerDataList.isEmpty()) {
                    renderMarkers();
                } else {
                    // If markerDataList is empty, wait for data to be downloaded
                    downloadData();
                }

                break;

            case 2:
                try {
                    uploadDataToFirebase((Location) msg.obj);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            default:
                super.handleMessage(msg);
        }
    }
    private void downloadData(){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                markerDataList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String markerID = dataSnapshot.getKey();
                    String latitudeStr = dataSnapshot.child("latitude").getValue(String.class);
                    String longitudeStr = dataSnapshot.child("longitude").getValue(String.class);
                    String base64ImageData = dataSnapshot.child("imageData").getValue(String.class);
                    long timestamp = dataSnapshot.child("timestamp").getValue(long.class);

                    if (latitudeStr != null && longitudeStr != null) {
                        try {
                            double latitude = Double.parseDouble(latitudeStr);
                            double longitude = Double.parseDouble(longitudeStr);
                            LatLng position = new LatLng(latitude, longitude);

                            MarkerData markerData = new MarkerData(markerID, position, base64ImageData,timestamp);
                            markerDataList.add(markerData);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (mainactivity.isMapReady() && !markerDataList.isEmpty()) {
                    // If map is ready and markerDataList is not empty, render markers immediately
                    renderMarkers();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void renderMarkers(){
        //         send message to the MainActivity to render the markers
        Message message = Message.obtain();
        message.what = 1;
        message.obj = markerDataList;
        mainactivity.mapHandler.sendMessage(message);

    }
    private void uploadDataToFirebase(@Nullable Location location)  throws IOException {
        try {

            double latitude = location != null ? location.getLatitude() : 0.0;
            double longitude = location != null ? location.getLongitude() : 0.0;

            // Convert latitude and longitude to String
            String lat = String.valueOf(latitude);
            String lng = String.valueOf(longitude);

            // Convert image to byte array
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mainactivity.getContentResolver(), mainactivity.imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();
            String base64ImageData = Base64.encodeToString(imageData, Base64.DEFAULT);
            long timestamp = System.currentTimeMillis();

            // Upload data to Firebase

            String key = databaseReference.push().getKey();
            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("latitude", lat);
            uploadData.put("longitude", lng);
            uploadData.put("imageData", base64ImageData);
            uploadData.put("timestamp",timestamp);
            databaseReference.child(key).setValue(uploadData);

        }catch (Exception e) {
            e.printStackTrace();
            // Handle the exception here
        }
    }

}


