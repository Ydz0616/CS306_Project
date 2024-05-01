package com.example.myapplication;


import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class FirebaseHandler extends Handler {
    private final DatabaseReference databaseReference;
    private final MainActivity mainactivity;
    private List<MarkerData> markerDataList;

    public FirebaseHandler(Looper looper, DatabaseReference databaseReference, MainActivity mainActivity){
        super(looper);
        this.databaseReference = databaseReference;
        this.mainactivity = mainActivity;
        markerDataList = new ArrayList<>();


    }

    @Override
    public void handleMessage(@NonNull Message msg){
        switch (msg.what) {
            case 1:
                downloadData();
                break;
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

                    if (latitudeStr != null && longitudeStr != null) {
                        try {
                            double latitude = Double.parseDouble(latitudeStr);
                            double longitude = Double.parseDouble(longitudeStr);
                            LatLng position = new LatLng(latitude, longitude);

                            MarkerData markerData = new MarkerData(markerID, position, base64ImageData);
                            markerDataList.add(markerData);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
//                send message to the MainActivity to render the markers
                Message message = Message.obtain();
                message.what = 1;
                message.obj = markerDataList;
                mainactivity.mapHandler.sendMessage(message);



            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}


