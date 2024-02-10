package com.example.test4;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.test4.databinding.InicioBinding;

import java.util.ArrayList;

public class Inicio extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InicioBinding inicio = InicioBinding.inflate(getLayoutInflater());

        inicio.btnDarPermisos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pedirPermisos();
                if( tienePermisos() ){
                    iniciarSesion();
                }
            }
        });

        if( tienePermisos() ){
            iniciarSesion();
        }else{
            pedirPermisos();
        }

        setContentView(inicio.getRoot());
    }

    private void iniciarSesion(){
        Intent intent = new Intent(this, IniciarSesion.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if( tienePermisos() ){
            iniciarSesion();
        }
    }

    private Boolean tienePermisos(){

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            //System.out.println(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) + " ========== " + locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) );
            if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ){
                return false;
            }
        }else{
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(     ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                    ( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ) ||
                    ( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU )
                    ){
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED ){
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(     ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            ){
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if(     ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) != PackageManager.PERMISSION_GRANTED
            ){
                return false;
            }

        }
        return true;
    }

    private void pedirPermisos(){

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ){
                startActivity( new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) );
            }
        }else{
            startActivity( new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) );
        }

        ArrayList<String> lista_permisos = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.INTERNET) ){
                    alertar_permiso(android.Manifest.permission.INTERNET);
                }else{
                    lista_permisos.add(android.Manifest.permission.INTERNET);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION) ){
                    alertar_permiso(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                }else{
                    lista_permisos.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) ){
                    alertar_permiso(android.Manifest.permission.ACCESS_FINE_LOCATION);
                }else{
                    lista_permisos.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.RECEIVE_BOOT_COMPLETED) ){
                    alertar_permiso(android.Manifest.permission.RECEIVE_BOOT_COMPLETED);
                }else{
                    lista_permisos.add(android.Manifest.permission.RECEIVE_BOOT_COMPLETED);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) ){
                    alertar_permiso(android.Manifest.permission.CAMERA);
                }else{
                    lista_permisos.add(android.Manifest.permission.CAMERA);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_NETWORK_STATE) ){
                    alertar_permiso(android.Manifest.permission.ACCESS_NETWORK_STATE);
                }else{
                    lista_permisos.add(android.Manifest.permission.ACCESS_NETWORK_STATE);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) ){
                    alertar_permiso(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                }else{
                    lista_permisos.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ){
                    alertar_permiso(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }else{
                    lista_permisos.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.FOREGROUND_SERVICE) ){
                    alertar_permiso(android.Manifest.permission.FOREGROUND_SERVICE);
                }else{
                    lista_permisos.add(android.Manifest.permission.FOREGROUND_SERVICE);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) ){
                    alertar_permiso(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }else{
                    lista_permisos.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) ){
                    alertar_permiso(android.Manifest.permission.POST_NOTIFICATIONS);
                }else{
                    lista_permisos.add(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.READ_MEDIA_IMAGES) ){
                    alertar_permiso(android.Manifest.permission.READ_MEDIA_IMAGES);
                }else{
                    lista_permisos.add(android.Manifest.permission.READ_MEDIA_IMAGES);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) ){
                    alertar_permiso(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION);
                }else{
                    lista_permisos.add(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) != PackageManager.PERMISSION_GRANTED ){
                if( shouldShowRequestPermissionRationale(android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) ){
                    alertar_permiso(android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC);
                }else{
                    lista_permisos.add(android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC);
                }
            }
        }

        if( lista_permisos.size() > 0 ){
            String[] arreglo_permisos = new String[lista_permisos.size()];
            lista_permisos.toArray(arreglo_permisos);

            ActivityCompat.requestPermissions(this,arreglo_permisos, 69);
        }
    }

    private void alertar_permiso(String permiso){
        new AlertDialog.Builder(this)
                .setTitle("Permiso necesario")
                .setMessage("Se necesita el permiso para el funcionamiento de la aplicación")
                .setPositiveButton("Pedir permiso", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions( Inicio.this, new String[]{permiso}, 70);
                    }
                }).create().show();
    }

}
