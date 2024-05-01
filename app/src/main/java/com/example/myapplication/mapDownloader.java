package com.example.myapplication;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.ArrayList;
import java.util.List;

class MapSetUpHandler extends Handler{

    private final FragmentManager fragmentManager;
    private final MainActivity mainactivity;

    private List<MarkerData> markerDataList;


    public MapSetUpHandler(Looper looper, FragmentManager fragmentManager, MainActivity mainActivity){
        super(looper);
        this.fragmentManager = fragmentManager;
        this.mainactivity = mainActivity;


    }
    @Override
    public void handleMessage(@NonNull Message msg){
        switch (msg.what){
            case 1:
                setupMap();
                break;

            default:
                super.handleMessage(msg);

        }
    }
    private void setupMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) fragmentManager.findFragmentById(R.id.mapFragment);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mapFragment.getMapAsync(mainactivity);
            }
        });
    }

}

