package com.example.test4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class Mapa extends Fragment implements OnMapReadyCallback, fragmentoBuscador {

    private GoogleMap gMap;
    private List<Usuario> usuarios = new ArrayList<>();
    private AdaptadorUsuarios adaptadorUsuarios;
    private ScheduledExecutorService actualizador_visual;
    private ScheduledExecutorService actualizador_logico;

    private RecyclerView listaUsuariosFiltrados;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.mapa, container, false);

        Intent intent_servicioGPS = new Intent(getActivity(), ServicioGPS.class);
        getActivity().startService(intent_servicioGPS);

        /*LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        ((RecyclerView)view.findViewById(R.id.listaUsuariosFiltrados)).setLayoutManager(linearLayoutManager);*/

        adaptadorUsuarios = new AdaptadorUsuarios(usuarios);

        adaptadorUsuarios.setOnClickListener(new AdaptadorUsuarios.OnClickListener() {
            @Override
            public void onClick(int position, Usuario usuario) {
                ((SearchView)getActivity().findViewById(R.id.buscadorUsuarios)).setIconified(true);
                ((SearchView)getActivity().findViewById(R.id.buscadorUsuarios)).onActionViewCollapsed();
                listaUsuariosFiltrados.setVisibility(View.GONE);
                gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom( usuario.marcador.getPosition(), 18.5f));
                usuario.marcador.showInfoWindow();
            }
        });


        listaUsuariosFiltrados = view.findViewById(R.id.listaUsuariosFiltrados);
        listaUsuariosFiltrados.setAdapter(adaptadorUsuarios);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setMapStyle( MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style) );

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.25f));

        actualizar();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        desactualizar();
    }

    @Override
    public void onPause() {
        super.onPause();
        desactualizar();
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizar();
    }

    private void actualizar_logica(){

        try{
            SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

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
                    ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Marker marcador = gMap.addMarker( new MarkerOptions()
                                        .position( new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")) )
                                        .title(json_object.getString("Nombre"))
                                        .snippet("Usuario: " + json_object.getInt("usuario"))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador)));
                                usuarios.add(new Usuario(json_object.getInt("usuario"), json_object.getString("Nombre"), marcador, new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")), new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")), new LatLng(json_object.getDouble("latitud"),json_object.getDouble("longitud")) ));
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
                    ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
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

    @Override
    public void buscador_cerrado() {
        listaUsuariosFiltrados.setVisibility(View.GONE);
    }

    @Override
    public void buscador_clickeado() {
        adaptadorUsuarios.filtrar("");
        listaUsuariosFiltrados.setVisibility(View.VISIBLE);
    }

    @Override
    public void buscador_escrito(String newText) {
        adaptadorUsuarios.filtrar(newText);
    }
}