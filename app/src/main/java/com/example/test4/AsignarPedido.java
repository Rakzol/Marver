package com.example.test4;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.test4.databinding.AsignarPedidoBinding;
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

public class AsignarPedido extends AppCompatActivity {

    AsignarPedidoBinding asignarPedido;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        asignarPedido = AsignarPedidoBinding.inflate(getLayoutInflater());
        setContentView(asignarPedido.getRoot());

        asignarPedido.btnRegresarAsigarPedido.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try{
                    URL url = new URL("https://www.marverrefacciones.mx/android/asignar_pedido");
                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                    conexion.setRequestMethod("POST");
                    conexion.setDoOutput(true);

                    SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

                    OutputStream output_sream = conexion.getOutputStream();
                    output_sream.write(( "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + getIntent().getExtras().getString("folio") ).getBytes());
                    output_sream.flush();
                    output_sream.close();

                    BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                    String linea;
                    StringBuilder constructor_cadena = new StringBuilder();
                    while( (linea = bufer_lectura.readLine()) != null ){
                        constructor_cadena.append(linea).append("\n");
                    }

                    JSONObject json_resultado = new JSONObject( constructor_cadena.toString() );

                    ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if( json_resultado.getInt("status") != 0 ){
                                    asignarPedido.imgResultadoAsignarPedido.setImageResource(R.drawable.error);
                                }

                                asignarPedido.txtResultadoPedido.setText( json_resultado.getString("mensaje") );

                                asignarPedido.prgAsignarPedido.setVisibility( View.GONE );
                                asignarPedido.contenedorAsignarPedido.setVisibility( View.VISIBLE );
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
