package com.example.test4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Mapa extends Fragment implements OnMapReadyCallback, fragmentoBuscador {

    private GoogleMap gMap;
    Marker marcador_cliente;
    Marker marcador_repartidor;

    String ultima_latitud = "", ultima_longitud = "";
    String cliente, nombre_cliente;

    Polyline poli_linea;

    Boolean primera_carga = true;

    private ScheduledExecutorService actualizador;

    public static Mapa NuevoMapa( String cliente, String nombre_cliente ){
        Mapa fragmento = new Mapa();
        Bundle argumentos = new Bundle();
        argumentos.putString("cliente", cliente);
        argumentos.putString("nombre_cliente", nombre_cliente);
        fragmento.setArguments(argumentos);

        return fragmento;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cliente = getArguments().getString("cliente");
        nombre_cliente = getArguments().getString("nombre_cliente");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.mapa, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setMapStyle( MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style) );

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.25f));

        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                View dialogView = getLayoutInflater().inflate(R.layout.dialogo_cambiar_posicion_cliente, null);

                builder.setView(dialogView);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                ((Button) dialogView.findViewById(R.id.btnCambairPosicionClienteCancelar)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });

                ((Button) dialogView.findViewById(R.id.btnCambairPosicionClienteCambiar)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Executors.newSingleThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    URL url = new URL("https://www.marverrefacciones.mx/android/actualizar_posicion");
                                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                    conexion.setRequestMethod("POST");
                                    conexion.setDoInput(false);
                                    conexion.setDoOutput(true);

                                    conexion.getOutputStream().write( ("c=" + cliente + "&la=" + latLng.latitude + "&lo=" + latLng.longitude ).getBytes());

                                    conexion.getResponseCode();

                                    ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try{
                                                if(marcador_cliente == null){
                                                    marcador_cliente = gMap.addMarker( new MarkerOptions()
                                                            .position( new LatLng( latLng.latitude, latLng.longitude ) )
                                                            .title("Cliente")
                                                            .snippet( nombre_cliente )
                                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador_cliente)));
                                                }else{
                                                    marcador_cliente.setPosition(new LatLng( latLng.latitude, latLng.longitude ));
                                                }

                                                primera_carga = true;
                                                trazar_ruta();

                                                alertDialog.dismiss();
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    }
                });

            }
        });

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

    private void actualizar_mapa(){
        ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
            @Override
            public void run() {
                try{
                    SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                    String latitud = preferencias_compartidas.getString("latitud", "");
                    String longitud = preferencias_compartidas.getString("longitud", "");

                    if(!latitud.isEmpty() && !longitud.isEmpty()){

                        if(ultima_latitud.equals(latitud) && ultima_longitud.equals(longitud)){
                            return;
                        }

                        ultima_latitud = latitud;
                        ultima_longitud = longitud;

                        if(marcador_repartidor == null){
                            marcador_repartidor = gMap.addMarker( new MarkerOptions()
                                    .position( new LatLng( Double.parseDouble(latitud), Double.parseDouble(longitud) ) )
                                    .title("Repartidor")
                                    .snippet( preferencias_compartidas.getString("usuario", null) )
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador)));
                        }else{
                            marcador_repartidor.setPosition( new LatLng( Double.parseDouble(latitud), Double.parseDouble(longitud) ) );
                            /*if(marcador_repartidor.isInfoWindowShown()){
                                gMap.moveCamera(CameraUpdateFactory.newLatLng( new LatLng( Double.parseDouble(latitud), Double.parseDouble(longitud) ) ));
                            }*/
                        }

                        trazar_ruta();

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void actualizar(){
        if( gMap != null ){
            actualizador = Executors.newSingleThreadScheduledExecutor();
            actualizador.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    actualizar_mapa();
                }}, 0, 1000, TimeUnit.MILLISECONDS);

            if(marcador_cliente == null){

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://www.marverrefacciones.mx/android/posicion_cliente");
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setDoOutput(true);

                            OutputStream output_sream = conexion.getOutputStream();
                            output_sream.write(( "c=" + cliente ).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json = new JSONObject( constructor_cadena.toString() );
                            ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if(!json.getString("latitud").equals("no") && !json.getString("longitud").equals("no")){
                                            marcador_cliente = gMap.addMarker( new MarkerOptions()
                                                    .position( new LatLng( Double.parseDouble(json.getString("latitud")), Double.parseDouble(json.getString("longitud")) ) )
                                                    .title("Cliente")
                                                    .snippet( nombre_cliente )
                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador_cliente)));

                                            trazar_ruta();
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
        }
    }
    private void desactualizar(){
        if( actualizador != null ){
            actualizador.shutdownNow();
        }
    }

    private void trazar_ruta(){
        try{
            //AIzaSyCAaLR-LdWOBIf1pDXFq8nDi3-j67uiheo
            if( marcador_repartidor != null && marcador_cliente != null ){

                JSONObject origin_latLng = new JSONObject();
                origin_latLng.put("latitude", marcador_repartidor.getPosition().latitude );
                origin_latLng.put("longitude", marcador_repartidor.getPosition().longitude );

                JSONObject origin_location = new JSONObject();
                origin_location.put("latLng", origin_latLng);

                JSONObject origin = new JSONObject();
                origin.put("location", origin_location);

                JSONObject destination_latLng = new JSONObject();
                destination_latLng.put("latitude", marcador_cliente.getPosition().latitude );
                destination_latLng.put("longitude", marcador_cliente.getPosition().longitude );

                JSONObject destination_location = new JSONObject();
                destination_location.put("latLng", destination_latLng);

                JSONObject destination = new JSONObject();
                destination.put("location", destination_location);

                JSONObject solicitud = new JSONObject();
                solicitud.put("origin", origin);
                solicitud.put("destination", destination);

                solicitud.put("travelMode", "TWO_WHEELER");
                solicitud.put("routingPreference", "TRAFFIC_AWARE");

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://routes.googleapis.com/directions/v2:computeRoutes");
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setRequestProperty("Content-Type", "application/json");
                            conexion.setRequestProperty("X-Goog-Api-Key", "AIzaSyCAaLR-LdWOBIf1pDXFq8nDi3-j67uiheo");
                            conexion.setRequestProperty("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline");
                            conexion.setDoOutput(true);

                            OutputStream output_sream = conexion.getOutputStream();
                            output_sream.write(solicitud.toString().getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json = new JSONObject( constructor_cadena.toString() );

                            ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        List<LatLng> poli_linea_decodificada = PolyUtil.decode(json.getJSONArray("routes").getJSONObject(0).getJSONObject("polyline").getString("encodedPolyline"));

                                        PolylineOptions configuracion_polilinea = new PolylineOptions()
                                                .addAll(poli_linea_decodificada)
                                                .color(Color.RED)
                                                .width(10);

                                        String tiempo_string = json.getJSONArray("routes").getJSONObject(0).getString("duration");
                                        tiempo_string = tiempo_string.substring(0, tiempo_string.length() - 1 );

                                        ((TextView)getView().findViewById(R.id.txtDistancia)).setText(
                                                String.format("%.1f", json.getJSONArray("routes").getJSONObject(0).getInt("distanceMeters") / 1000f )
                                                        + " Km"
                                        );

                                        ((TextView)getView().findViewById(R.id.txtTiempo)).setText(
                                                String.format("%.1f", Float.parseFloat(tiempo_string) / 60f )
                                                        + " min"
                                        );
                                        if(poli_linea != null){
                                            poli_linea.remove();
                                        }
                                        poli_linea = gMap.addPolyline(configuracion_polilinea);

                                        if(primera_carga == true){
                                            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(poli_linea_decodificada), 250));
                                            primera_carga = false;
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
        }catch (Exception ex){}
    }

    // Método para obtener los límites de la cámara basados en una lista de puntos LatLng
    private LatLngBounds getLatLngBounds(List<LatLng> points) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        return builder.build();
    }

    @Override
    public void buscador_enviado(String query) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try{
                    URL url = new URL("https://places.googleapis.com/v1/places:searchText");
                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                    conexion.setRequestMethod("POST");
                    conexion.setRequestProperty("Content-Type", "application/json");
                    conexion.setRequestProperty("X-Goog-Api-Key", "AIzaSyCAaLR-LdWOBIf1pDXFq8nDi3-j67uiheo");
                    conexion.setRequestProperty("X-Goog-FieldMask", "places.location");
                    conexion.setDoOutput(true);

                    JSONObject solicitud = new JSONObject();
                    solicitud.put("textQuery", query);
                    solicitud.put("maxResultCount", 1);

                    OutputStream output_sream = conexion.getOutputStream();
                    output_sream.write(solicitud.toString().getBytes());
                    output_sream.flush();
                    output_sream.close();

                    BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                    String linea;
                    StringBuilder constructor_cadena = new StringBuilder();
                    while( (linea = bufer_lectura.readLine()) != null ){
                        constructor_cadena.append(linea).append("\n");
                    }

                    JSONObject json = new JSONObject( constructor_cadena.toString() );

                    ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                JSONObject ubicacion = json.getJSONArray("places").getJSONObject(0).getJSONObject("location");

                                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(ubicacion.getDouble("latitude"), ubicacion.getDouble("longitude")), 18f));

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

    @Override
    public void buscador_cerrado() {
    }

    @Override
    public void buscador_clickeado() {
    }

    @Override
    public void buscador_escrito(String newText) {
    }

}