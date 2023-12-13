package com.example.test4;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.test4.databinding.CapaBinding;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public class Principal extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences.Editor editor = getSharedPreferences("MiAppPref", MODE_PRIVATE).edit();
        editor.putString("usuario", "nombreUsuario");
        editor.putString("token", "tokenDeSesion");
        editor.apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET}, 69);
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 70);
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 71);
            }
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECEIVE_BOOT_COMPLETED}, 76);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.FOREGROUND_SERVICE}, 72);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 73);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if( ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 74);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if( ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.FOREGROUND_SERVICE_LOCATION}, 75);
            }
        }

        Intent serviceIntent = new Intent(this, ServicioGPS.class);
        startService(serviceIntent);

        CapaBinding capa = CapaBinding.inflate(getLayoutInflater());

        capa.btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capa.txtUsuario.setEnabled(false);
                capa.txtContrasena.setEnabled(false);
                capa.btnIniciarSesion.setVisibility(View.GONE);
                capa.pgrIniciando.setVisibility(View.VISIBLE);

                Aplicacion aplicacion = (Aplicacion)getApplication();

                aplicacion.servicio_ejecucion.execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://www.marverrefacciones.mx/android/iniciar_sesion");
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setDoOutput(true);

                            OutputStream output_sream = conexion.getOutputStream();
                            output_sream.write(("usuario="+capa.txtUsuario.getText().toString()+"&contraseña="+capa.txtContrasena.getText().toString()).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json = new JSONObject( constructor_cadena.toString() );
                            aplicacion.controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if( json.getBoolean("usuario") && json.getBoolean("contraseña") ){
                                            Toast.makeText(v.getContext(), "Todo correcto :D", Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(MainActivity.this, LoggedInActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }else {
                                            if (!json.getBoolean("usuario")) {
                                                capa.txtUsuario.setError("Usuario Inexistente");
                                            }
                                            if (!json.getBoolean("contraseña")) {
                                                capa.txtContrasena.setError("Contraseña Incorrecta");
                                            }
                                            capa.txtUsuario.setEnabled(true);
                                            capa.txtContrasena.setEnabled(true);
                                            capa.btnIniciarSesion.setVisibility(View.VISIBLE);
                                            capa.pgrIniciando.setVisibility(View.GONE);
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });

            }
        });

        setContentView(capa.getRoot());

    }
}
