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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
    private long startTime;
    private List<MarkerData> markerDataList;

    private Polyline gpsTrack;

//    private LocationCallback locationCallback = new LocationCallback() {
//        @Override
////        public void onLocationResult(LocationResult locationResult) {
////            if (locationResult != null) {
////                Location location = locationResult.getLastLocation();
////                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
////                //                    show lat lng
////                Log.d("MAPHANDLER", "LAT:  " + latLng.latitude + "LNG: " + latLng.longitude);
////                recordGPS(latLng);
////            } else {
////                Log.d("MAP", "NO LOCATION");
////            }
////        }
////    };
    private float totalDistance;
    private final LocationCallback locationCallback;

    private FusedLocationProviderClient fusedLocationClient;

    private Context context;
    private List<LatLng> points;
    private List<MarkerData> storedMarkerData;
    private PolylineOptions polylineOptions;

    public MapHandler(Looper looper, GoogleMap googleMapm, MainActivity mainActivity, Context context) {
        super(looper);
        storedMarkerData = new ArrayList<>();
        this.mainActivity = mainActivity;
        this.context = context;
        this.polylineOptions = new PolylineOptions().color(Color.BLUE).width(20);
        this.points = new ArrayList<>();
        this.markerDataList = new ArrayList<>();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if(locationResult!=null){
                    Location location = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    recordGPS(latLng);
                }else{
                    Log.d("MAP","NO LOCATION");
                }
            }
        };

    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 1:
                updateStoredMarkerData((List<MarkerData>) msg.obj);
                renderMarkers((List<MarkerData>) msg.obj);
                break;
            case 2:
                startRendering();
                break;
            case 3:
                stopRendering();
                break;
//                renderMarkers((List<MarkerData>) msg.obj);
            default:
                super.handleMessage(msg);
        }
    }

    private float calculateDistance(LatLng latLng1, LatLng latLng2) {
        float[] results = new float[1];
        Location.distanceBetween(latLng1.latitude, latLng1.longitude,
                latLng2.latitude, latLng2.longitude, results);
        return results[0];
    }
    private void updateStoredMarkerData(List<MarkerData> markerDataList) {
        // Update the stored marker data every time renderMarkers() is called
        storedMarkerData.clear(); // Clear existing data
        storedMarkerData.addAll(markerDataList);
    }
    public float getTotalDistance() {
        return totalDistance;
    }
    private void startRendering(){
        startTime = System.currentTimeMillis();
        startLocationUpdates();
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
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions

                    return;
                }
                mainActivity.mMap.setMyLocationEnabled(true);


            }
        });
    }

    private void recordGPS(LatLng latLng) {

        if (!points.isEmpty()) {
            LatLng previousLatLng = points.get(points.size() - 1);
            float distance = calculateDistance(previousLatLng, latLng);
            totalDistance += distance;
        }
        points.add(latLng);
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
        long stopTime = System.currentTimeMillis();
        long timeIntervalMillis = stopTime - startTime;
        long timeIntervalMinutes = timeIntervalMillis / (60 * 1000);
        float speed = 0.0f;
        if (!points.isEmpty() && timeIntervalMinutes > 0) {
            float distance = getTotalDistance();
            speed = distance / timeIntervalMinutes; // Speed in meters per minute
        }
        LayoutInflater inflater = mainActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout,
               null);
        TextView textTime = layout.findViewById(R.id.text_time);
        textTime.setText("Time: " + timeIntervalMinutes + " minutes"); // Replace formattedTime with your time value

        TextView textSpeed = layout.findViewById(R.id.text_speed);
        textSpeed.setText("Speed: " + speed + " m/min"); // Replace formattedSpeed with your speed value

        TextView textDistance = layout.findViewById(R.id.text_distance);
        textDistance.setText("Distance: " + totalDistance + " meters"); // Replace formattedDistance with your distance value
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);
        totalDistance = getTotalDistance();
        // Clear the map
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.mMap.clear();

            }
        });
        renderMarkers(storedMarkerData);
    }

}
