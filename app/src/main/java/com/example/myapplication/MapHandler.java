package com.example.myapplication;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapHandler extends Handler {

    private final MainActivity mainActivity;

    private List<MarkerData> markerDataList;

    private Polyline gpsTrack;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {
                Location location = locationResult.getLastLocation();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //                    show lat lng
                Log.d("MAPHANDLER", "LAT:  " + latLng.latitude + "LNG: " + latLng.longitude);
                recordGPS(latLng);
            } else {
                Log.d("MAP", "NO LOCATION");
            }
        }
    };


    private FusedLocationProviderClient fusedLocationClient;

    private Context context;
    private List<LatLng> points;

    private PolylineOptions polylineOptions;

    public MapHandler(Looper looper, GoogleMap googleMapm, MainActivity mainActivity, Context context) {
        super(looper);
        this.mainActivity = mainActivity;
        this.context = context;
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.BLUE);
        polylineOptions.width(20);
        points = new ArrayList<>();
        markerDataList = new ArrayList<>();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 1:
                renderMarkers((List<MarkerData>) msg.obj);
                break;
            case 2:
                startLocationUpdates();
            case 3:
                stopRendering();
//                renderMarkers((List<MarkerData>) msg.obj);
            default:
                super.handleMessage(msg);
        }
    }

    private void renderMarkers(List<MarkerData> markerDataList) {
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

    private void startLocationUpdates() {
        Log.d("MAP", "TRIGGERED");

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdateDelayMillis(1000)
                .build();

        Log.d("MAP", "REQUST TRIGGERED");

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            Log.d("MAP", "NO PERMISSION");
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        } else {
            Log.d("MAP", "YES PERMISSION");
        }


        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());


    }

    private void renderRoute() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.mMap.clear();
                mainActivity.mMap.addPolyline(polylineOptions);
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mainActivity.mMap.setMyLocationEnabled(true);


            }
        });
    }

    private void recordGPS(LatLng latLng) {
        polylineOptions.add(latLng);
        Log.d("MAP", "RENDERING POINT LAT : " + latLng.latitude + " LNG : " + latLng.longitude);
        renderRoute();


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

    public void stopRendering() {
//        fusedLocationClient.removeLocationUpdates(locationCallback);
//        mainActivity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mainActivity.mMap.clear();
//                Message msg = Message.obtain();
//                msg.what = 1;
//                mainActivity.firebaseHandler.sendMessage(msg);
//
//
//            }
//        });

    }

}
