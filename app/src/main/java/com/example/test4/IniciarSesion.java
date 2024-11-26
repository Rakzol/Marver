package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

    Boolean spinnerInicializacion = true;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IniciarSesionBinding iniciar_sesion = IniciarSesionBinding.inflate(getLayoutInflater());

        SharedPreferences preferencias_compartidas_credenciales = getSharedPreferences("credenciales", MODE_PRIVATE);

        Spinner spinner = iniciar_sesion.spinnerSucursal;

        // Lista de opciones
        String[] opciones = {"Mochis", "Guasave"};

        // Crear el adaptador
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                opciones
        );

        // Estilo del dropdown
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        int index = -1;
        for (int i = 0; i < opciones.length; i++) {
            if (opciones[i].equals(preferencias_compartidas_credenciales.getString("sucursal", "Mochis"))) {
                index = i;
                break;
            }
        }

        // Si se encuentra, seleccionar esa opción
        if (index != -1) {
            spinner.setSelection(index);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(spinnerInicializacion){
                    spinnerInicializacion = false;
                    return;
                }
                SharedPreferences.Editor editor_preferencias_compartidas_credenciales = preferencias_compartidas_credenciales.edit();
                editor_preferencias_compartidas_credenciales.putString("sucursal", parent.getItemAtPosition(position).toString());
                editor_preferencias_compartidas_credenciales.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if( preferencias_compartidas_credenciales.getInt("clave", 0) != 0 ){
            Intent intent = new Intent(IniciarSesion.this, Manejador.class);
            startActivity(intent);
            finish();
        }

        iniciar_sesion.buttonIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciar_sesion.textUsuarioIniciarSesion.setEnabled(false);
                iniciar_sesion.textContrasenaIniciarSesion.setEnabled(false);
                iniciar_sesion.spinnerSucursal.setEnabled(false);
                iniciar_sesion.buttonIniciarSesion.setVisibility(View.GONE);
                iniciar_sesion.progressIniciarSesion.setVisibility(View.VISIBLE);

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://www.marverrefacciones.mx/android/iniciar_sesion");
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setDoOutput(true);

                            OutputStream output_sream = conexion.getOutputStream();
                            output_sream.write(("clave="+iniciar_sesion.textUsuarioIniciarSesion.getText().toString()+"&contraseña="+iniciar_sesion.textContrasenaIniciarSesion.getText().toString()).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json = new JSONObject( constructor_cadena.toString() );
                            ((Aplicacion)getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if( json.getBoolean("usuario") && json.getBoolean("contraseña") ){
                                            SharedPreferences.Editor editor_preferencias_compartidas_credenciales = preferencias_compartidas_credenciales.edit();

                                            editor_preferencias_compartidas_credenciales.putString("usuario", json.getString("nombre") );
                                            editor_preferencias_compartidas_credenciales.putString("contraseña", iniciar_sesion.textContrasenaIniciarSesion.getText().toString());
                                            editor_preferencias_compartidas_credenciales.putInt("clave", Integer.parseInt(iniciar_sesion.textUsuarioIniciarSesion.getText().toString()) );
                                            editor_preferencias_compartidas_credenciales.putInt("intentos", 11);
                                            editor_preferencias_compartidas_credenciales.apply();

                                            Intent intent = new Intent(IniciarSesion.this, Manejador.class);
                                            startActivity(intent);
                                            finish();
                                        }else {
                                            if (!json.getBoolean("usuario")) {
                                                iniciar_sesion.textUsuarioIniciarSesion.setError("Usuario Inexistente");
                                            }
                                            if (!json.getBoolean("contraseña")) {
                                                iniciar_sesion.textContrasenaIniciarSesion.setError("Contraseña Incorrecta");
                                            }
                                            iniciar_sesion.textUsuarioIniciarSesion.setEnabled(true);
                                            iniciar_sesion.textContrasenaIniciarSesion.setEnabled(true);
                                            iniciar_sesion.spinnerSucursal.setEnabled(true);
                                            iniciar_sesion.buttonIniciarSesion.setVisibility(View.VISIBLE);
                                            iniciar_sesion.progressIniciarSesion.setVisibility(View.GONE);
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();

                                        iniciar_sesion.textUsuarioIniciarSesion.setError("Usuario Inexistente");
                                        iniciar_sesion.textUsuarioIniciarSesion.setEnabled(true);
                                        iniciar_sesion.textContrasenaIniciarSesion.setEnabled(true);
                                        iniciar_sesion.spinnerSucursal.setEnabled(true);
                                        iniciar_sesion.buttonIniciarSesion.setVisibility(View.VISIBLE);
                                        iniciar_sesion.progressIniciarSesion.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();

                            ((Aplicacion)getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                @Override
                                public void run() {
                                    iniciar_sesion.textUsuarioIniciarSesion.setError("Usuario Inexistente");
                                    iniciar_sesion.textUsuarioIniciarSesion.setEnabled(true);
                                    iniciar_sesion.textContrasenaIniciarSesion.setEnabled(true);
                                    iniciar_sesion.spinnerSucursal.setEnabled(true);
                                    iniciar_sesion.buttonIniciarSesion.setVisibility(View.VISIBLE);
                                    iniciar_sesion.progressIniciarSesion.setVisibility(View.GONE);
                                }
                            });

                        }
                    }
                });

            }
        });

        setContentView(iniciar_sesion.getRoot());
    }
}
