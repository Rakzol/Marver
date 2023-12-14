package com.example.test4;

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

public class IniciarSesion extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IniciarSesionBinding iniciar_sesion = IniciarSesionBinding.inflate(getLayoutInflater());

        iniciar_sesion.btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciar_sesion.txtUsuario.setEnabled(false);
                iniciar_sesion.txtContrasena.setEnabled(false);
                iniciar_sesion.btnIniciarSesion.setVisibility(View.GONE);
                iniciar_sesion.pgrIniciando.setVisibility(View.VISIBLE);

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
                            aplicacion.controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if( json.getBoolean("usuario") && json.getBoolean("contraseña") ){
                                            Toast.makeText(v.getContext(), "Todo correcto :D", Toast.LENGTH_LONG).show();
                                            //Intent intent = new Intent(MainActivity.this, LoggedInActivity.class);
                                            //startActivity(intent);
                                            //finish();
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
