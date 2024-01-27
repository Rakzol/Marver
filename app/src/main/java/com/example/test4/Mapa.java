package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Mapa extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Usuario> usuariosFiltrados = new ArrayList<>();
    private AdaptadorUsuarios adaptadorUsuarios;
    private Intent intent_servicioGPS;

    private ScheduledExecutorService actualizador_visual;
    private ScheduledExecutorService actualizador_logico;

    private MapaBinding mapa;

    private SearchView searchView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent_servicioGPS = new Intent(this, ServicioGPS.class);
        startService(intent_servicioGPS);

        mapa = MapaBinding.inflate(getLayoutInflater());
        setContentView(mapa.getRoot());

        SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

        mapa.nombreRepartidor.setText( preferencias_compartidas.getString("usuario", "") );
        mapa.numeroRepartidor.setText( String.valueOf(preferencias_compartidas.getInt("id", 0)) );

        mapa.botonScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(Mapa.this);

                scanner
                .startScan()
                .addOnSuccessListener(
                        barcode -> {
                            String rawValue = barcode.getRawValue();

                            Toast.makeText(Mapa.this, rawValue, Toast.LENGTH_LONG).show();
                        })
                .addOnCanceledListener(
                        () -> {
                            // Task canceled
                        })
                .addOnFailureListener(
                        e -> {
                            // Task failed with an exception
                        });
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mapa.listaUsuarios.setLayoutManager(linearLayoutManager);
        adaptadorUsuarios = new AdaptadorUsuarios(usuariosFiltrados);

        adaptadorUsuarios.setOnClickListener(new AdaptadorUsuarios.OnClickListener() {
            @Override
            public void onClick(int position, Usuario usuario) {
                searchView.setIconified(true);
                searchView.onActionViewCollapsed();
                mapa.layerLista.setVisibility(View.GONE);
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom( usuario.marcador.getPosition(), 17f));
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
            preferencias_compartidas_editor.remove("id");
            preferencias_compartidas_editor.apply();

            desactualizar();

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

        actualizar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        desactualizar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        desactualizar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizar();
    }

    private void actualizar_logica(){

        try{
            SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

            String salida = "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "");

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
                                usuarios.add(new Usuario(json_object.getInt("usuario"), json_object.getString("Nombre"), marcador, new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")), new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")), new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")) ));
                                usuariosFiltrados.add(usuarios.get(usuarios.size()-1));
                                adaptadorUsuarios.notifyDataSetChanged();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }else{
                    usuario.posicion_nueva = new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud"));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void actualizar_vista(){

        try {
            int fps = 120;
            for( int p = 1; p <= fps; p++ ){
                for( Usuario usuario : usuarios ){
                    int finalP = p;
                    ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                        @Override
                        public void run() {
                            try{

                                if( usuario.marcador.isInfoWindowShown() ){
                                    gMap.moveCamera(CameraUpdateFactory.newLatLng(usuario.marcador.getPosition()));
                                }

                                if(finalP == 1){
                                    usuario.posicion_final = new LatLng( usuario.posicion_nueva.latitude, usuario.posicion_nueva.longitude );
                                }

                                if( usuario.posicion_inicial.latitude != usuario.posicion_final.latitude || usuario.posicion_inicial.longitude != usuario.posicion_final.longitude ){

                                    //Sacamos la diferencia dependiendo del numero mayor,
                                    //En esta parte de mexico la latitud siempre es positiva y la longitud negativa
                                    double latitud_dif_abs = Math.abs( usuario.posicion_inicial.latitude - usuario.posicion_final.latitude ) * finalP / fps;
                                    double longitud_dif_abs = Math.abs( Math.abs(usuario.posicion_inicial.longitude) + usuario.posicion_final.longitude ) * finalP / fps;

                                    double latitud = usuario.posicion_inicial.latitude >= usuario.posicion_final.latitude ? usuario.posicion_inicial.latitude - latitud_dif_abs : usuario.posicion_inicial.latitude + latitud_dif_abs;
                                    double longitud = usuario.posicion_inicial.longitude >= usuario.posicion_final.longitude ? usuario.posicion_inicial.longitude - longitud_dif_abs : usuario.posicion_inicial.longitude + longitud_dif_abs;

                                    usuario.marcador.setPosition(
                                            new LatLng(
                                                    latitud,
                                                    longitud));
                                }

                                if(finalP == fps){
                                    usuario.posicion_inicial = new LatLng( usuario.posicion_final.latitude, usuario.posicion_final.longitude );
                                }

                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
                Thread.sleep(5500/fps);
            }
            //System.out.println("Terminooooooo");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualizar(){
        if( gMap != null ){
            actualizador_logico = Executors.newSingleThreadScheduledExecutor();
            actualizador_visual = Executors.newSingleThreadScheduledExecutor();
            actualizador_logico.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    actualizar_logica();
                }}, 0, 500, TimeUnit.MILLISECONDS);

            actualizador_visual.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    actualizar_vista();
                }}, 0, 1, TimeUnit.NANOSECONDS);
        }
    }
    private void desactualizar(){
        if( actualizador_logico != null ){
            actualizador_logico.shutdownNow();
        }
        if( actualizador_visual != null ){
            actualizador_visual.shutdownNow();
        }
    }

    public static double calcularAngulo(double latA, double lonA, double latB, double lonB) {
        double deltaLng = lonB - lonA;
        double deltaLat = latB - latA;
        // Imprimir el resultado (opcional)
        //System.out.println("Ángulo entre los puntos: " + Math.toDegrees(Math.atan2(deltaLng, deltaLat)) + " grados");

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