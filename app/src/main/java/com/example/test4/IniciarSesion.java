package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test4.databinding.IniciarSesionBinding;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class IniciarSesion extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IniciarSesionBinding iniciar_sesion = IniciarSesionBinding.inflate(getLayoutInflater());

        SharedPreferences preferencias_compartidas_credenciales = getSharedPreferences("credenciales", MODE_PRIVATE);

        if( preferencias_compartidas_credenciales.getString("usuario", null) != null ){
            //Intent intent = new Intent(IniciarSesion.this, Mapa.class);
            Intent intent = new Intent(IniciarSesion.this, Manejador.class);
            startActivity(intent);
            finish();
        }

        iniciar_sesion.btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciar_sesion.txtUsuario.setEnabled(false);
                iniciar_sesion.txtContrasena.setEnabled(false);
                iniciar_sesion.btnIniciarSesion.setVisibility(View.GONE);
                iniciar_sesion.pgrIniciando.setVisibility(View.VISIBLE);

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://www.marverrefacciones.mx/android/iniciar_sesion");
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setDoOutput(true);

                            OutputStream output_sream = conexion.getOutputStream();
                            output_sream.write(("usuario="+iniciar_sesion.txtUsuario.getText().toString()+"&contraseña="+iniciar_sesion.txtContrasena.getText().toString()).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json = new JSONObject( constructor_cadena.toString() );
                            ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if( json.getBoolean("usuario") && json.getBoolean("contraseña") ){
                                            SharedPreferences.Editor editor_preferencias_compartidas_credenciales = preferencias_compartidas_credenciales.edit();

                                            editor_preferencias_compartidas_credenciales.putString("usuario", iniciar_sesion.txtUsuario.getText().toString());
                                            editor_preferencias_compartidas_credenciales.putString("contraseña", iniciar_sesion.txtContrasena.getText().toString());
                                            editor_preferencias_compartidas_credenciales.putInt("id", json.getInt("id"));
                                            editor_preferencias_compartidas_credenciales.putInt("intentos", 11);
                                            editor_preferencias_compartidas_credenciales.apply();

                                            Intent intent = new Intent(IniciarSesion.this, Manejador.class);
                                            startActivity(intent);
                                            finish();
                                        }else {
                                            if (!json.getBoolean("usuario")) {
                                                iniciar_sesion.txtUsuario.setError("Usuario Inexistente");
                                            }
                                            if (!json.getBoolean("contraseña")) {
                                                iniciar_sesion.txtContrasena.setError("Contraseña Incorrecta");
                                            }
                                            iniciar_sesion.txtUsuario.setEnabled(true);
                                            iniciar_sesion.txtContrasena.setEnabled(true);
                                            iniciar_sesion.btnIniciarSesion.setVisibility(View.VISIBLE);
                                            iniciar_sesion.pgrIniciando.setVisibility(View.GONE);
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

        setContentView(iniciar_sesion.getRoot());
    }
}
