package com.example.test4;

import android.content.Context;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Mapa extends Fragment implements OnMapReadyCallback, fragmentoBuscador {

    private GoogleMap gMap;
    Marker marcador_cliente;
    Marker marcador_repartidor;

    long timeStamp = 0;

    String cliente, nombre_cliente;

    Polyline polilinea;

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

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragmentMapRuta);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setMapStyle( MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style) );

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.25f));

        gMap.addMarker( new MarkerOptions()
                .position( new LatLng(
                        25.7943047,
                        -108.9859510
                ) )
                .title( "Marver Refacciones" )
                .icon(
                        BitmapDescriptorFactory.fromResource( R.drawable.marcador_marver )
                )
                .zIndex(1)
        );

        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                View dialogView = getLayoutInflater().inflate(R.layout.dialogo_cambiar_posicion_cliente, null);

                builder.setView(dialogView);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                ((Button) dialogView.findViewById(R.id.buttonCancelarCambiarPosicionCliente)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });

                ((Button) dialogView.findViewById(R.id.buttonCambiarPosicionCliente)).setOnClickListener(new View.OnClickListener() {
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

                                    //desactualizar();
                                    actualizar();

                                    alertDialog.dismiss();
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

    private void actualizar(){
        if( gMap != null ){

            desactualizar();
            actualizador = Executors.newSingleThreadScheduledExecutor();
            actualizador.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {

                    try{
                        SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                        if(preferencias_compartidas.getLong("timeStamp", 0) == timeStamp){
                            return;
                        }
                        timeStamp = preferencias_compartidas.getLong("timeStamp", 0);

                        URL url = new URL("https://www.marverrefacciones.mx/android/posicion_cliente" );
                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                        conexion.setRequestMethod("POST");
                        conexion.setDoOutput(true);

                        OutputStream output_sream = conexion.getOutputStream();
                        output_sream.write( ( "clave=" + cliente + "&lat=" + preferencias_compartidas.getString("latitud", "") + "&lon=" + preferencias_compartidas.getString("longitud", "") ) .getBytes());
                        output_sream.flush();
                        output_sream.close();

                        BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                        String linea;
                        StringBuilder constructor_cadena = new StringBuilder();
                        while( (linea = bufer_lectura.readLine()) != null ){
                            constructor_cadena.append(linea).append("\n");
                        }

                        JSONObject json_ruta = new JSONObject( constructor_cadena.toString() );

                        ((Aplicacion)requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                            @Override
                            public void run() {

                                try{

                                    if(polilinea != null){
                                        polilinea.remove();
                                    }

                                    JSONArray polilineas = json_ruta.getJSONArray("polilineas");

                                    if(marcador_repartidor == null){
                                        marcador_repartidor = gMap.addMarker( new MarkerOptions()
                                                .position( new LatLng(
                                                        polilineas.getJSONArray(0).getDouble(1),
                                                        polilineas.getJSONArray(0).getDouble(0)
                                                ) )
                                                .title( preferencias_compartidas.getString("usuario", "" ))
                                                .snippet( String.valueOf(preferencias_compartidas.getInt("id", 0 )) )
                                                .icon(
                                                        BitmapDescriptorFactory.fromResource( R.drawable.marcador )
                                                )
                                                .zIndex(2)
                                        );
                                    }else{
                                        marcador_repartidor.setPosition( new LatLng( polilineas.getJSONArray(0).getDouble(1), polilineas.getJSONArray(0).getDouble(0) ) );
                                    }

                                    if( polilineas.length() > 1 ){

                                        if(marcador_cliente == null){
                                            marcador_cliente = gMap.addMarker( new MarkerOptions()
                                                    .position( new LatLng(
                                                            polilineas.getJSONArray(polilineas.length()-1).getDouble(1),
                                                            polilineas.getJSONArray(polilineas.length()-1).getDouble(0)
                                                    ) )
                                                    .title( preferencias_compartidas.getString("usuario", "" ))
                                                    .snippet( String.valueOf(preferencias_compartidas.getInt("id", 0 )) )
                                                    .icon(
                                                            BitmapDescriptorFactory.fromResource( R.drawable.marcador_cliente )
                                                    )
                                                    .zIndex(3)
                                            );
                                        }else{
                                            marcador_cliente.setPosition( new LatLng( polilineas.getJSONArray(polilineas.length()-1).getDouble(1), polilineas.getJSONArray(polilineas.length()-1).getDouble(0) ) );
                                        }

                                        /*PolylineOptions configuracion_polilinea = new PolylineOptions()
                                                .addAll( Ruta.geoPolylineToGooglePolyline( polilineas ) )
                                                .color( Color.parseColor(json_ruta.getString("color")) )
                                                .width(5);

                                        polilinea = gMap.addPolyline(configuracion_polilinea);*/

                                        ((TextView)getView().findViewById(R.id.textDistanciaMapa)).setText(json_ruta.getString("distancia"));

                                        ((TextView)getView().findViewById(R.id.textTiempoMapa)).setText(json_ruta.getString("tiempo"));
                                    }

                                }catch (Exception ex){ ex.printStackTrace(); }
                            }
                        });

                    }catch (Exception ex){}

                }}, 0, 1000, TimeUnit.MILLISECONDS);
        }
    }
    private void desactualizar(){
        if( actualizador != null ){
            actualizador.shutdownNow();
        }
        timeStamp = 0;
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

                    ((Aplicacion)requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
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