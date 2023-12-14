package com.example.test4;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
            }
        });

        if( tienePermisos() ){
            iniciarSesion();
        }else{
            inicio.pgrPedirPermisos.setVisibility(View.GONE);
            inicio.btnDarPermisos.setVisibility(View.VISIBLE);
            inicio.txtInfo.setVisibility(View.VISIBLE);
            pedirPermisos();
        }

        setContentView(inicio.getRoot());

        //SharedPreferences.Editor editor = getSharedPreferences("MiAppPref", MODE_PRIVATE).edit();
        //editor.putString("usuario", "nombreUsuario");
        //editor.putString("token", "tokenDeSesion");
        //editor.apply();

        //System.out.println("ya se pidieron");

        //Intent serviceIntent = new Intent(this, ServicioGPS.class);
        //startService(serviceIntent);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(     ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED
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
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ){
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if( ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                return false;
            }
        }
        return true;
    }

    private void pedirPermisos(){
        ArrayList<String> lista_permisos = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ){
                lista_permisos.add(android.Manifest.permission.INTERNET);
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                System.out.println(shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION));
                if( !shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION) ){
                    new AlertDialog.Builder(this)
                            .setTitle("Permiso necesario")
                            .setMessage("Se necesita el permiso para el funcionamiento de la aplicación")
                            .setPositiveButton("Conceder", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions( Inicio.this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 70);
                                }
                            }).create().show();
                }else{
                    lista_permisos.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                lista_permisos.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED ){
                lista_permisos.add(android.Manifest.permission.RECEIVE_BOOT_COMPLETED);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED ){
                lista_permisos.add(android.Manifest.permission.FOREGROUND_SERVICE);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                lista_permisos.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ){
                lista_permisos.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if( ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                lista_permisos.add(android.Manifest.permission.INTERNET);
            }
        }

        if( lista_permisos.size() > 0 ){

            String[] arreglo_permisos = new String[lista_permisos.size()];
            lista_permisos.toArray(arreglo_permisos);
            System.out.println(lista_permisos.toString());

            ActivityCompat.requestPermissions(this,arreglo_permisos, 69);
        }
    }

}
