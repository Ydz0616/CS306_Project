# HIT N’ RUN USER MANUAL

**Version:** 1.0.0  
**Author:** Yuandong Zhang  
**Date:** May 2024

## Contents
1. [Introduction](#introduction)
2. [APP Overview](#app-overview)
3. [Permission Request](#permission-request)
   - [Storage Permission](#storage-permission)
   - [Camera and Location Permission](#camera-and-location-permission)
4. [Photo Taking and Sharing](#photo-taking-and-sharing)
   - [Photo Taking](#photo-taking)
   - [Photo Preview](#photo-preview)
   - [Photo Sharing and Display](#photo-sharing-and-display)
   - [Marker Customizing](#marker-customizing)
5. [Running Recording and Music Playing](#running-recording-and-music-playing)
   - [Start and Stop](#start-and-stop)
   - [Trajectory Zoom Levels](#trajectory-zoom-levels)
6. [Multi-thread Transmission](#multi-thread-transmission)

## 1. Introduction
The purpose of this manual is to document and describe the features of Hit N’ Run, an Android application designed for running enthusiasts. Hit N’ Run integrates features such as photo sharing, music live-streaming, and trajectory recording to optimize the running experience. Technical details, including multi-thread design, are also documented for future improvements.

## 2. APP Overview
Hit N’ Run streamlines pre-run preparation by integrating features such as music selection, trajectory recording, and running analysis into a single platform. It emphasizes sharing among runners by enabling real-time photo uploads and displaying them on a map. Hit N’ Run aims to be a companion for both casual runners and professionals, offering trajectory recording, motion analysis, and real-time status sharing, all while enjoying music.

## 3. Permission Request
Hit N’ Run requires the following permissions:
1. **External Storage:** Allows access to external storage for audio content.
2. **Location:** Enables location-based features for real-time rendering of trajectories and photo markers.
3. **Camera:** Grants access to the camera for real-time photo sharing.
4. **Internet:** Allows connectivity to online services for photo synchronization and trajectory updates.

### 3.1 Storage Permission
Upon app initialization, users will be prompted to grant storage permission using a toggle switch.

### 3.2 Camera and Location Permission
After granting storage permission, users will be prompted for camera and location permissions. Users can select either 'while using the app' or 'only this time' option. Once permissions are granted, users can utilize all features of Hit N’ Run.

## 4. Photo Taking and Sharing
To take and share a photo:
1. Click the grey, shutter-like button located on the bottom left of the screen.

### 4.1 Photo Taking
Once clicked, the camera is triggered, allowing users to adjust settings and take a photo.

### 4.2 Photo Preview
After taking a photo, users can preview it. If satisfied, they can upload and share it. Otherwise, they can cancel or retake the photo.

### 4.3 Photo Sharing and Display
Uploaded photos are instantly rendered on the map, displayed alongside other users' photos as markers. Users can zoom in/out to view clustered or de-clustered markers.

### 4.4 Marker Customizing
By clicking a photo marker, users can customize and save its title.

## 5. Running Recording and Music Playing
Running recording and music playing can be accessed by clicking the play button on the bottom right of the screen.

### 5.1 Start and Stop
Clicking the play/start button clears markers and renders the trajectory, while playing selected music. Clicking stop clears the route and re-renders markers, stopping the music.

### 5.2 Trajectory Zoom Levels
Users can adjust zoom levels to observe the running trajectory.

## 6. Multi-thread Transmission
Hit N’ Run utilizes a looper-handler approach:
1. **MainActivity:** Manages the app's UI and coordinates interactions between components.
2. **FirebaseHandler:** Handles communication with Firebase Realtime Database asynchronously.
3. **MapSetUpHandler:** Sets up map functionalities and notifies MainActivity when the map is ready.
4. **MapHandler:** Renders markers and route information on the map asynchronously.
5. **MusicHandler:** Manages music playback independently to ensure responsiveness.

Refer to the source code for more technical details and multi-threading information.
