package com.example.myapplication;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.example.myapplication.databinding.ActivityMainBinding;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapCapabilities;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.database.annotations.Nullable;
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{
    private GoogleMap mMap;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final int EDIT_REQUEST = 1;

    ActivityMainBinding mainBinding;
    ActivityResultLauncher<Uri> takePictureLauncher;
    Uri imageUri;

    List<Marker> markerList = new ArrayList<>();
//    google database setup

    DatabaseReference databaseReference;
    FirebaseDatabase firebaseDatabase;

//   on create function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, new OnMapsSdkInitializedCallback() {
            @Override
            public void onMapsSdkInitialized(@NonNull MapsInitializer.Renderer renderer) {

                 Log.d("TAG", "onMapsSdkInitialized: ");             }         });

        EdgeToEdge.enable(this);
        //        setting up mainbinding for the image display and the image uri
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });



//        setContentView(mainBinding.getRoot());
        imageUri = createUri();
        registerPictureLauncher();
        mainBinding.btnTakePicture.setOnClickListener(view->{
            checkCameraPermissionAndOpenCamera();
        });


//        setting up map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);

        mapFragment.getMapAsync(this);


    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
    mMap = googleMap;
//    MapCapabilities capabilities = googleMap.getMapCapabilities();
//    System.out.println("is advanced marker enabled?" + capabilities.isAdvancedMarkersAvailable());
    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));
    LatLng kunshanLatLng = new LatLng(31.416, 120.9014);
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kunshanLatLng, 10));
    mMap.getUiSettings().setZoomControlsEnabled(true); // Enable zoom control


    // Retrieve data from Firebase Realtime Database
    DatabaseReference databaseReference = firebaseDatabase.getReference("images");
    databaseReference.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            // Clear existing markers
            for (Marker marker : markerList) {
                marker.remove();
            }
            markerList.clear();

            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                String markerID = dataSnapshot.getKey();
                Log.d("markerID",markerID);
                // Get latitude, longitude, and Base64 image data from the snapshot
                String latitudeStr = dataSnapshot.child("latitude").getValue(String.class);
                String longitudeStr = dataSnapshot.child("longitude").getValue(String.class);
                String base64ImageData = dataSnapshot.child("imageData").getValue(String.class);

                // Check if latitude and longitude strings are retrieved successfully
                if (latitudeStr != null && longitudeStr != null) {
                    try {
                        // Parse latitude and longitude strings to doubles
                        double latitude = Double.parseDouble(latitudeStr);
                        double longitude = Double.parseDouble(longitudeStr);
                        LatLng position = new LatLng(latitude,longitude);
                        // Print retrieved latitude and longitude
                        Log.d("Map", "Retrieved Latitude: " + latitude + ", Longitude: " + longitude);

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
                        Marker marker = mMap.addMarker(markerOptions);
                        assert marker != null;
                        marker.setTag(markerID);
                        markerList.add(marker);

                    } catch (NumberFormatException e) {
                        // Handle if latitude or longitude cannot be parsed as doubles
                        e.printStackTrace();
                    }
                } else {
                    // Data not retrieved successfully, prompt a message
                    Log.e("Map", "Error: Latitude or Longitude data not retrieved successfully");
                    Toast.makeText(MainActivity.this, "Error: Latitude or Longitude data not retrieved successfully", Toast.LENGTH_SHORT).show();
                }
            }
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(@NonNull Marker marker) {
                    showEditTitleDialog(marker);
                }
            });
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(@NonNull Marker marker) {
                    // Do nothing
                }

                @Override
                public void onMarkerDrag(@NonNull Marker marker) {
                    // Do nothing
                }

                @Override
                public void onMarkerDragEnd(@NonNull Marker marker) {
                    updateMarkerPosition(marker);

                }
            });
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            // Handle onCancelled event
        }
    });
}
    private Marker findMarkerByPosition(LatLng position) {
        for (Marker marker : markerList) {
            if (marker.getPosition().equals(position)) {
                return marker;
            }
        }
        return null;
    }


//showing edit title dialog once the opponent is dragged
    private void showEditTitleDialog(final Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Marker Title");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(marker.getTitle()); // Set current title as default text
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newTitle = input.getText().toString();
                Log.d("NewTitle", "New title: " + newTitle);
                if (!newTitle.isEmpty()) {
                    // Update the marker's title
                    Log.d("NewTitle", "New title: " + newTitle);
                    // Notify the user that the title has been updated
                    marker.setTitle(newTitle);
//                    Toast.makeText(MainActivity.this, newTitle, Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Title updated successfully", Toast.LENGTH_SHORT).show();

                } else {
                    // Notify the user that the title cannot be empty
                    Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Cancel the dialog
                dialog.dismiss();
            }
        });

        builder.show();
    }

    // Method to crop a bitmap to a square
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
//Method to decode base64 to image that can be displayed
    public Bitmap decodeBase64ToBitmap(String base64ImageData) {
        byte[] decodedBytes = Base64.decode(base64ImageData, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

//    method to create image uri
    private Uri createUri(){
        File imageFile = new File(getApplicationContext().getFilesDir(),"camera_photo.jpg");
        return FileProvider.getUriForFile(
                getApplicationContext(),
               "com.example.myapplication.fileProvider",
                imageFile

        );
    }
//method to launch picture
    private void registerPictureLauncher(){
        firebaseDatabase = FirebaseDatabase.getInstance("https://sharedalbum-4da54-default-rtdb.firebaseio.com/");
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        try{
                            if(result){
                                // Get location
                                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                boolean locationPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
//                              check permission
                                if (!locationPermission) {
                                    // Request location permission
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                                    return;
                                }
                                Location location  =locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (location!=null){
                                    try{uploadDataToFirebase(location);}catch (IOException e){throw new RuntimeException(e);};
                                }




                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

//    permission checker

    public void checkCameraPermissionAndOpenCamera(){
        boolean cameraPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (!cameraPermission) {
            // Request camera permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }else{
            takePictureLauncher.launch(imageUri);
        }


    }

//    handle all permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE){
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                takePictureLauncher.launch(imageUri);
            }else{
                Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

//handle marker location access
    private void updateMarkerPosition(Marker marker) {
        String markerID = (String) marker.getTag();
        // Find the marker's position in the list

        if (markerID!=null) {
            // Get the new position of the marker
            LatLng newPosition = marker.getPosition();
            String newlat = String.valueOf(newPosition.latitude);
            String newlong  = String.valueOf(newPosition.longitude);
            // Update the position in the Firebase database
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("images");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        String database_markerID = dataSnapshot.getKey();
                        if(markerID.equals(database_markerID)){
                            dataSnapshot.getRef().child("latitude").setValue(newlat);
                            dataSnapshot.getRef().child("longitude").setValue(newlong);
                            break;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle onCancelled event
                }
            });
        }
    }

//    handle write and read data access

    private void uploadDataToFirebase(@Nullable Location location)  throws IOException {
        try {

            double latitude = location != null ? location.getLatitude() : 0.0;
            double longitude = location != null ? location.getLongitude() : 0.0;

            // Convert latitude and longitude to String
            String lat = String.valueOf(latitude);
            String lng = String.valueOf(longitude);

            // Convert image to byte array
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();
            String base64ImageData = Base64.encodeToString(imageData, Base64.DEFAULT);
            long timestamp = System.currentTimeMillis();

            // Upload data to Firebase
            databaseReference = firebaseDatabase.getReference("images");
            String key = databaseReference.push().getKey();
            Map<String, Object> uploadData = new HashMap<>();
            uploadData.put("latitude", lat);
            uploadData.put("longitude", lng);
            uploadData.put("imageData", base64ImageData);
            uploadData.put("timestamp",timestamp);
            databaseReference.child(key).setValue(uploadData);

            Toast.makeText(MainActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
        }catch (Exception e) {
            e.printStackTrace();
            // Handle the exception here
            Toast.makeText(MainActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        }
    }

}
