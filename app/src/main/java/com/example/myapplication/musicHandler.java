package com.example.myapplication;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import java.io.File;

//TODO Handle music
public class musicHandler extends Handler {
    private final MainActivity mainActivity;
    private MediaPlayer mediaPlayer;

    private boolean permissionGranted = false;



    public musicHandler(Looper looper, MainActivity mainActivity){
        super(looper);
        this.mainActivity = mainActivity;
        mediaPlayer = new MediaPlayer();

    }

    @Override
    public void handleMessage(@NonNull Message msg){
        switch (msg.what){
            case 1:
                musicPlay();
                break;
            case 2:
                musicStop();
            case 3:
                musicSwitch();

            default:
                super.handleMessage(msg);

        }
    }

    private void musicPlay(){

        String filePath = Environment.getExternalStorageDirectory().getPath()+"/Music/night.mp3";
        Log.d("musicHandler","PATH : " + filePath);
        File file  = new File(filePath);
        Log.d("musicHandler" , "file exists ? " + file.exists() + "can read? " + file.canRead());
        try{
                permissionGranted = true;
                mediaPlayer = MediaPlayer.create(
                        mainActivity,
                        Uri.parse(filePath)
                );

                Log.d("musicHandler" , "file exists ? " + file.exists() + "can read? " + file.canRead());
                mediaPlayer.start();
                Log.d("musicHandler" , "PLaying now");


            }catch (Exception e){
                e.printStackTrace();
            }


    };
    private void musicStop(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            Log.d("musicHandler" , "stopped");
        }
    };
    private void musicSwitch(){};

}
