package com.example.test4;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter{

    View myView;
    public CustomInfoWindow(Context context){
        myView = LayoutInflater.from(context).inflate(R.layout.custom_info_window,null);
    }
    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        return null;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {

        // Obtén referencias a las vistas dentro del diseño personalizado
        TextView titleTextView = myView.findViewById(R.id.textTituloInfoWindow);
        TextView snippetTextView = myView.findViewById(R.id.textDescripcionInfoWindow);

        // Establece el texto del título y el fragmento
        titleTextView.setText(marker.getTitle());
        snippetTextView.setText(marker.getSnippet());

        return myView;
    }
}
