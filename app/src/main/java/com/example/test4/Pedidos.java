package com.example.test4;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Pedidos extends Fragment implements fragmentoBuscador {

    public static String PENDIENTES = "pendientes";
    public static String EN_RUTA = "en_ruta";
    public static String ENTREGADOS = "entregados";
    public static String FINALIZADOS = "finalizados";
    public static String NO_ENTREGADOS = "no_entregados";
    public static String NO_PAGADOS = "no_pagados";
    public static String RECHAZADOS = "rechazados";

    private String tipoPedido;

    private ActivityResultLauncher<Intent> lanzadorActividadResultado;

    public Pedido pedido_seleccionado;
    private AdaptadorPedidos adaptadorPedidos;

    private Boolean primera_consulta = true;
    private ScheduledExecutorService actualizador;

    private View view;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static Pedidos NuevoPedido(String tipoPedido){
        Pedidos fragmento = new Pedidos();
        Bundle argumentos = new Bundle();
        argumentos.putString("tipoPedido", tipoPedido);
        fragmento.setArguments(argumentos);

        return fragmento;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        view = inflater.inflate(R.layout.pedidos, container, false);

        tipoPedido = getArguments().getString("tipoPedido");

        lanzadorActividadResultado = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult resultado) {
                        if( resultado.getResultCode() == Activity.RESULT_OK ){
                            String ruta = resultado.getData().getStringExtra("ruta");
                            pedido_seleccionado.bitmapFoto = BitmapFactory.decodeFile(ruta);
                            adaptadorPedidos.notifyDataSetChanged();
                        }
                    }
                });

        ((TextView)view.findViewById(R.id.textInformacionPedidos)).setText( "Cargando " + ((Toolbar)requireActivity().findViewById(R.id.toolBarManejador)).getTitle() );

        actualizar();

        return view;
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
    private void desactualizar(){
        if( actualizador != null ){
            actualizador.shutdownNow();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        primera_consulta = true;
        actualizar();
    }

    private void seleccionarHora( View dialogView ){

        Calendar calendar = Calendar.getInstance();
        // Get the current hour and minute
        int hourAct = calendar.get(Calendar.HOUR_OF_DAY);  // 24-hour format
        int minuteAct = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (TimePicker view, int hourOfDay, int minute) -> {
                    // Formato AM/PM
                    String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                    int hourIn12Format = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                    if(hourIn12Format == 0) hourIn12Format = 12; // para mostrar 12:XX en lugar de 0:XX

                    // Mostrar la hora seleccionada en TextView
                    ((TextView) dialogView.findViewById(R.id.textLlegadaNotificarPedido)).setText(String.format("%02d:%02d %s", hourIn12Format, minute, amPm));
                }, hourAct, minuteAct, false); // 'false' para formato de 12 horas

        timePickerDialog.show();
    }

    private void actualizar(){

        desactualizar();
        actualizador = Executors.newSingleThreadScheduledExecutor();

        actualizador.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {

                ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

                if( activeNetwork == null ){
                    //System.out.println("Ya no hay conexion para actualizar");
                    return;
                }
                if(!activeNetwork.isConnectedOrConnecting()){
                    //System.out.println("Ya no hay conexion para actualizar");
                    return;
                }
                if ( activeNetwork.getType() != ConnectivityManager.TYPE_WIFI && !primera_consulta ) {
                    //System.out.println("Ya no hay wifi para actualizar");
                    return;
                }

                primera_consulta = false;

                try{
                    URL url = new URL("https://www.marverrefacciones.mx/android/pedidos_" + tipoPedido);
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

                    JSONArray json_pedidos = new JSONArray( constructor_cadena.toString() );

                    List<Pedido> lista_pedidos = new ArrayList<>();

                    for( int c = 0; c < json_pedidos.length(); c++ ){
                        JSONObject json_pedido = json_pedidos.getJSONObject(c);

                        lista_pedidos.add( new Pedido(
                                tipoPedido,
                                json_pedido.optString("fecha"),
                                json_pedido.optInt("pedido"),
                                json_pedido.optInt("pedidoRepartidor"),
                                json_pedido.optString("observacionesPedido"),
                                json_pedido.optInt("tipoComprobante"),
                                json_pedido.optInt("folioComprobante"),
                                json_pedido.optInt("clienteClave"),
                                json_pedido.optString("clienteNombre"),
                                json_pedido.optInt("repartidor"),
                                json_pedido.optInt("codigos"),
                                json_pedido.optInt("piezas"),
                                json_pedido.optDouble("total"),
                                json_pedido.optDouble("latitud"),
                                json_pedido.optDouble("longitud"),
                                json_pedido.optString("codigoPostal"),
                                json_pedido.optString("calle"),
                                json_pedido.optString("colonia"),
                                json_pedido.optString("numeroExterior"),
                                json_pedido.optString("numeroInterior"),
                                json_pedido.optString("observacionesUbicacion"),
                                json_pedido.optDouble("feria"),
                                null,
                                null,
                                View.GONE,
                                View.GONE
                        ) );

                    }

                    if( ((RecyclerView)view.findViewById(R.id.recyclerViewPedidos)).getAdapter() != null ){
                        if( ((RecyclerView)view.findViewById(R.id.recyclerViewPedidos)).getAdapter().getItemCount() == lista_pedidos.size() ){
                            //System.out.println("Son la misma cantidad");
                            return;
                        }
                    }

                    ((Aplicacion)requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if( lista_pedidos.size() > 0 ) {
                                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                                    ((RecyclerView) view.findViewById(R.id.recyclerViewPedidos)).setLayoutManager(linearLayoutManager);
                                    adaptadorPedidos = new AdaptadorPedidos(lista_pedidos, requireActivity());

                                    adaptadorPedidos.ColocarEscuchadorClickFotografiarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {
                                            pedido_seleccionado = pedido;
                                            Intent intent = new Intent(getContext(), Fotografiar.class);
                                            intent.putExtra("pedidoRepartidor", pedido.pedidoRepartidor);
                                            lanzadorActividadResultado.launch(intent);
                                        }
                                    });

                                    adaptadorPedidos.ColocarEscuchadorClickNotificarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            View dialogView = getLayoutInflater().inflate(R.layout.dialogo_notificar_pedido, null);

                                            builder.setView(dialogView);

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            ((Button) dialogView.findViewById(R.id.buttonCerrarNotificarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    alertDialog.dismiss();
                                                }
                                            });

                                            ((TextView) dialogView.findViewById(R.id.textLlegadaNotificarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    seleccionarHora(dialogView);
                                                }
                                            });

                                            ((TextView) dialogView.findViewById(R.id.textLlegadaNotificarPedido)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                                @Override
                                                public void onFocusChange(View v, boolean hasFocus) {
                                                    if (hasFocus) {
                                                        seleccionarHora(dialogView);
                                                    }
                                                }
                                            });

                                            ((Button) dialogView.findViewById(R.id.buttonNotificarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    ((ProgressBar) dialogView.findViewById(R.id.progressNotificarPedido)).setVisibility(View.VISIBLE);
                                                    ((Button) dialogView.findViewById(R.id.buttonNotificarPedido)).setVisibility(View.GONE);
                                                    Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                URL url = new URL("https://www.marverrefacciones.mx/android/notificar");
                                                                HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                                conexion.setRequestMethod("POST");
                                                                conexion.setDoOutput(true);

                                                                SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                                OutputStream output_sream = conexion.getOutputStream();
                                                                output_sream.write(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.folioComprobante + "&comprobante=" + pedido.tipoComprobante + "&camion=" + ((EditText) dialogView.findViewById(R.id.textCamionNotificarPedido)).getText() + "&llegada=" + ((EditText) dialogView.findViewById(R.id.textLlegadaNotificarPedido)).getText()).getBytes());
                                                                output_sream.flush();
                                                                output_sream.close();

                                                                BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                                                String linea;
                                                                StringBuilder constructor_cadena = new StringBuilder();
                                                                while ((linea = bufer_lectura.readLine()) != null) {
                                                                    constructor_cadena.append(linea).append("\n");
                                                                }

                                                                System.out.println(constructor_cadena.toString());
                                                                JSONObject json_resultado = new JSONObject(constructor_cadena.toString());

                                                                ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        try {
                                                                            if (json_resultado.getInt("status") != 0) {
                                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoNotificarPedido)).setImageResource(R.drawable.error);
                                                                            }
                                                                            Toast.makeText(getContext(), json_resultado.getString("mensaje"), Toast.LENGTH_LONG).show();
                                                                        } catch (Exception e) {
                                                                            Toast.makeText(getContext(), "Error con la conexion", Toast.LENGTH_LONG).show();
                                                                            ((ImageView) dialogView.findViewById(R.id.imageResultadoNotificarPedido)).setImageResource(R.drawable.error);
                                                                            e.printStackTrace();
                                                                        }
                                                                        ((ProgressBar) dialogView.findViewById(R.id.progressNotificarPedido)).setVisibility(View.GONE);
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoNotificarPedido)).setVisibility(View.VISIBLE);
                                                                        ((Button) dialogView.findViewById(R.id.buttonCerrarNotificarPedido)).setVisibility(View.VISIBLE);
                                                                    }
                                                                });
                                                            } catch (Exception e) {
                                                                ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        ((ProgressBar) dialogView.findViewById(R.id.progressNotificarPedido)).setVisibility(View.GONE);
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoNotificarPedido)).setImageResource(R.drawable.error);
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoNotificarPedido)).setVisibility(View.VISIBLE);
                                                                        ((Button) dialogView.findViewById(R.id.buttonCerrarNotificarPedido)).setVisibility(View.VISIBLE);
                                                                        Toast.makeText(getContext(), "Error con la conexion", Toast.LENGTH_LONG).show();
                                                                    }
                                                                });
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    });
                                                }
                                            });

                                        }
                                    });

                                    adaptadorPedidos.ColocarEscuchadorClickEntregarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            View dialogView = getLayoutInflater().inflate(R.layout.dialogo_procesar_pedido, null);

                                            builder.setView(dialogView);

                                            ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Entregando Pedido. . .");

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            ((Button) dialogView.findViewById(R.id.buttonCerrarProcesarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    alertDialog.dismiss();
                                                }
                                            });

                                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        URL url = new URL("https://www.marverrefacciones.mx/android/entregar_pedido");
                                                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                        conexion.setRequestMethod("POST");
                                                        conexion.setDoOutput(true);

                                                        SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                        OutputStream output_sream = conexion.getOutputStream();
                                                        output_sream.write(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.pedido).getBytes());
                                                        System.out.println(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.pedido));
                                                        output_sream.flush();
                                                        output_sream.close();

                                                        BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                                        String linea;
                                                        StringBuilder constructor_cadena = new StringBuilder();
                                                        while ((linea = bufer_lectura.readLine()) != null) {
                                                            constructor_cadena.append(linea).append("\n");
                                                        }

                                                        System.out.println(constructor_cadena.toString());
                                                        JSONObject json_resultado = new JSONObject(constructor_cadena.toString());

                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    if (json_resultado.getInt("status") != 0) {
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    } else {
                                                                        ((BottomNavigationView) requireActivity().findViewById(R.id.bottomNavigationViewManejador)).setSelectedItemId(R.id.itemEnRutaBarraNavegacionInferior);
                                                                    }
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText(json_resultado.getString("mensaje"));
                                                                } catch (Exception e) {
                                                                    ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                                    e.printStackTrace();
                                                                }
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                                ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                            }
                                                        });
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });

                                        }
                                    });

                                    adaptadorPedidos.ColocarEscuchadorClickNoEntregarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            View dialogView = getLayoutInflater().inflate(R.layout.dialogo_procesar_pedido, null);

                                            builder.setView(dialogView);

                                            ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Cambiando a no entregado. . .");

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            ((Button) dialogView.findViewById(R.id.buttonCerrarProcesarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    alertDialog.dismiss();
                                                }
                                            });

                                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        URL url = new URL("https://www.marverrefacciones.mx/android/no_entregar_pedido");
                                                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                        conexion.setRequestMethod("POST");
                                                        conexion.setDoOutput(true);

                                                        SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                        OutputStream output_sream = conexion.getOutputStream();
                                                        output_sream.write(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.pedido).getBytes());
                                                        output_sream.flush();
                                                        output_sream.close();

                                                        BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                                        String linea;
                                                        StringBuilder constructor_cadena = new StringBuilder();
                                                        while ((linea = bufer_lectura.readLine()) != null) {
                                                            constructor_cadena.append(linea).append("\n");
                                                        }

                                                        System.out.println(constructor_cadena.toString());
                                                        JSONObject json_resultado = new JSONObject(constructor_cadena.toString());

                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    if (json_resultado.getInt("status") != 0) {
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    } else {
                                                                        ((BottomNavigationView) requireActivity().findViewById(R.id.bottomNavigationViewManejador)).setSelectedItemId(R.id.itemEnRutaBarraNavegacionInferior);
                                                                    }
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText(json_resultado.getString("mensaje"));
                                                                } catch (Exception e) {
                                                                    ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                                    e.printStackTrace();
                                                                }
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                                ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                            }
                                                        });
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });

                                        }
                                    });

                                    adaptadorPedidos.ColocarEscuchadorClickNoPagarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            View dialogView = getLayoutInflater().inflate(R.layout.dialogo_procesar_pedido, null);

                                            builder.setView(dialogView);

                                            ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Cambiando a no pagado. . .");

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            ((Button) dialogView.findViewById(R.id.buttonCerrarProcesarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    alertDialog.dismiss();
                                                }
                                            });

                                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        URL url = new URL("https://www.marverrefacciones.mx/android/no_pagar_pedido");
                                                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                        conexion.setRequestMethod("POST");
                                                        conexion.setDoOutput(true);

                                                        SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                        OutputStream output_sream = conexion.getOutputStream();
                                                        output_sream.write(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.pedido).getBytes());
                                                        output_sream.flush();
                                                        output_sream.close();

                                                        BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                                        String linea;
                                                        StringBuilder constructor_cadena = new StringBuilder();
                                                        while ((linea = bufer_lectura.readLine()) != null) {
                                                            constructor_cadena.append(linea).append("\n");
                                                        }

                                                        System.out.println(constructor_cadena.toString());
                                                        JSONObject json_resultado = new JSONObject(constructor_cadena.toString());

                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    if (json_resultado.getInt("status") != 0) {
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    } else {
                                                                        ((BottomNavigationView) requireActivity().findViewById(R.id.bottomNavigationViewManejador)).setSelectedItemId(R.id.itemEnRutaBarraNavegacionInferior);
                                                                    }
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText(json_resultado.getString("mensaje"));
                                                                } catch (Exception e) {
                                                                    ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                                    e.printStackTrace();
                                                                }
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                                ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                            }
                                                        });
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });

                                        }
                                    });

                                    adaptadorPedidos.ColocarEscuchadorClickRechazarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            View dialogView = getLayoutInflater().inflate(R.layout.dialogo_procesar_pedido, null);

                                            builder.setView(dialogView);

                                            ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Rechazando Pedido. . .");

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            ((Button) dialogView.findViewById(R.id.buttonCerrarProcesarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    alertDialog.dismiss();
                                                }
                                            });

                                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        URL url = new URL("https://www.marverrefacciones.mx/android/rechazar_pedido");
                                                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                        conexion.setRequestMethod("POST");
                                                        conexion.setDoOutput(true);

                                                        SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                        OutputStream output_sream = conexion.getOutputStream();
                                                        output_sream.write(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.pedido).getBytes());
                                                        output_sream.flush();
                                                        output_sream.close();

                                                        BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                                        String linea;
                                                        StringBuilder constructor_cadena = new StringBuilder();
                                                        while ((linea = bufer_lectura.readLine()) != null) {
                                                            constructor_cadena.append(linea).append("\n");
                                                        }

                                                        System.out.println(constructor_cadena.toString());
                                                        JSONObject json_resultado = new JSONObject(constructor_cadena.toString());

                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    if (json_resultado.getInt("status") != 0) {
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    } else {
                                                                        ((BottomNavigationView) requireActivity().findViewById(R.id.bottomNavigationViewManejador)).setSelectedItemId(R.id.itemEnRutaBarraNavegacionInferior);
                                                                    }
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText(json_resultado.getString("mensaje"));
                                                                } catch (Exception e) {
                                                                    ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                                    e.printStackTrace();
                                                                }
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                                ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                            }
                                                        });
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });

                                        }
                                    });

                                    adaptadorPedidos.ColocarEscuchadorClickEliminarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            View dialogView = getLayoutInflater().inflate(R.layout.dialogo_procesar_pedido, null);

                                            builder.setView(dialogView);

                                            ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Eliminando Pedido. . .");

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            ((Button) dialogView.findViewById(R.id.buttonCerrarProcesarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    alertDialog.dismiss();
                                                }
                                            });

                                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        URL url = new URL("https://www.marverrefacciones.mx/android/eliminar_pedido");
                                                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                        conexion.setRequestMethod("POST");
                                                        conexion.setDoOutput(true);

                                                        SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                        OutputStream output_sream = conexion.getOutputStream();
                                                        output_sream.write(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.pedido).getBytes());
                                                        output_sream.flush();
                                                        output_sream.close();

                                                        BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                                        String linea;
                                                        StringBuilder constructor_cadena = new StringBuilder();
                                                        while ((linea = bufer_lectura.readLine()) != null) {
                                                            constructor_cadena.append(linea).append("\n");
                                                        }

                                                        System.out.println(constructor_cadena.toString());
                                                        JSONObject json_resultado = new JSONObject(constructor_cadena.toString());

                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    if (json_resultado.getInt("status") != 0) {
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    } else {
                                                                        ((BottomNavigationView) requireActivity().findViewById(R.id.bottomNavigationViewManejador)).setSelectedItemId(R.id.itemPendientesBarraNavegacionInferior);
                                                                    }
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText(json_resultado.getString("mensaje"));
                                                                } catch (Exception e) {
                                                                    ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                    ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                                    e.printStackTrace();
                                                                }
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                                ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                            }
                                                        });
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });

                                        }
                                    });

                                    adaptadorPedidos.ColocarEscuchadorClickFinalizarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                            View dialogView = getLayoutInflater().inflate(R.layout.dialogo_finalizar_pedido, null);

                                            builder.setView(dialogView);

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            ((Button) dialogView.findViewById(R.id.buttonCerrarFinalizarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    alertDialog.dismiss();
                                                }
                                            });

                                            ((Button) dialogView.findViewById(R.id.buttonFinalizarPedido)).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    ((ProgressBar) dialogView.findViewById(R.id.progressFinalizarPedido)).setVisibility(View.VISIBLE);
                                                    ((Button) dialogView.findViewById(R.id.buttonFinalizarPedido)).setVisibility(View.GONE);
                                                    Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                URL url = new URL("https://www.marverrefacciones.mx/android/finalizar_pedido");
                                                                HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                                conexion.setRequestMethod("POST");
                                                                conexion.setDoOutput(true);

                                                                SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                                OutputStream output_sream = conexion.getOutputStream();
                                                                output_sream.write(("clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.pedido + "&codigo=" + ((EditText) dialogView.findViewById(R.id.textCodigoFinalizarPedido)).getText()).getBytes());
                                                                output_sream.flush();
                                                                output_sream.close();

                                                                BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                                                String linea;
                                                                StringBuilder constructor_cadena = new StringBuilder();
                                                                while ((linea = bufer_lectura.readLine()) != null) {
                                                                    constructor_cadena.append(linea).append("\n");
                                                                }

                                                                System.out.println(constructor_cadena.toString());
                                                                JSONObject json_resultado = new JSONObject(constructor_cadena.toString());

                                                                ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        try {
                                                                            if (json_resultado.getInt("status") != 0) {
                                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoFinalizarPedido)).setImageResource(R.drawable.error);
                                                                            }else{
                                                                                ((BottomNavigationView) requireActivity().findViewById(R.id.bottomNavigationViewManejador)).setSelectedItemId(R.id.itemEnRutaBarraNavegacionInferior);
                                                                            }
                                                                            Toast.makeText(getContext(), json_resultado.getString("mensaje"), Toast.LENGTH_LONG).show();
                                                                        } catch (Exception e) {
                                                                            Toast.makeText(getContext(), "Error en la conexion", Toast.LENGTH_LONG).show();
                                                                            ((ImageView) dialogView.findViewById(R.id.imageResultadoFinalizarPedido)).setImageResource(R.drawable.error);
                                                                            e.printStackTrace();
                                                                        }
                                                                        ((ProgressBar) dialogView.findViewById(R.id.progressFinalizarPedido)).setVisibility(View.GONE);
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoFinalizarPedido)).setVisibility(View.VISIBLE);
                                                                        ((Button) dialogView.findViewById(R.id.buttonCerrarFinalizarPedido)).setVisibility(View.VISIBLE);
                                                                    }
                                                                });
                                                            } catch (Exception e) {
                                                                ((Aplicacion) requireActivity().getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        ((ProgressBar) dialogView.findViewById(R.id.progressFinalizarPedido)).setVisibility(View.GONE);
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoFinalizarPedido)).setImageResource(R.drawable.error);
                                                                        ((ImageView) dialogView.findViewById(R.id.imageResultadoFinalizarPedido)).setVisibility(View.VISIBLE);
                                                                        ((Button) dialogView.findViewById(R.id.buttonCerrarFinalizarPedido)).setVisibility(View.VISIBLE);
                                                                        Toast.makeText(getContext(), "Error con la conexion", Toast.LENGTH_LONG).show();
                                                                    }
                                                                });
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    });
                                                }
                                            });

                                        }
                                    });

                                    ((RecyclerView)view.findViewById(R.id.recyclerViewPedidos)).setAdapter(adaptadorPedidos);
                                    view.findViewById(R.id.textInformacionPedidos).setVisibility( View.GONE );
                                    view.findViewById(R.id.recyclerViewPedidos).setVisibility( View.VISIBLE );
                                    adaptadorPedidos.notifyDataSetChanged();

                                    //System.out.println("Actualizados");
                                }else{
                                    //System.out.println("No hay");

                                    ((RecyclerView)view.findViewById(R.id.recyclerViewPedidos)).setAdapter(null);
                                    view.findViewById(R.id.recyclerViewPedidos).setVisibility( View.INVISIBLE );
                                    ((TextView)view.findViewById(R.id.textInformacionPedidos)).setText( "No hay " + ((Toolbar)requireActivity().findViewById(R.id.toolBarManejador)).getTitle() );
                                    view.findViewById(R.id.textInformacionPedidos).setVisibility( View.VISIBLE );
                                }

                                view.findViewById(R.id.progressPedidos).setVisibility( View.GONE );
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void buscador_cerrado() {

    }

    @Override
    public void buscador_clickeado() {

    }

    @Override
    public void buscador_escrito(String newText) {
        if(adaptadorPedidos != null){
            adaptadorPedidos.filtrar(newText);
        }
    }

    @Override
    public void buscador_enviado(String query) {

    }

    public static void subir_fotos(Context contexto){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try{
                    SharedPreferences procesos = contexto.getSharedPreferences("procesos", Context.MODE_PRIVATE);
                    SharedPreferences.Editor procesosEdit = contexto.getSharedPreferences("procesos", Context.MODE_PRIVATE).edit();

                    SharedPreferences credenciales = contexto.getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                    if( !procesos.getBoolean("subiendo_fotos", false) ){
                        //System.out.println("Subiendo fotos. . . . .");
                        procesosEdit.putBoolean("subiendo_fotos", true);
                        procesosEdit.apply();

                        File filesDir = contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                        if(filesDir == null || !filesDir.isDirectory()){
                            procesosEdit.putBoolean("subiendo_fotos", false);
                            procesosEdit.apply();
                            //System.out.println("Ya no hay ARCHIVOOOS");
                            return;
                        }

                        File[] files = filesDir.listFiles();

                        while( files != null && files.length > 0 ){

                            if( credenciales.getInt("clave", 0) == 0 ){
                                procesosEdit.putBoolean("subiendo_fotos", false);
                                procesosEdit.apply();
                                //System.out.println("Ya no hay USUARIOOOO");
                                return;
                            }

                            ConnectivityManager connectivityManager = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

                            if( activeNetwork == null ){
                                procesosEdit.putBoolean("subiendo_fotos", false);
                                procesosEdit.apply();
                                //System.out.println("Ya no hay CONEXIOOON");
                                return;
                            }

                            if (!activeNetwork.isConnectedOrConnecting()) {
                                procesosEdit.putBoolean("subiendo_fotos", false);
                                procesosEdit.apply();
                                //System.out.println("Ya no hay CONEXIOOON");
                                return;
                            }

                            if (activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
                                procesosEdit.putBoolean("subiendo_fotos", false);
                                procesosEdit.apply();
                                //System.out.println("Ya no hay WIFIIII");
                                return;
                            }

                            //System.out.println("WIFI");

                            File firstFile = files[0];

                            FileInputStream fileInputStream = new FileInputStream(firstFile);
                            byte[] bytes = new byte[(int) firstFile.length()];
                            fileInputStream.read(bytes);
                            fileInputStream.close();

                            URL url = new URL("https://www.marverrefacciones.mx/android/subir_foto");
                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                            conexion.setRequestMethod("POST");
                            conexion.setDoOutput(true);

                            OutputStream output_sream = conexion.getOutputStream();
                            //Base64.encodeToString(bytes, Base64.NO_WRAP).replace(" ", "+")
                            output_sream.write( ( "foto=" + Base64.encodeToString(bytes, Base64.NO_WRAP) + "&clave=" + credenciales.getInt("clave", 0) + "&contraseña=" + credenciales.getString("contraseña", "") + "&nombre=" + firstFile.getName() ).getBytes());
                            output_sream.flush();
                            output_sream.close();

                            BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                            String linea;
                            StringBuilder constructor_cadena = new StringBuilder();
                            while( (linea = bufer_lectura.readLine()) != null ){
                                constructor_cadena.append(linea).append("\n");
                            }

                            JSONObject json = new JSONObject( constructor_cadena.toString() );

                            if( json.optInt("eliminar") == 0 ){
                                // Manejar el caso en que el archivo no se pudo subir
                                System.err.println("Error: El archivo no pudo ser subido.");
                                return;
                            }

                            // Intenta eliminar el archivo
                            boolean deleted = firstFile.delete();
                            if (!deleted) {
                                // Manejar el caso en que el archivo no se pudo eliminar
                                System.err.println("Error: El archivo no pudo ser eliminado.");
                                return;
                            }

                            filesDir = contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                            if(filesDir == null || !filesDir.isDirectory()){
                                procesosEdit.putBoolean("subiendo_fotos", false);
                                procesosEdit.apply();
                                //System.out.println("Ya no hay ARCHIVOOOS");
                                return;
                            }
                            files = filesDir.listFiles();
                        }

                        procesosEdit.putBoolean("subiendo_fotos", false);
                        procesosEdit.apply();
                        //System.out.println("Todas las fotos SUBIDAS . . . .");
                    }else{
                        //System.out.println("Las fotos ya estan siendo subidas. . . ");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
