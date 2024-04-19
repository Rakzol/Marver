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

import com.example.test4.databinding.MapaBinding;

public class Ruta extends Fragment implements OnMapReadyCallback {

    private GoogleMap gMap;

    List<Polyline> polilineas = new ArrayList<>();

    List<Marker> marcadores = new ArrayList<>();

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

            }
        });

        mapaBinding.btnFinalizarEntregaPedidosMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

    private void actualizar(){
        if( gMap != null ){
            actualizador = Executors.newSingleThreadScheduledExecutor();
            actualizador.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {

                    try{
                        URL url = new URL("https://www.marverrefacciones.mx/android/rutas_repartidores" );
                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                        conexion.setRequestMethod("POST");
                        conexion.setDoOutput(true);

                        SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

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

                        JSONObject json_pedidos = new JSONObject( constructor_cadena.toString() );

                        ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
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

                                    JSONArray polilinea = json_pedidos.getJSONArray("repartidores").getJSONObject(0).getJSONArray("polilinea");
                                    JSONArray ultima = polilinea.getJSONArray(polilinea.length() - 1);

                                    marcadores.add(
                                            gMap.addMarker( new MarkerOptions()
                                                    .position( new LatLng(
                                                            ultima.getDouble(1),
                                                            ultima.getDouble(0)
                                                    ) )
                                                    .title( "asasdasd" )
                                                    .snippet( "Folio: " + "asasdasd" )
                                                    .icon(
                                                            BitmapDescriptorFactory.fromResource( R.drawable.marcador_marver )
                                                    )
                                            )
                                    );


                                    /*PolylineOptions configuracion_polilinea = new PolylineOptions()
                                            .addAll(poli_linea_decodificada)
                                            .color( Color.argb(255, 100, 149, 237))
                                            .width(10);

                                    if(primera_carga){
                                        gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(poli_linea_decodificada), 250));
                                        primera_carga = false;
                                    }

                                    poli_lineaes.add( gMap.addPolyline(configuracion_polilinea) );*/

                                }catch (Exception ex){}
                            }
                        });

                        /*if(json_pedidos.getInt("status") == 1){
                            ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                @Override
                                public void run() {
                                    //mapaBinding.btnIniciarEntregaPedidosMapa.setVisibility( View.VISIBLE );
                                }
                            });
                        }*/
                    }catch (Exception ex){}

                }}, 0, 25000, TimeUnit.MILLISECONDS);
        }
    }
    private void desactualizar(){
        if( actualizador != null ){
            actualizador.shutdownNow();
        }
    }

    private List<LatLng> geoPolylineToGooglePolyline(List<double[]> geoPolylines) {
        List<LatLng> googlePolylines = new ArrayList<>();
        for (double[] geoPolyline : geoPolylines) {
            googlePolylines.add(new LatLng(geoPolyline[1], geoPolyline[0]));
        }
        return googlePolylines;
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