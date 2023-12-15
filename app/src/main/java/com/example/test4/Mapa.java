package com.example.test4;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test4.databinding.MapaBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class Mapa extends AppCompatActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent_servicioGPS = new Intent(this, ServicioGPS.class);
        startService(intent_servicioGPS);

        MapaBinding mapa = MapaBinding.inflate(getLayoutInflater());
        setContentView(mapa.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.0f));

        new Runnable(){
            @Override
            public void run() {
                googleMap.clear();
                LatLng sydney = new LatLng(-33.852, 151.211);
                googleMap.addMarker(new MarkerOptions()
                        .position(sydney)
                        .title("Marker in Sydney"));
                new Handler().postDelayed(this, 1000);
            }
        }.run();
    }
}