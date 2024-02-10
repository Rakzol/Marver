package com.example.test4;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

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

public class Pedidos extends Fragment implements fragmentoBuscador {

    public static String PENDIENTES = "pendientes";
    public static String FINALIZADOS = "finalizados";
    public static String EN_RUTA = "en_ruta";

    private ActivityResultLauncher<Intent> lanzadorActividadResultado;

    public Boolean entregable;

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

        lanzadorActividadResultado = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult resultado) {
                        if( resultado.getResultCode() == Activity.RESULT_OK ){
                            String ruta = resultado.getData().getStringExtra("ruta");
                            Toast.makeText(view.getContext(), ruta, Toast.LENGTH_LONG).show();
                            //pedido.bitmapFoto = BitmapFactory.decodeFile(ruta);
                            adaptadorPedidos.notifyDataSetChanged();
                        }else{
                            Toast.makeText( getContext(), String.valueOf(resultado.getResultCode()), Toast.LENGTH_LONG).show();
                        }
                    }
                });

        ((TextView)view.findViewById(R.id.txtPedidosInformacion)).setText( "Cargando " + ((Toolbar)requireActivity().findViewById(R.id.barra_herramientas_superior_mapa)).getTitle() );

        entregable = getArguments().getBoolean("entregable");

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
                                    adaptadorPedidos.ColocarEscuchadorClickPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                        @Override
                                        public void pedidoClickeado(int indice, Pedido pedido) {
                                            /*holder.barra.setVisibility( holder.barra.getVisibility() == View.GONE ? View.VISIBLE : View.GONE );*/
                                        /*pedido.visibilidad = pedido.visibilidad == View.GONE ? View.VISIBLE : View.GONE;
                                        adaptadorPedidos.notifyDataSetChanged();*/

                                            Intent intent = new Intent( getContext(), Fotografiar.class);
                                            intent.putExtra("folio", pedido.folio);
                                            //v.getContext().startActivity(intent);
                                            lanzadorActividadResultado.launch(intent);

                                        }
                                    });
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
            System.out.println("Filtrando. . .");
            adaptadorPedidos.filtrar(newText);
        }
    }
}
