package com.example.test4;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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
    //Marker marcadorMarver;
    Marker marcadorRepartidor;

    //long timeStamp = 0;

    //int idPedido = 0;

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

        mapaBinding.buttonIniciarEntregaMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapaBinding.buttonIniciarEntregaMapa.setEnabled(false);
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
                            output_sream.write(( "clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") ).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json_resultado = new JSONObject( constructor_cadena.toString() );

                            ((Aplicacion)requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        mapaBinding.buttonIniciarEntregaMapa.setEnabled(true);

                                        if(json_resultado.getInt("status") == 0){
                                            refrescar();
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

        mapaBinding.buttonFinalizarEntregaMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapaBinding.buttonFinalizarEntregaMapa.setEnabled(false);
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
                            output_sream.write(( "clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") ).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json_resultado = new JSONObject( constructor_cadena.toString() );

                            ((Aplicacion)requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        mapaBinding.buttonFinalizarEntregaMapa.setEnabled(true);

                                        if(json_resultado.getInt("status") == 0){
                                            refrescar();
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

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragmentMapMapa);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setMapStyle( MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style) );

        gMap.setInfoWindowAdapter( new CustomInfoWindow(getContext()) );

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.7891565,-108.9953355), 13.25f));

        actualizar();
        refrescar();
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
            actualizador.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    ((Aplicacion)requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                            if(marcadorRepartidor == null){
                                marcadorRepartidor = gMap.addMarker( new MarkerOptions()
                                        .position( new LatLng(
                                                Double.parseDouble(preferencias_compartidas.getString("latitud", "0")),
                                                Double.parseDouble(preferencias_compartidas.getString("longitud", "0"))
                                        ) )
                                        .title( preferencias_compartidas.getString("usuario", "" ))
                                        .snippet( String.valueOf(preferencias_compartidas.getInt("clave", 0 )) )
                                        .icon(
                                                BitmapDescriptorFactory.fromResource( R.drawable.marcador )
                                        )
                                        .zIndex(2)
                                );
                            }else{
                                marcadorRepartidor.setPosition( new LatLng( Double.parseDouble(preferencias_compartidas.getString("latitud", "0")), Double.parseDouble(preferencias_compartidas.getString("longitud", "0")) ) );
                            }
                        }
                    });

                }}, 0, 500, TimeUnit.MILLISECONDS);
        }
    }
    private void desactualizar(){
        if( actualizador != null ){
            actualizador.shutdownNow();
        }
    }

    private void refrescar(){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try{
                    URL url = new URL("https://www.marverrefacciones.mx/android/rutas_repartidores");
                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                    conexion.setRequestMethod("POST");
                    conexion.setDoOutput(true);

                    SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                    OutputStream output_sream = conexion.getOutputStream();
                    output_sream.write(("repartidor=" + preferencias_compartidas.getInt("clave", 0) ).getBytes());
                    output_sream.flush();
                    output_sream.close();

                    BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                    String linea;
                    StringBuilder constructor_cadena = new StringBuilder();
                    while ((linea = bufer_lectura.readLine()) != null) {
                        constructor_cadena.append(linea).append("\n");
                    }

                    JSONObject jsonResultado = new JSONObject(constructor_cadena.toString());

                    ((Aplicacion)requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                        @Override
                        public void run() {

                            try{

                                for(int c = 0; c < polilineas.size(); c++ ){
                                    polilineas.get(c).remove();
                                }
                                polilineas.clear();

                                for(int c = 0; c < marcadores.size(); c++ ){
                                    marcadores.get(c).remove();
                                }
                                marcadores.clear();

                                if(jsonResultado.has("marver")){
                                    JSONObject marver = jsonResultado.getJSONObject("marver");

                                    mapaBinding.buttonFinalizarEntregaMapa.setVisibility( View.VISIBLE );
                                    mapaBinding.buttonIniciarEntregaMapa.setVisibility( View.GONE );

                                    mapaBinding.textDistanciaMapa.setText( (marver.optInt("metrosEstimadosSumatoria") / 1000) + " Km" );
                                    mapaBinding.textTiempoMapa.setText( (marver.optInt("segundosEstimadosSumatoria") / 60) + " min" );

                                    JSONArray pedidos = jsonResultado.getJSONArray("pedidos");
                                    Boolean entregaActualEncontrada = false;
                                    for(int c = 0; c < pedidos.length(); c++){
                                        JSONObject pedido = pedidos.getJSONObject(c);

                                        String tipo = pedido.optString("status");
                                        if(tipo.contains("NO ENTREGADO") || tipo.contains("RECHAZADO")){
                                            tipo = "rechazado";
                                        }else if(tipo.contains("ENTREGADO")){
                                            tipo = "entregado";
                                        }else{
                                            tipo = "pendiente";
                                        }

                                        String colorPolylinea = "#FF0000";
                                        Integer indicePolylinea = 0;
                                        if(!entregaActualEncontrada && tipo == "pendiente"){
                                            colorPolylinea = "#87CEEB";
                                            indicePolylinea = 1;
                                            entregaActualEncontrada = true;
                                        }

                                        if(pedido.optInt("tipoComprobante") != 3){
                                            marcadores.add(
                                                    gMap.addMarker( new MarkerOptions()
                                                            .position( new LatLng(
                                                                    pedido.optDouble("latitud", 0),
                                                                    pedido.optDouble("longitud", 0)) )
                                                            .title("Pedido Normal")
                                                            .snippet(
                                                                    "Llegada estimada: " + pedido.optString("fechaLlegadaEstimada") + "\n" +
                                                                            "Llegada: " + pedido.optString("fechaLlegada") + "\n" +
                                                                            "Eficiencia: " + pedido.optInt("fechaLlegadaEficiencia") + "\n" +
                                                                            "Status: " + pedido.optString("status") + "\n" +
                                                                            "Pedido: " + pedido.optInt("pedido") + "\n" +
                                                                            "Cliente: " + pedido.optInt("clienteClave") + " " + pedido.optString("clienteNombre") + "\n" +
                                                                            "Calle: " + pedido.optString("calle") + "\n" +
                                                                            "Colonia: " + pedido.optString("colonia") + "\n" +
                                                                            "Codigo postal: " + pedido.optString("codigoPostal") + "\n" +
                                                                            "Número exterior: " + pedido.optString("numeroExterior") + "\n" +
                                                                            "Número interior: " + pedido.optString("numeroInterior") + "\n" +
                                                                            "Observaciones: " + pedido.optString("observacionesUbicacion") + "\n" +

                                                                            "Folio: " + pedido.optInt("folioComprobante") + "\n" +
                                                                            "Comprobante: " + pedido.optInt("tipoComprobante") + "\n" +
                                                                            "Codigos: " + pedido.optInt("codigos") + "\n" +
                                                                            "Unidades: " + pedido.optInt("piezas") + "\n" +
                                                                            "Total: " + pedido.optDouble("total") + "\n" +
                                                                            "Observaciones: " + pedido.optString("observacionesPedido"))
                                                            .icon(
                                                                    BitmapDescriptorFactory.fromResource( getResources().getIdentifier( tipo + "_" + ( pedido.optInt("indice") + 1 ), "drawable", requireActivity().getPackageName()) )
                                                            )
                                                            .zIndex(3))
                                            );
                                        }else{
                                            System.out.println(tipo + "_" + pedido.optInt("indice"));
                                            marcadores.add(
                                                    gMap.addMarker( new MarkerOptions()
                                                            .position( new LatLng(
                                                                    pedido.optDouble("latitud", 0),
                                                                    pedido.optDouble("longitud", 0)) )
                                                            .title("Pedido Especial")
                                                            .snippet(
                                                                    "Llegada estimada: " + pedido.optString("fechaLlegadaEstimada") + "\n" +
                                                                            "Llegada: " + pedido.optString("fechaLlegada") + "\n" +
                                                                            "Eficiencia: " + pedido.optInt("fechaLlegadaEficiencia") + "\n" +
                                                                            "Status: " + pedido.optString("status") + "\n" +
                                                                            "Pedido: " + pedido.optInt("pedido") + "\n" +
                                                                            "Cliente: " + pedido.optInt("clienteClave") + " " + pedido.optString("clienteNombre") + "\n" +
                                                                            "Calle: " + pedido.optString("calle") + "\n" +
                                                                            "Colonia: " + pedido.optString("colonia") + "\n" +
                                                                            "Codigo postal: " + pedido.optString("codigoPostal") + "\n" +
                                                                            "Número exterior: " + pedido.optString("numeroExterior") + "\n" +
                                                                            "Número interior: " + pedido.optString("numeroInterior") + "\n" +
                                                                            "Observaciones: " + pedido.optString("observacionesUbicacion") + "\n" +

                                                                            "Observaciones: " + pedido.optString("observacionesPedido"))
                                                            .icon(
                                                                    BitmapDescriptorFactory.fromResource( getResources().getIdentifier( tipo + "_" + (pedido.optInt("indice") + 1), "drawable", requireActivity().getPackageName()) )
                                                            )
                                                            .zIndex(3))
                                            );
                                        }

                                        PolylineOptions configuracion_polilinea = new PolylineOptions()
                                                .addAll( decodePolyline(pedido.getString("polylineaCodificada")) )
                                                .color( Color.parseColor(colorPolylinea) )
                                                .width(5)
                                                .zIndex(indicePolylinea);

                                        polilineas.add( gMap.addPolyline(configuracion_polilinea) );
                                    }

                                    String colorPolylinea = "#FF0000";
                                    Integer indicePolylinea = 0;
                                    if(!entregaActualEncontrada){
                                        colorPolylinea = "#87CEEB";
                                        indicePolylinea = 1;
                                        entregaActualEncontrada = true;
                                    }

                                    marcadores.add(
                                            gMap.addMarker( new MarkerOptions()
                                                    .position( new LatLng(
                                                            marver.optDouble("latitud", 0),
                                                            marver.optDouble("longitud", 0)) )
                                                    .title("Marver Refacciones")
                                                    .snippet(
                                                            "Inicio: " + marver.optString("fechaInicio") + "\n" +
                                                                    "Llegada Estimada: " + marver.optString("fechaLlegadaEstimada"))
                                                    .icon(
                                                            BitmapDescriptorFactory.fromResource( R.drawable.marcador_marver )
                                                    )
                                                    .zIndex(3))
                                    );

                                    System.out.println(marver.optString("polylineaCodificada"));
                                    PolylineOptions configuracion_polilinea = new PolylineOptions()
                                            .addAll( decodePolyline(marver.optString("polylineaCodificada")) )
                                            .color( Color.parseColor(colorPolylinea) )
                                            .width(5)
                                            .zIndex(indicePolylinea);

                                    polilineas.add( gMap.addPolyline(configuracion_polilinea) );

                                }else{
                                    mapaBinding.buttonFinalizarEntregaMapa.setVisibility( View.GONE );
                                    mapaBinding.buttonIniciarEntregaMapa.setVisibility( View.VISIBLE );

                                    mapaBinding.textDistanciaMapa.setText("0 Km");
                                    mapaBinding.textTiempoMapa.setText("0 min");
                                }

                            }catch (Exception ex){ ex.printStackTrace(); }
                        }
                    });

                }catch (Exception ex){}
            }
        });

    }

    // Method to decode a polyline into a list of latitude/longitude points
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            poly.add(p);
        }

        return poly;
    }

    /*public static List<LatLng> geoPolylineToGooglePolyline( JSONArray geoPolylines ) {

        List<LatLng> googlePolylines = new ArrayList<>();
        try{
            for(int c = 0; c < geoPolylines.length(); c++){
                JSONArray geoPolyline = geoPolylines.getJSONArray(c);
                googlePolylines.add(new LatLng(geoPolyline.getDouble(1),geoPolyline.getDouble(0)));
            }
        }catch (Exception ex){}

        return googlePolylines;
    }*/

}