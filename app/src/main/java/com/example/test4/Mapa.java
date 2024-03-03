package com.example.test4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Mapa extends Fragment implements OnMapReadyCallback {

    private GoogleMap gMap;
    Marker marcador_cliente;
    Marker marcador_repartidor;

    String ultima_latitud = "", ultima_longitud = "";
    String cliente, nombre_cliente;

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

                    String latitud = preferencias_compartidas.getString("latitud", null);
                    String longitud = preferencias_compartidas.getString("longitud", null);

                    if( latitud != null && longitud != null ){

                        if(ultima_latitud == latitud && ultima_longitud == longitud){
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
                            gMap.moveCamera(CameraUpdateFactory.newLatLng( new LatLng( Double.parseDouble(latitud), Double.parseDouble(longitud) ) ));
                        }

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
                                        if(json.getString("latitud") != "no" && json.getString("longitud") != "no" ){
                                            Marker marcador_cliente = gMap.addMarker( new MarkerOptions()
                                                    .position( new LatLng( Double.parseDouble(json.getString("latitud")), Double.parseDouble(json.getString("longitud")) ) )
                                                    .title("Cliente")
                                                    .snippet( nombre_cliente )
                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador_cliente)));
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

}