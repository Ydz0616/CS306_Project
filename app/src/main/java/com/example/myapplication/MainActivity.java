package com.example.myapplication;
import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
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

import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.database.annotations.Nullable;
public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback{

    private ActivityResultLauncher<String[]> permissionLauncher;
    private static final int STORAGE_PERMISSION_CODE = 23;
    private LocationListener myLocationListener;
    GoogleMap mMap;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    private static final int EDIT_REQUEST = 1;

    ActivityMainBinding mainBinding;
    ActivityResultLauncher<Uri> takePictureLauncher;
    Uri imageUri;

    List<Marker> markerList = new ArrayList<>();
//    google database setup

    DatabaseReference databaseReference;
    FirebaseDatabase firebaseDatabase;
    MapSetUpHandler mapSetUpHandler;
    MapHandler mapHandler;

    musicHandler musicHandler;

    FirebaseHandler firebaseHandler;

    private boolean mapReady = false;
    private boolean isRecording = false;
//   on create function
    @Override    protected void onCreate(Bundle savedInstanceState) {



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

        mainBinding.playButton.setOnClickListener(view->{
            changeRecordButton();
        });
//        looper and handler
        HandlerThread mapSetupThread = new HandlerThread("MapSetupThread");
        mapSetupThread.start();
        Looper looper = mapSetupThread.getLooper();
        mapSetUpHandler = new MapSetUpHandler(looper, getSupportFragmentManager(), this);
        mapSetUpHandler.sendEmptyMessage(1);

        HandlerThread mapUpdateThread = new HandlerThread("MapUpdateThread");
        mapUpdateThread.start();
        looper = mapUpdateThread.getLooper();
        mapHandler = new MapHandler(looper, mMap, this, this);

        HandlerThread firebaseThread = new HandlerThread("FirebaseThread");
        firebaseThread.start();
        looper = firebaseThread.getLooper();
        DatabaseReference databaseReference = firebaseDatabase.getReference("images");
        firebaseHandler = new FirebaseHandler(looper, databaseReference, this);

        HandlerThread musicThread = new HandlerThread("MusicThread");
        musicThread.start();
        looper = musicThread.getLooper();
        musicHandler = new musicHandler(looper, this);

        if(!checkStoragePermissions()){
            requestForStoragePermissions();
        }
//        boolean locationPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
////                              check permission
//        if (!locationPermission) {
//            // Request location permission
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//        }
//        boolean cameraPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
//        if (!cameraPermission) {
//            // Request camera permission
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
//        }
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> isGranted) {
                        if (isGranted.containsValue(false)) {
                            // Handle permission denied
                        } else {
                            // Handle permission granted
                            Log.d(TAG, "onActivityResult: Permissions granted");
                        }
                    }
                });

        checkAndRequestPermissions();



    }
    private void checkAndRequestPermissions() {
        boolean storagePermission = checkStoragePermissions();
        boolean locationPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean cameraPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (!storagePermission || !locationPermission || !cameraPermission) {

            permissionLauncher.launch(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
            });
        }
    }
    public boolean checkStoragePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11 (R) or above
            return Environment.isExternalStorageManager();
        }else {
            //Below android 11
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
        }
    }

    private ActivityResultLauncher<Intent> storageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>(){

                        @Override
                        public void onActivityResult(ActivityResult o) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                                //Android is 11 (R) or above
                                if(Environment.isExternalStorageManager()){
                                    //Manage External Storage Permissions Granted
                                    Log.d(TAG, "onActivityResult: Manage External Storage Permissions Granted");
                                }else{
                                    Toast.makeText(MainActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                //Below android 11
                                Toast.makeText(MainActivity.this,"Sorry, please update your android version",Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
    private void requestForStoragePermissions() {
        //Android is 11 (R) or above
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }catch (Exception e){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }else{
            //Below android 11
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }

    }
    private void changeRecordButton(){
        if(!isRecording){
            recordStart();
            mainBinding.playButton.setImageResource(R.drawable.ic_stop);
        }else {
            recordEnd();
            mainBinding.playButton.setImageResource(R.drawable.ic_start);
        }
        isRecording = !isRecording;
    }

    private void recordStart(){
        Message msg = Message.obtain();
        msg.what = 1;
        musicHandler.sendMessage(msg);
        Message msg2 = Message.obtain();
        msg2.what = 2;
        mapHandler.sendMessage(msg2);

    }


    private void recordEnd(){
        Message msg = Message.obtain();
        msg.what = 2;
        musicHandler.sendMessage(msg);
        Message msg2 = Message.obtain();
        msg2.what =3 ;
        mapHandler.sendMessage(msg2);

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

    mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(@NonNull Marker marker) {
            showEditTitleDialog(marker);
        }
    });
//   TODO: I don't think the ondrag is a good idea, so the ondrag listener code is delelted, might consider to redo it in future
        mapReady = true;
        firebaseHandler.sendEmptyMessage(1);

    }


    public boolean isMapReady() {
        return mapReady;
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
                                    try{
//                                        uploadDataToFirebase(location);}catch (IOException e){throw new RuntimeException(e);
                                        Message msg = Message.obtain();
                                        msg.what = 2;
                                        msg.obj = location;
                                        firebaseHandler.sendMessage(msg);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
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
            Toast.makeText(MainActivity.this, "PLEASE RECLICK CAMERA", Toast.LENGTH_SHORT);
        }else{
            takePictureLauncher.launch(imageUri);
        }


    }

//    handle all permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.length > 0){
                boolean write = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if(read && write){
                    Toast.makeText(MainActivity.this, "Storage Permissions Granted", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


}
