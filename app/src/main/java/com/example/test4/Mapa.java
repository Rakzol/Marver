package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.test4.databinding.MapaBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

        mapa.listaUsuarios.setAdapter();

        setContentView(mapa.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try{
            getMenuInflater().inflate(R.menu.barra_herramientas_menu, menu);

            MenuItem menuItem = menu.findItem(R.menu.barra_herramientas_menu);
            SearchView searchView = (SearchView) menuItem.getActionView();
            searchView.setQueryHint("Nombre De Repartidor");

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });


        }catch (Exception e){
            e.printStackTrace();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
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
                                                .title(json_object.getString("Nombre"))
                                                .snippet("Usuario: " + json_object.getInt("usuario"))
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador)));
                                        marcadores.put(json_object.getInt("usuario"), marcador);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }

                    int fps = 60;
                    for( int p = 1; p <= fps; p++ ){
                        for( int c = 0; c < json_array.length(); c++ ){
                            int finalP = p;
                            JSONObject json_object = json_array.getJSONObject(c);
                            ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        LatLng posicion_anterior = marcadores.get(json_object.getInt("usuario")).getPosition();
                                        LatLng posicion_nueva = new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud"));

                                        if( posicion_anterior.latitude != posicion_nueva.latitude || posicion_anterior.longitude != posicion_nueva.longitude ){

                                            //Sacamos la diferencia dependiendo del numero mayor,
                                            //En esta parte de mexico la latitud siempre es positiva y la longitud negativa
                                            double latitud_dif_abs = Math.abs( posicion_anterior.latitude - posicion_nueva.latitude ) * finalP / fps;
                                            double longitud_dif_abs = Math.abs( Math.abs(posicion_anterior.longitude) + posicion_nueva.longitude ) * finalP / fps;

                                            double latitud = posicion_anterior.latitude >= posicion_nueva.latitude ? posicion_anterior.latitude - latitud_dif_abs : posicion_anterior.latitude + latitud_dif_abs;
                                            double longitud = posicion_anterior.longitude >= posicion_nueva.longitude ? posicion_anterior.longitude - longitud_dif_abs : posicion_anterior.longitude + longitud_dif_abs;

                                            marcadores.get(json_object.getInt("usuario")).setPosition(
                                                    new LatLng(
                                                            latitud,
                                                            longitud));
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        Thread.sleep(1000/fps);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }}, 0, 1, TimeUnit.MILLISECONDS);
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