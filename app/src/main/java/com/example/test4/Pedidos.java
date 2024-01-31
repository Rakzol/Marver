package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.test4.databinding.PedidosBinding;
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

public class Pedidos extends AppCompatActivity {

    PedidosBinding pedidos;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pedidos = PedidosBinding.inflate(getLayoutInflater());
        setContentView(pedidos.getRoot());

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try{
                    URL url = new URL("https://www.marverrefacciones.mx/android/pedidos");
                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                    conexion.setRequestMethod("POST");
                    conexion.setDoOutput(true);

                    SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

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

                        Code128Writer writer = new Code128Writer();

                        WindowManager windowManager = (WindowManager) Pedidos.this.getSystemService(Pedidos.this.WINDOW_SERVICE);
                        DisplayMetrics metrics = new DisplayMetrics();
                        windowManager.getDefaultDisplay().getMetrics(metrics);

                        BitMatrix bitMatrix = writer.encode( json_pedido.getInt("folio") + "c" + json_pedido.getInt("comprobante"), BarcodeFormat.CODE_128, metrics.widthPixels-50, (metrics.widthPixels-50)/2);

                        int width = bitMatrix.getWidth();
                        int height = bitMatrix.getHeight();
                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

                        for (int x = 0; x < width; x++) {
                            for (int y = 0; y < height; y++) {
                                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                            }
                        }

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
                                bitmap
                            ) );
                    }

                    ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(Pedidos.this);
                                pedidos.listaPedidos.setLayoutManager(linearLayoutManager);
                                AdaptadorPedidos adaptadorPedidos = new AdaptadorPedidos(lista_pedidos);
                                adaptadorPedidos.ColocarEscuchadorClickPedido(new AdaptadorPedidos.EscuchadorClickPedido() {
                                    @Override
                                    public void pedidoClickeado(int indice, Pedido pedido, AdaptadorPedidos.ViewHolder holder) {
                                        holder.barra.setVisibility( holder.barra.getVisibility() == View.GONE ? View.VISIBLE : View.GONE );
                                        adaptadorPedidos.notifyDataSetChanged();
                                    }
                                });
                                pedidos.listaPedidos.setAdapter(adaptadorPedidos);
                                pedidos.pgrPedidos.setVisibility( View.GONE );
                                pedidos.listaPedidos.setVisibility( View.VISIBLE );
                                adaptadorPedidos.notifyDataSetChanged();
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
