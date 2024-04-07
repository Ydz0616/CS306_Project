package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;
    private final Context mContext;
    private Marker mMarker;

    public CustomInfoWindowAdapter(Context context) {
        mContext = context;
        mWindow = LayoutInflater.from(context).inflate(R.layout.customize_info_window, null);
    }

    private void renderWindowText(Marker marker, View view) {
        mMarker = marker;
        TextView txtLat = view.findViewById(R.id.txtLat);
        TextView txtLng = view.findViewById(R.id.txtLng);
        EditText editTitle = view.findViewById(R.id.editTitle);

        LatLng position = marker.getPosition();
        double latitude = position.latitude;
        double longitude = position.longitude;

        txtLat.setText("Latitude: " + latitude);
        txtLng.setText("Longitude: " + longitude);
        editTitle.setText(marker.getTitle());

        // Set listener for editing marker title
        editTitle.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && mMarker != null && mMarker.equals(marker)) {
                String newTitle = textView.getText().toString();
                marker.setTitle(newTitle);
                textView.clearFocus();
                // Hide keyboard after editing
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    @Override
    public View getInfoWindow(Marker marker) {
        renderWindowText(marker, mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
