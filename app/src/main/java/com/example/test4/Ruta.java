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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.maps.android.PolyUtil;

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

public class Ruta extends Fragment implements OnMapReadyCallback {

    private GoogleMap gMap;

    List<Pedido> pedidos = new ArrayList<>();

    Marker marcador_repartidor;

    String ultima_latitud = "", ultima_longitud = "";

    List<Polyline> poli_lineaes = new ArrayList<>();

    List<Marker> marcadores_clientes = new ArrayList<>();

    Boolean primera_carga = true;

    private ScheduledExecutorService actualizador;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

            if(pedidos.isEmpty()){

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://www.marverrefacciones.mx/android/pedidos_en_ruta" );
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setDoOutput(true);

                            SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                            OutputStream output_sream = conexion.getOutputStream();
                            output_sream.write(( "clave=" + preferencias_compartidas.getInt("id", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") ).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONArray json_pedidos = new JSONArray( constructor_cadena.toString() );

                            System.out.println(json_pedidos);

                            for( int c = 0; c < json_pedidos.length(); c++ ){
                                JSONObject json_pedido = json_pedidos.getJSONObject(c);

                                pedidos.add( new Pedido(
                                        json_pedido.getString("fecha"),
                                        json_pedido.getInt("comprobante"),
                                        json_pedido.getInt("folio"),
                                        json_pedido.getInt("cliente_clave"),
                                        json_pedido.getString("cliente_nombre"),
                                        Integer.parseInt( json_pedido.getString("vendedor") ),
                                        json_pedido.getInt("codigos"),
                                        json_pedido.getInt("piezas"),
                                        json_pedido.getDouble("total"),
                                        null,
                                        null,
                                        View.GONE,
                                        View.GONE,
                                        true,
                                        json_pedido.optDouble("latitud"),
                                        json_pedido.optDouble("longitud"),
                                        json_pedido.optString("numero_exterior"),
                                        json_pedido.optString("numero_interior"),
                                        json_pedido.optString("observaciones"),
                                        json_pedido.optDouble("feria")
                                ) );

                            }

                            ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    trazar_ruta();
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

            System.out.println("Intentando trazar");
            if( marcador_repartidor != null && !pedidos.isEmpty() ){

                System.out.println("Trazando");
                JSONObject origin_latLng = new JSONObject();
                origin_latLng.put("latitude", marcador_repartidor.getPosition().latitude );
                origin_latLng.put("longitude", marcador_repartidor.getPosition().longitude );

                JSONObject origin_location = new JSONObject();
                origin_location.put("latLng", origin_latLng);

                JSONObject origin = new JSONObject();
                origin.put("location", origin_location);

                /*JSONObject intermediate_1_latLng = new JSONObject();
                intermediate_1_latLng.put("latitude", 25.8223585 );
                intermediate_1_latLng.put("longitude", -108.9971250 );

                JSONObject intermediate_1_location = new JSONObject();
                intermediate_1_location.put("latLng", intermediate_1_latLng);

                JSONObject intermediate_1 = new JSONObject();
                intermediate_1.put("location", intermediate_1_location);

                JSONObject intermediate_2_latLng = new JSONObject();
                intermediate_2_latLng.put("latitude", 25.80142123803296 );
                intermediate_2_latLng.put("longitude", -108.96246396568267 );

                JSONObject intermediate_2_location = new JSONObject();
                intermediate_2_location.put("latLng", intermediate_2_latLng);

                JSONObject intermediate_2 = new JSONObject();
                intermediate_2.put("location", intermediate_2_location);

                JSONObject intermediate_3_latLng = new JSONObject();
                intermediate_3_latLng.put("latitude", 25.8000570 );
                intermediate_3_latLng.put("longitude", -109.0123157 );

                JSONObject intermediate_3_location = new JSONObject();
                intermediate_3_location.put("latLng", intermediate_3_latLng);

                JSONObject intermediate_3 = new JSONObject();
                intermediate_3.put("location", intermediate_3_location);

                JSONObject intermediate_4_latLng = new JSONObject();
                intermediate_4_latLng.put("latitude", 25.7675927 );
                intermediate_4_latLng.put("longitude", -109.0022865 );

                JSONObject intermediate_4_location = new JSONObject();
                intermediate_4_location.put("latLng", intermediate_4_latLng);

                JSONObject intermediate_4 = new JSONObject();
                intermediate_4.put("location", intermediate_4_location);

                JSONArray intermediates = new JSONArray();
                intermediates.put(intermediate_1);
                intermediates.put(intermediate_2);
                intermediates.put(intermediate_3);
                intermediates.put(intermediate_4);*/

                JSONArray intermediates = new JSONArray();


                for(int c = 0; c < pedidos.size(); c++){
                    Pedido pedido = pedidos.get(c);
                    if( pedido.latitud != null && pedido.longitud != null && !Double.isNaN(pedido.latitud) && !Double.isNaN(pedido.longitud) ){

                        JSONObject intermediate_latLng = new JSONObject();
                        intermediate_latLng.put("latitude", pedido.latitud );
                        intermediate_latLng.put("longitude", pedido.longitud );

                        JSONObject intermediate_location = new JSONObject();
                        intermediate_location.put("latLng", intermediate_latLng);

                        JSONObject intermediate = new JSONObject();
                        intermediate.put("location", intermediate_location);

                        intermediates.put(intermediate);
                    }
                }

                JSONObject destination_latLng = new JSONObject();
                destination_latLng.put("latitude", 25.7942362 );
                destination_latLng.put("longitude", -108.9858341 );

                JSONObject destination_location = new JSONObject();
                destination_location.put("latLng", destination_latLng);

                JSONObject destination = new JSONObject();
                destination.put("location", destination_location);

                JSONObject solicitud = new JSONObject();
                solicitud.put("origin", origin);
                if(intermediates.length() > 0){
                    solicitud.put("intermediates", intermediates);
                }
                solicitud.put("destination", destination);

                solicitud.put("travelMode", "TWO_WHEELER");
                solicitud.put("routingPreference", "TRAFFIC_AWARE");
                if(intermediates.length() > 0){
                    solicitud.put("optimizeWaypointOrder", "true");
                }

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://routes.googleapis.com/directions/v2:computeRoutes");
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setRequestProperty("Content-Type", "application/json");
                            conexion.setRequestProperty("X-Goog-Api-Key", "AIzaSyCAaLR-LdWOBIf1pDXFq8nDi3-j67uiheo");
                            conexion.setRequestProperty("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.legs.distanceMeters,routes.optimizedIntermediateWaypointIndex,routes.legs.duration,routes.legs.polyline.encodedPolyline,routes.legs.startLocation,routes.legs.endLocation");
                            conexion.setDoOutput(true);

                            OutputStream output_sream = conexion.getOutputStream();
                            output_sream.write(solicitud.toString().getBytes());
                            output_sream.flush();
                            output_sream.close();

                            System.out.println("reciviendo");
                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json = new JSONObject( constructor_cadena.toString() );

                            System.out.println("a mostrar");
                            System.out.println(json);
                            ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {

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

                                        for(int c = 0; c < poli_lineaes.size(); c++ ){
                                            poli_lineaes.get(c).remove();
                                        }
                                        poli_lineaes.clear();

                                        for(int c = 0; c < marcadores_clientes.size(); c++ ){
                                            marcadores_clientes.get(c).remove();
                                        }
                                        marcadores_clientes.clear();

                                        JSONArray legs = json.getJSONArray("routes").getJSONObject(0).getJSONArray("legs");

                                        if( legs.length() > 1 ){
                                            for(int c = 0; c < legs.length() - 1; c++ ){
                                                JSONObject leg = legs.getJSONObject(c);

                                                /* polilineas */
                                                List<LatLng> poli_linea_decodificada = PolyUtil.decode( leg.getJSONObject("polyline").getString("encodedPolyline") );

                                                PolylineOptions configuracion_polilinea = new PolylineOptions()
                                                        .addAll(poli_linea_decodificada)
                                                        .color( c == 0 ? Color.argb(255, 100, 149, 237) : Color.BLACK )
                                                        .width(10);

                                                if(primera_carga && c == 0){
                                                    gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(poli_linea_decodificada), 250));
                                                    primera_carga = false;
                                                }

                                                poli_lineaes.add( gMap.addPolyline(configuracion_polilinea) );

                                                /* marcadores */

                                                Pedido pedido;

                                                try{
                                                    pedido = pedidos.get( json.getJSONArray("routes").getJSONObject(0).getJSONArray("optimizedIntermediateWaypointIndex").getInt(c) );
                                                }catch (Exception ex){
                                                    pedido = pedidos.get( 0 );
                                                }

                                                marcadores_clientes.add(
                                                        gMap.addMarker( new MarkerOptions()
                                                                .position( new LatLng(
                                                                        leg.getJSONObject("endLocation").getJSONObject("latLng").getDouble("latitude"),
                                                                        leg.getJSONObject("endLocation").getJSONObject("latLng").getDouble("longitude")
                                                                ) )
                                                                .title( pedido.cliente_nombre )
                                                                .snippet( "Folio: " + pedido.folio )
                                                                .icon(
                                                                        c < legs.length() - 1
                                                                                ? BitmapDescriptorFactory.fromResource( getResources().getIdentifier("marcador_cliente_"+(c+1), "drawable", requireActivity().getPackageName()) )
                                                                                : BitmapDescriptorFactory.fromResource( R.drawable.marcador_marver )
                                                                )
                                                        )
                                                );
                                            }
                                        }else{
                                            JSONObject leg = legs.getJSONObject(0);

                                            /* polilineas */
                                            List<LatLng> poli_linea_decodificada = PolyUtil.decode( leg.getJSONObject("polyline").getString("encodedPolyline") );

                                            PolylineOptions configuracion_polilinea = new PolylineOptions()
                                                    .addAll(poli_linea_decodificada)
                                                    .color( Color.argb(255, 100, 149, 237))
                                                    .width(10);

                                            if(primera_carga){
                                                gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(poli_linea_decodificada), 250));
                                                primera_carga = false;
                                            }

                                            poli_lineaes.add( gMap.addPolyline(configuracion_polilinea) );

                                            /* marcadores */

                                            marcadores_clientes.add(
                                                    gMap.addMarker( new MarkerOptions()
                                                            .position( new LatLng(
                                                                    leg.getJSONObject("endLocation").getJSONObject("latLng").getDouble("latitude"),
                                                                    leg.getJSONObject("endLocation").getJSONObject("latLng").getDouble("longitude")
                                                            ) )
                                                            .title( "Marver Refacciones" )
                                                            .snippet( "Sucursal Mochis" )
                                                            .icon(BitmapDescriptorFactory.fromResource( R.drawable.marcador_marver ))
                                                    )
                                            );
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

}