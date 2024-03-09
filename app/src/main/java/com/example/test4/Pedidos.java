package com.example.test4;

import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
import java.util.List;
import java.util.concurrent.Executors;

public class Pedidos extends Fragment implements fragmentoBuscador {

    public static String PENDIENTES = "pendientes";
    public static String EN_RUTA = "en_ruta";
    public static String ENTREGADOS = "entregados";
    public static String FINALIZADOS = "finalizados";

    private ActivityResultLauncher<Intent> lanzadorActividadResultado;

    public Boolean entregable;

    public Pedido pedido_seleccionado;
    private AdaptadorPedidos adaptadorPedidos;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static Pedidos NuevoPedido( String tipo_pedido, Boolean entregable ){
        Pedidos fragmento = new Pedidos();
        Bundle argumentos = new Bundle();
        argumentos.putString("tipo_pedido", tipo_pedido);
        argumentos.putBoolean("entregable", entregable);
        fragmento.setArguments(argumentos);

        return fragmento;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.pedidos, container, false);

        entregable = getArguments().getBoolean("entregable");

        if(entregable){
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
        }

        ((TextView)view.findViewById(R.id.txtPedidosInformacion)).setText( "Cargando " + ((Toolbar)requireActivity().findViewById(R.id.barra_herramientas_superior_mapa)).getTitle() );

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try{
                    URL url = new URL("https://www.marverrefacciones.mx/android/pedidos_" + getArguments().getString("tipo_pedido"));
                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                    conexion.setRequestMethod("POST");
                    conexion.setDoOutput(true);

                    SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                    OutputStream output_sream = conexion.getOutputStream();
                    output_sream.write(( "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") ).getBytes());
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
                                entregable
                        ) );

                        /*float c_temp = c;
                        ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView)view.findViewById(R.id.txtPedidosCarga)).setText( ( ( c_temp + 1f ) / json_pedidos.length() ) * 100f + " %" );
                            }
                        });*/
                    }

                    ((Aplicacion)requireActivity().getApplication()).controlador_hilo_princpal.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if( lista_pedidos.size() > 0 ){
                                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                                    ((RecyclerView)view.findViewById(R.id.listaPedidos)).setLayoutManager(linearLayoutManager);
                                    adaptadorPedidos = new AdaptadorPedidos(lista_pedidos, requireActivity());

                                    adaptadorPedidos.ColocarEscuchadorClickLocalizarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {

                                            Mapa mapa = Mapa.NuevoMapa(String.valueOf(pedido.cliente_clave), pedido.cliente_nombre);

                                            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                                            transaction.replace(R.id.contenedor_fragmentos, mapa);
                                            transaction.commit();
                                        }
                                    });

                                    if(entregable){

                                        adaptadorPedidos.ColocarEscuchadorClickFotografiarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                            @Override
                                            public void pedidoClickeado(int indice, Pedido pedido) {
                                                pedido_seleccionado = pedido;
                                                Intent intent = new Intent( getContext(), Fotografiar.class);
                                                intent.putExtra("folio", pedido.folio);
                                                intent.putExtra("comprobante", pedido.comprobante);
                                                lanzadorActividadResultado.launch(intent);
                                            }
                                        });

                                        adaptadorPedidos.ColocarEscuchadorClickEntregarPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                            @Override
                                            public void pedidoClickeado(int indice, Pedido pedido) {

                                                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                                View dialogView = getLayoutInflater().inflate(R.layout.dialogo_procesar_pedido, null);

                                                builder.setView(dialogView);

                                                ((TextView) dialogView.findViewById(R.id.txtResultadoPedido)).setText( "Entregando pedido. . ." );

                                                AlertDialog alertDialog = builder.create();
                                                alertDialog.show();

                                                ((Button) dialogView.findViewById(R.id.btnRegresarAsigarPedido)).setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        alertDialog.dismiss();
                                                    }
                                                });

                                                Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try{
                                                            URL url = new URL("https://www.marverrefacciones.mx/android/entregar_pedido");
                                                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                            conexion.setRequestMethod("POST");
                                                            conexion.setDoOutput(true);

                                                            SharedPreferences preferencias_compartidas = requireContext().getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                                                            OutputStream output_sream = conexion.getOutputStream();
                                                            output_sream.write(( "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + pedido.folio + "&comprobante=" + pedido.comprobante ).getBytes());
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
                                                                    try {

                                                                        ((ProgressBar) dialogView.findViewById(R.id.prgAsignarPedido)).setVisibility( View.GONE );
                                                                        if( json_resultado.getInt("status") != 0 ){
                                                                            ((ImageView) dialogView.findViewById(R.id.imgResultadoAsignarPedido)).setImageResource(R.drawable.error);
                                                                        }else{
                                                                            subir_fotos(requireContext());
                                                                            ((BottomNavigationView)requireActivity().findViewById(R.id.barra_vista_navegacion_inferior)).setSelectedItemId(R.id.nav_inferior_entregados);
                                                                        }
                                                                        ((ImageView) dialogView.findViewById(R.id.imgResultadoAsignarPedido)).setVisibility(View.VISIBLE);

                                                                        ((TextView) dialogView.findViewById(R.id.txtResultadoPedido)).setText( json_resultado.getString("mensaje") );

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
                                        });
                                    }
                                    ((RecyclerView)view.findViewById(R.id.listaPedidos)).setAdapter(adaptadorPedidos);
                                    view.findViewById(R.id.txtPedidosInformacion).setVisibility( View.GONE );
                                    //view.findViewById(R.id.txtPedidosCarga).setVisibility( View.GONE );
                                    view.findViewById(R.id.listaPedidos).setVisibility( View.VISIBLE );
                                    adaptadorPedidos.notifyDataSetChanged();
                                }else{
                                    ((TextView)view.findViewById(R.id.txtPedidosInformacion)).setText( "No hay " + ((Toolbar)requireActivity().findViewById(R.id.barra_herramientas_superior_mapa)).getTitle() );
                                }
                                view.findViewById(R.id.pgrPedidos).setVisibility( View.GONE );
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

        return view;
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
                    SharedPreferences credenciales = contexto.getSharedPreferences("credenciales", Context.MODE_PRIVATE);

                    if( !procesos.getBoolean("subiendo_fotos", false) ){
                        System.out.println("Subiendo fotos. . . . .");
                        procesos.edit().putBoolean("subiendo_fotos", true).apply();

                        File filesDir = contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                        if(filesDir == null || !filesDir.isDirectory()){
                            procesos.edit().putBoolean("subiendo_fotos", false).apply();
                            System.out.println("Ya no hay ARCHIVOOOS");
                            return;
                        }

                        File[] files = filesDir.listFiles();

                        while( files != null && files.length > 0 ){

                            if( credenciales.getString("usuario", "") == "" ){
                                procesos.edit().putBoolean("subiendo_fotos", false).apply();
                                System.out.println("Ya no hay USUARIOOOO");
                                break;
                            }

                            ConnectivityManager connectivityManager = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

                            if (isConnected) {
                                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                                    System.out.println("WIFI");

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
                                    output_sream.write( ( "foto=" + Base64.encodeToString(bytes, Base64.NO_WRAP) + "&usuario=" + credenciales.getString("usuario", "") + "&contraseña=" + credenciales.getString("contraseña", "") + "&nombre=" + firstFile.getName() ).getBytes());
                                    output_sream.flush();
                                    output_sream.close();

                                    BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                                    String linea;
                                    StringBuilder constructor_cadena = new StringBuilder();
                                    while( (linea = bufer_lectura.readLine()) != null ){
                                        constructor_cadena.append(linea).append("\n");
                                    }

                                    JSONObject json = new JSONObject( constructor_cadena.toString() );

                                    if( json.getInt("eliminar") != 0 ){
                                        // Intenta eliminar el archivo
                                        boolean deleted = firstFile.delete();
                                        if (!deleted) {
                                            // Manejar el caso en que el archivo no se pudo eliminar
                                            System.err.println("Error: El archivo no pudo ser eliminado.");
                                        }

                                    }

                                } else {
                                    procesos.edit().putBoolean("subiendo_fotos", false).apply();
                                    System.out.println("Ya no hay WIFIIII");
                                    break;
                                }
                            }else{
                                procesos.edit().putBoolean("subiendo_fotos", false).apply();
                                System.out.println("Ya no hay CONEXIOOON");
                                break;
                            }

                            filesDir = contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                            files = filesDir.listFiles();
                        }

                        procesos.edit().putBoolean("subiendo_fotos", false).apply();
                        System.out.println("Todas las fotos SUBIDAS . . . .");
                    }else{
                        System.out.println("Las fotos ya estan siendo subidas. . . ");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
