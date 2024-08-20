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
import android.widget.Toast;

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

import com.example.test4.databinding.MapaBinding;

public class Ruta extends Fragment implements OnMapReadyCallback {

    private GoogleMap gMap;

    List<Polyline> polilineas = new ArrayList<>();

    List<Marker> marcadores = new ArrayList<>();
    Marker marcadorMarver;
    Marker marcadorRepartidor;

    long timeStamp = 0;

    int idPedido = 0;

    private ScheduledExecutorService actualizador;

    private MapaBinding mapaBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mapaBinding = MapaBinding.inflate(inflater, container, false);

        View view = mapaBinding.getRoot();

        mapaBinding.btnIniciarEntregaPedidosMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapaBinding.btnIniciarEntregaPedidosMapa.setEnabled(false);
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://www.marverrefacciones.mx/android/iniciar_ruta");
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

                            JSONObject json_resultado = new JSONObject( constructor_cadena.toString() );

                            ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        mapaBinding.btnIniciarEntregaPedidosMapa.setEnabled(true);

                                        if(json_resultado.getInt("status") == 0){
                                            //desactualizar();
                                            actualizar();
                                        }else{
                                            Toast.makeText( getContext(), json_resultado.getString("mensaje"), Toast.LENGTH_LONG).show();
                                        }
                                    }catch (Exception ex){}
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        mapaBinding.btnFinalizarEntregaPedidosMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapaBinding.btnFinalizarEntregaPedidosMapa.setEnabled(false);
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            URL url = new URL("https://www.marverrefacciones.mx/android/finalizar_ruta");
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

                            JSONObject json_resultado = new JSONObject( constructor_cadena.toString() );

                            ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        mapaBinding.btnFinalizarEntregaPedidosMapa.setEnabled(true);

                                        if(json_resultado.getInt("status") == 0){
                                            //desactualizar();
                                            actualizar();
                                        }else{
                                            Toast.makeText( getContext(), json_resultado.getString("mensaje"), Toast.LENGTH_LONG).show();
                                        }
                                    }catch (Exception ex){}
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setMapStyle( MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style) );

        gMap.setInfoWindowAdapter( new CustomInfoWindow(getContext()) );

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.25f));

        marcadorMarver = gMap.addMarker( new MarkerOptions()
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

                        URL url = new URL("https://www.marverrefacciones.mx/android/rutas_repartidores" );
                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                        conexion.setRequestMethod("POST");
                        conexion.setDoOutput(true);

                        JSONObject envio = new JSONObject();
                        JSONObject repartidor = new JSONObject();
                        repartidor.put("id", preferencias_compartidas.getInt("id", 0));
                        repartidor.put("lat", preferencias_compartidas.getString("latitud", ""));
                        repartidor.put("lon", preferencias_compartidas.getString("longitud", ""));

                        envio.put("repartidor", repartidor);

                        OutputStream output_sream = conexion.getOutputStream();
                        output_sream.write(envio.toString().getBytes());
                        output_sream.flush();
                        output_sream.close();

                        BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                        String linea;
                        StringBuilder constructor_cadena = new StringBuilder();
                        while( (linea = bufer_lectura.readLine()) != null ){
                            constructor_cadena.append(linea).append("\n");
                        }

                        //System.out.println(constructor_cadena.toString());
                        JSONObject json_pedidos = new JSONObject( constructor_cadena.toString() );

                        ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                            @Override
                            public void run() {

                                try{

                                    for(int c = 0; c < polilineas.size(); c++ ){
                                        polilineas.get(c).remove();
                                    }
                                    polilineas.clear();

                                    JSONObject repartidor = json_pedidos.getJSONArray("repartidores").getJSONObject(0);
                                    JSONArray polilineaRepartidor = repartidor.getJSONArray("polilinea");
                                    JSONArray ultimaPosicionRepartidor = polilineaRepartidor.getJSONArray(polilineaRepartidor.length() - 1);

                                    if(marcadorRepartidor == null){
                                        marcadorRepartidor = gMap.addMarker( new MarkerOptions()
                                                .position( new LatLng(
                                                        ultimaPosicionRepartidor.getDouble(1),
                                                        ultimaPosicionRepartidor.getDouble(0)
                                                ) )
                                                .title( preferencias_compartidas.getString("usuario", "" ))
                                                .snippet( String.valueOf(preferencias_compartidas.getInt("id", 0 )) )
                                                .icon(
                                                        BitmapDescriptorFactory.fromResource( R.drawable.marcador )
                                                )
                                                .zIndex(2)
                                        );
                                    }else{
                                        marcadorRepartidor.setPosition( new LatLng( ultimaPosicionRepartidor.getDouble(1), ultimaPosicionRepartidor.getDouble(0) ) );
                                    }

                                    if(json_pedidos.has("incorporacion")){
                                        JSONObject incorporacion = json_pedidos.getJSONObject("incorporacion");

                                        PolylineOptions configuracion_polilinea = new PolylineOptions()
                                                .addAll( geoPolylineToGooglePolyline( incorporacion.getJSONArray("polilinea") ) )
                                                .color( Color.parseColor(incorporacion.getString("color")) )
                                                .width(5);

                                        polilineas.add( gMap.addPolyline(configuracion_polilinea) );
                                    }

                                    if( json_pedidos.has("id") ){

                                        Boolean pedidosEntregados = true;

                                        if( json_pedidos.getInt("id") != idPedido ){
                                            idPedido = json_pedidos.getInt("id");

                                            for(int c = 0; c < marcadores.size(); c++ ){
                                                marcadores.get(c).remove();
                                            }
                                            marcadores.clear();

                                            JSONObject ruta = json_pedidos.getJSONObject("ruta");

                                            /*Crar todos los marcadores de pedidos*/
                                            for(int c = 0; c < ruta.getJSONArray("legs").length() -1; c++){

                                                JSONObject leg = ruta.getJSONArray("legs").getJSONObject(c);
                                                JSONArray polilinea = leg.getJSONObject("polyline").getJSONArray("polilinea");
                                                JSONArray ultimaPosicion = polilinea.getJSONArray(polilinea.length()-1);
                                                JSONObject pedido = leg.getJSONObject("pedido");

                                                if(pedidosEntregados && pedido.getInt("status") == 4 ){
                                                    pedidosEntregados = false;
                                                }

                                                marcadores.add(
                                                        gMap.addMarker( new MarkerOptions()
                                                        .position( new LatLng(
                                                                ultimaPosicion.getDouble(1),
                                                                ultimaPosicion.getDouble(0)
                                                        ) )
                                                        .title( "Folio: " + pedido.getInt("folio") )
                                                        .snippet(
                                                                "Cliente: " + pedido.getInt("cliente_clave") + " " + pedido.getString("cliente_nombre") + "\n" +
                                                                "Pedido: " + pedido.getInt("pedido") + "\n" +
                                                                "Total: " + pedido.getDouble("total") + "\n" +
                                                                ( !pedido.isNull("feria") ? "Feria: " + pedido.getDouble("feria") + "\n" : "" ) +
                                                                ( !pedido.isNull("calle") ? "Calle: " + pedido.getString("calle") + "\n" : "" ) +
                                                                ( !pedido.isNull("numero_exterior") ? "Número exterior: " + pedido.getString("numero_exterior") + "\n" : "" ) +
                                                                ( !pedido.isNull("numero_interior") ? "Número interior: " + pedido.getString("numero_interior") + "\n" : "" ) +
                                                                "Llegada: " + leg.getString("llegada") + "\n" +
                                                                "Duración: " + leg.getString("Totalduration") + " Minutos\n" +
                                                                "Distancia: " + leg.getString("Totaldistance") + " Km."
                                                        )
                                                        .icon(
                                                                BitmapDescriptorFactory.fromResource( getResources().getIdentifier("marcador_cliente_"+(c+1) + ( pedido.getInt("status") != 4 ? "_verde" : "" ), "drawable", requireActivity().getPackageName()) )
                                                        )
                                                        .zIndex(3))
                                                );

                                            }
                                            /*Crar todos los marcadores de pedidos*/

                                            marcadorMarver.setSnippet(
                                                    "Llegada: " + ruta.getString("llegada") + "\n" +
                                                    "Duración: " + ruta.getString("duration") + " Minutos\n" +
                                                    "Distancia: " + ruta.getString("distance") + " Km."
                                            );

                                            mapaBinding.txtDistancia.setText( ruta.getString("distance") + "Km" );
                                            mapaBinding.txtTiempo.setText( ruta.getString("duration") + " min" );
                                        }else{
                                            /*Actualizar todos los marcadores de pedidos*/

                                            JSONObject ruta = json_pedidos.getJSONObject("ruta");

                                            for(int c = 0; c < ruta.getJSONArray("legs").length() -1; c++){

                                                JSONObject leg = ruta.getJSONArray("legs").getJSONObject(c);
                                                JSONObject pedido = leg.getJSONObject("pedido");

                                                if(pedidosEntregados && pedido.getInt("status") == 4 ){
                                                    pedidosEntregados = false;
                                                }

                                                marcadores.get(c).setIcon(
                                                        BitmapDescriptorFactory.fromResource( getResources().getIdentifier("marcador_cliente_"+(c+1) + ( pedido.getInt("status") != 4 ? "_verde" : "" ), "drawable", requireActivity().getPackageName()) )
                                                );

                                            }

                                            /*Actualizar todos los marcadores de pedidos*/
                                        }

                                        /* Dibujar las polilineas */
                                        JSONObject ruta = json_pedidos.getJSONObject("ruta");

                                        for(int c = 0; c < ruta.getJSONArray("legs").length(); c++){

                                            JSONObject leg = ruta.getJSONArray("legs").getJSONObject(c);

                                            PolylineOptions configuracion_polilinea = new PolylineOptions()
                                                    .addAll( geoPolylineToGooglePolyline( leg.getJSONObject("polyline").getJSONArray("polilinea") ) )
                                                    .color( Color.parseColor(leg.getString("color")) )
                                                    .width(5);

                                            polilineas.add( gMap.addPolyline(configuracion_polilinea) );

                                        }
                                        /* Dibujar las polilineas */

                                        /*Verificar si se puede mostrar el botón de finalizacion*/

                                        /*  Lo igualamos a tru para siempre poder finalizar la entrega sin realmente
                                            terminar de entrar, Si borramos esta linea de codigo regresara a la normalidad */
                                        pedidosEntregados = true;

                                        if(pedidosEntregados){
                                            mapaBinding.btnFinalizarEntregaPedidosMapa.setVisibility( View.VISIBLE );
                                            mapaBinding.btnIniciarEntregaPedidosMapa.setVisibility( View.GONE );
                                        }else{
                                            mapaBinding.btnFinalizarEntregaPedidosMapa.setVisibility( View.GONE );
                                            mapaBinding.btnIniciarEntregaPedidosMapa.setVisibility( View.GONE );
                                        }
                                        /*Verificar si se puede mostrar el botón de finalizacion*/
                                    }else{
                                        mapaBinding.btnFinalizarEntregaPedidosMapa.setVisibility( View.GONE );
                                        mapaBinding.btnIniciarEntregaPedidosMapa.setVisibility( View.VISIBLE );

                                        idPedido = 0;

                                        for(int c = 0; c < marcadores.size(); c++ ){
                                            marcadores.get(c).remove();
                                        }
                                        marcadores.clear();

                                        marcadorMarver.setSnippet("");

                                        mapaBinding.txtDistancia.setText("0 Km");
                                        mapaBinding.txtTiempo.setText("0 min");
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

    public static List<LatLng> geoPolylineToGooglePolyline( JSONArray geoPolylines ) {

        List<LatLng> googlePolylines = new ArrayList<>();
        try{
            for(int c = 0; c < geoPolylines.length(); c++){
                JSONArray geoPolyline = geoPolylines.getJSONArray(c);
                googlePolylines.add(new LatLng(geoPolyline.getDouble(1),geoPolyline.getDouble(0)));
            }
        }catch (Exception ex){}

        return googlePolylines;
    }

}