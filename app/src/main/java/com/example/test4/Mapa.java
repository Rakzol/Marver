package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Mapa extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Usuario> usuariosFiltrados = new ArrayList<>();
    private AdaptadorUsuarios adaptadorUsuarios;
    private Intent intent_servicioGPS;

    private ScheduledExecutorService pintador;

    private MapaBinding mapa;

    private Boolean pausado = false;

    private SearchView searchView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent_servicioGPS = new Intent(this, ServicioGPS.class);
        startService(intent_servicioGPS);

        mapa = MapaBinding.inflate(getLayoutInflater());
        setContentView(mapa.getRoot());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mapa.listaUsuarios.setLayoutManager(linearLayoutManager);
        adaptadorUsuarios = new AdaptadorUsuarios(usuariosFiltrados);

        adaptadorUsuarios.setOnClickListener(new AdaptadorUsuarios.OnClickListener() {
            @Override
            public void onClick(int position, Usuario usuario) {
                searchView.setIconified(true);
                searchView.onActionViewCollapsed();
                mapa.layerLista.setVisibility(View.GONE);
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom( usuario.marcador.getPosition(), 18f));
                usuario.marcador.showInfoWindow();
            }
        });

        mapa.listaUsuarios.setAdapter(adaptadorUsuarios);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                this,
                linearLayoutManager.getOrientation()
        );
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divisor));
        mapa.listaUsuarios.addItemDecoration(dividerItemDecoration);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.barra_herramientas_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.buscadorUsuarios);
        searchView = (SearchView) menuItem.getActionView();

        // Detectar cuando se hace clic para activar la escritura
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Se llama cuando se hace clic para activar la escritura.
                // Realiza las acciones que desees cuando se activa la escritura.
                mapa.layerLista.setVisibility(View.VISIBLE);
            }
        });

        // Detectar cuando se cierra la escritura (se hace clic en la "x")
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                // Se llama cuando se cierra la escritura (se hace clic en la "x").
                // Realiza las acciones que desees cuando se cancela la escritura.
                mapa.layerLista.setVisibility(View.GONE);
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                usuariosFiltrados.clear();
                for( Usuario usuario : usuarios ){
                    if( usuario.nombre.toLowerCase().contains( newText.toLowerCase() ) ||
                            usuario.id.toString().equals(newText)){
                        usuariosFiltrados.add(usuario);
                    }
                }
                adaptadorUsuarios.notifyDataSetChanged();
                return false;
            }
        });

            searchView.setQueryHint("Nombre de Usuario");

            /*menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // El SearchView se ha expandido (abierto)
                // Aquí puedes realizar acciones cuando se abre el SearchView
                mapa.layerLista.setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // El SearchView se ha colapsado (cerrado)
                // Aquí puedes realizar acciones cuando se cierra el SearchView
                mapa.layerLista.setVisibility(View.GONE);
                return true;
            }
        });*/

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.cerrarSesion){
            SharedPreferences.Editor preferencias_compartidas_editor = getSharedPreferences("credenciales", MODE_PRIVATE).edit();
            preferencias_compartidas_editor.remove("usuario");
            preferencias_compartidas_editor.remove("contraseña");
            preferencias_compartidas_editor.apply();

            pintador.shutdown();

            /*Intent intent_cierre = new Intent(this, ServicioGPS.class);
            intent_cierre.setAction("cerrar");
            startService(intent_cierre);
            stopService(intent_cierre);*/

            Intent intent = new Intent(this, Inicio.class);
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setMapStyle( MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style) );

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.25f));

        pintador = Executors.newSingleThreadScheduledExecutor();
        pintador.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

                String salida = "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "");

                if( preferencias_compartidas.getString("usuario", null) == null || pausado ){
                    System.out.println("Sin refresco de mapa: " + preferencias_compartidas.getString("usuario", null) + " " + pausado );
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                        Usuario usuario = Usuario.get(usuarios, json_object.getInt("usuario") );
                        if( usuario == null ){
                            ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Marker marcador = gMap.addMarker( new MarkerOptions()
                                                .position( new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")) )
                                                .title(json_object.getString("Nombre"))
                                                .snippet("Usuario: " + json_object.getInt("usuario"))
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador)));
                                        usuarios.add(new Usuario(json_object.getInt("usuario"), json_object.getString("Nombre"), marcador));
                                        usuariosFiltrados.add(usuarios.get(usuarios.size()-1));
                                        adaptadorUsuarios.notifyDataSetChanged();
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

                            Usuario usuario = Usuario.get(usuarios, json_object.getInt("usuario") );
                            ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        LatLng posicion_anterior = usuario.marcador.getPosition();
                                        LatLng posicion_nueva = new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud"));

                                        if( usuario.marcador.isInfoWindowShown() ){
                                            gMap.moveCamera(CameraUpdateFactory.newLatLng(usuario.marcador.getPosition()));
                                        }

                                        if( posicion_anterior.latitude != posicion_nueva.latitude || posicion_anterior.longitude != posicion_nueva.longitude ){

                                            //Sacamos la diferencia dependiendo del numero mayor,
                                            //En esta parte de mexico la latitud siempre es positiva y la longitud negativa
                                            double latitud_dif_abs = Math.abs( posicion_anterior.latitude - posicion_nueva.latitude ) * finalP / fps;
                                            double longitud_dif_abs = Math.abs( Math.abs(posicion_anterior.longitude) + posicion_nueva.longitude ) * finalP / fps;

                                            double latitud = posicion_anterior.latitude >= posicion_nueva.latitude ? posicion_anterior.latitude - latitud_dif_abs : posicion_anterior.latitude + latitud_dif_abs;
                                            double longitud = posicion_anterior.longitude >= posicion_nueva.longitude ? posicion_anterior.longitude - longitud_dif_abs : posicion_anterior.longitude + longitud_dif_abs;

                                            usuario.marcador.setPosition(
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

    @Override
    protected void onPause() {
        super.onPause();
        pausado = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        pausado = false;
    }
}