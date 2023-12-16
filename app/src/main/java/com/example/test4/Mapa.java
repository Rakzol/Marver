package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test4.databinding.MapaBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Mapa extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private Map<Integer,Marker> marcadores = new HashMap();

    private MapaBinding mapa;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent_servicioGPS = new Intent(this, ServicioGPS.class);
        startService(intent_servicioGPS);

        mapa = MapaBinding.inflate(getLayoutInflater());

        mapa.chkNombres.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

        setContentView(mapa.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setMapStyle( MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style) );

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.25f));

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

                String salida = "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "");

                if( preferencias_compartidas.getString("usuario", null) == null ){
                    return;
                }

                try {
                    URL url = new URL("https://www.marverrefacciones.mx/android/posiciones");
                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                    conexion.setRequestMethod("POST");
                    conexion.setDoOutput(true);

                    OutputStream output_sream = conexion.getOutputStream();
                    output_sream.write(salida.getBytes());
                    output_sream.flush();
                    output_sream.close();

                    BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                    String linea;
                    StringBuilder constructor_cadena = new StringBuilder();
                    while ((linea = bufer_lectura.readLine()) != null) {
                        constructor_cadena.append(linea).append("\n");
                    }

                    JSONArray json_array = new JSONArray( constructor_cadena.toString() );

                    for( int c = 0; c < json_array.length(); c++ ){
                        JSONObject json_object = json_array.getJSONObject(c);
                        if( !marcadores.containsKey(json_object.getInt("usuario")) ){

                            ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Marker marcador = gMap.addMarker( new MarkerOptions()
                                                .position( new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")) )
                                                .title(json_object.getString("Nombre")));

                                        marcadores.put(json_object.getInt("usuario"), marcador);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }else{
                            ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {

                                    try {
                                        LatLng latlng_actual = marcadores.get(json_object.getInt("usuario")).getPosition();
                                        LatLng latlng_nueva = new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud"));
                                        if( latlng_actual.latitude != latlng_nueva.latitude || latlng_actual.longitude != latlng_nueva.longitude ){
                                            marcadores.get(json_object.getInt("usuario")).setRotation( 270.0f -
                                                    (float)calcularAnguloV2(
                                                            latlng_actual.latitude,
                                                            latlng_actual.longitude,
                                                            latlng_nueva.latitude,
                                                            latlng_nueva.longitude) );
                                            marcadores.get(json_object.getInt("usuario")).setPosition( latlng_nueva );
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }}, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public static double calcularAngulo(double latA, double lonA, double latB, double lonB) {
        double deltaLng = lonB - lonA;
        double deltaLat = latB - latA;
        // Imprimir el resultado (opcional)
        System.out.println("Ángulo entre los puntos: " + Math.toDegrees(Math.atan2(deltaLng, deltaLat)) + " grados");

        return Math.toDegrees(Math.atan2(deltaLng, deltaLat));
    }

    public static double calcularAnguloV2(double lat1, double lon1, double lat2, double lon2) {
        // Convertir las latitudes y longitudes de grados a radianes
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calcular la diferencia de longitudes
        double deltaLon = lon2Rad - lon1Rad;

        // Calcular el ángulo usando la fórmula del coseno
        double cosTheta = Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);

        // Obtener el ángulo en radianes
        double thetaRad = Math.acos(cosTheta);

        // Convertir el ángulo de radianes a grados
        double thetaGrados = Math.toDegrees(thetaRad);

        return thetaGrados;
    }

}