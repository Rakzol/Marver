package com.example.test4;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AdaptadorPedidos extends RecyclerView.Adapter<AdaptadorPedidos.ViewHolder> {

    public List<Pedido> pedidos;
    public List<Pedido> pedidosFiltrados;
    public EscuchadorClickPedido escuchadorClickLocalizarPedido;
    public EscuchadorClickPedido escuchadorClickFotografiarPedido;
    public EscuchadorClickPedido escuchadorClickNotificarPedido;
    public EscuchadorClickPedido escuchadorClickEntregarPedido;
    public EscuchadorClickPedido escuchadorClickEliminarPedido;

    public FragmentActivity actividad;
    public AdaptadorPedidos(List<Pedido> pedidos, FragmentActivity actividad){
        this.pedidos = pedidos;
        pedidosFiltrados = new ArrayList<>(pedidos);
        this.actividad = actividad;
    }

    public void filtrar(String filtro){
        pedidosFiltrados.clear();
        if (filtro.isEmpty()) {
            pedidosFiltrados.addAll(pedidos);
        } else {
            for (Pedido pedido : pedidos) {
                if ( ( pedido.cliente_clave + " " + pedido.cliente_nombre ).toLowerCase().contains( filtro.toLowerCase() ) || ( pedido.folio.toString() ).toLowerCase().contains( filtro.toLowerCase() ) ) {
                    pedidosFiltrados.add(pedido);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedidos, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pedido pedido = pedidosFiltrados.get(position);
        holder.fecha.setText(pedido.fecha);
        holder.comprobante.setText( pedido.comprobante == 1 ? "FACTURA" : pedido.comprobante == 2 ? "RECIBO" : pedido.comprobante == 5 ? "PREVENTA" : "SIN TIPO" );
        holder.folio.setText(pedido.folio.toString());
        holder.cliente.setText(pedido.cliente_clave.toString() + " " + pedido.cliente_nombre);
        holder.vendedor.setText(pedido.vendedor.toString());
        holder.codigos.setText(pedido.codigos.toString());
        holder.piezas.setText(pedido.piezas.toString());
        holder.total.setText(pedido.total.toString());

        if(pedido.feria != null && !Double.isNaN(pedido.feria)){
            holder.feria.setText(pedido.feria.toString());
        }else{
            holder.feria.setVisibility(View.GONE);
            holder.lblFeria.setVisibility(View.GONE);
        }

        holder.barra.setImageBitmap(pedido.bitmapBarra);
        holder.foto.setImageBitmap(pedido.bitmapFoto);

        holder.barra.setVisibility(pedido.visibilidad == View.VISIBLE && pedido.bitmapBarra != null ? View.VISIBLE : View.GONE);
        holder.foto.setVisibility(pedido.visibilidad == View.VISIBLE && pedido.bitmapFoto != null ? View.VISIBLE : View.GONE );

        holder.pgrBarra.setVisibility(pedido.visibilidadPgr);

        holder.btnLocalizarPedido.setVisibility( pedido.visibilidad == View.VISIBLE ? View.VISIBLE : View.GONE );
        holder.btnNotificarPedido.setVisibility( pedido.visibilidad == View.VISIBLE ? View.VISIBLE : View.GONE );
        holder.btnEliminarPedido.setVisibility( pedido.eliminable && pedido.visibilidad == View.VISIBLE ? View.VISIBLE : View.GONE );
        holder.btnEntregarPedido.setVisibility( pedido.entregable && pedido.visibilidad == View.VISIBLE && pedido.bitmapFoto != null ? View.VISIBLE : View.GONE );
        holder.btnFotografiarPedido.setVisibility( pedido.entregable && pedido.visibilidad == View.VISIBLE ? View.VISIBLE : View.GONE );

        holder.btnLocalizarPedido.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickLocalizarPedido != null){
                    escuchadorClickLocalizarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.btnNotificarPedido.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickNotificarPedido != null){
                    escuchadorClickNotificarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.btnEliminarPedido.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickEliminarPedido != null){
                    escuchadorClickEliminarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        if(pedido.entregable){
            holder.btnFotografiarPedido.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(escuchadorClickFotografiarPedido != null){
                        escuchadorClickFotografiarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                    }
                }
            });

            holder.btnEntregarPedido.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(escuchadorClickEntregarPedido != null){
                        escuchadorClickEntregarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                    }
                }
            });
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( pedido.visibilidad == View.GONE ){

                    if( pedido.bitmapBarra == null ){
                        pedido.visibilidadPgr = View.VISIBLE;

                        Executors.newSingleThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Code128Writer writer = new Code128Writer();

                                WindowManager windowManager = (WindowManager) v.getContext().getSystemService(Context.WINDOW_SERVICE);
                                DisplayMetrics metrics = new DisplayMetrics();
                                windowManager.getDefaultDisplay().getMetrics(metrics);

                                BitMatrix bitMatrix = writer.encode( pedido.folio + "c" + pedido.comprobante, BarcodeFormat.CODE_128, metrics.widthPixels-50, (metrics.widthPixels-50)/2);

                                int width = bitMatrix.getWidth();
                                int height = bitMatrix.getHeight();
                                pedido.bitmapBarra = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

                                for (int x = 0; x < width; x++) {
                                    for (int y = 0; y < height; y++) {
                                        pedido.bitmapBarra.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                                    }
                                }

                                ((Aplicacion)actividad.getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(pedido.bitmapFoto == null){
                                            pedido.bitmapFoto = BitmapFactory.decodeFile(new File( actividad.getExternalFilesDir(Environment.DIRECTORY_PICTURES), pedido.folio + "c" + pedido.comprobante + ".jpg" ).getAbsolutePath() );
                                            if( pedido.bitmapFoto == null ){

                                                Executors.newSingleThreadExecutor().execute(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try{
                                                            URL url = new URL("https://www.marverrefacciones.mx/android/fotos/" + pedido.folio + "c" + pedido.comprobante + ".jpg");
                                                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                            conexion.setDoInput(true);
                                                            conexion.connect();
                                                            InputStream input = conexion.getInputStream();
                                                            pedido.bitmapFoto = BitmapFactory.decodeStream(input);
                                                            ((Aplicacion)actividad.getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    notifyDataSetChanged();
                                                                }
                                                            });
                                                        }catch (Exception e){
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                });

                                            }
                                        }

                                        pedido.visibilidadPgr = View.GONE;
                                        pedido.visibilidad = View.VISIBLE;
                                        notifyDataSetChanged();
                                    }
                                });
                            }
                        });
                    }else{
                        if(pedido.bitmapFoto == null){
                            pedido.bitmapFoto = BitmapFactory.decodeFile(new File( actividad.getExternalFilesDir(Environment.DIRECTORY_PICTURES), pedido.folio + "c" + pedido.comprobante + ".jpg" ).getAbsolutePath() );
                            if( pedido.bitmapFoto == null ){

                                Executors.newSingleThreadExecutor().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try{
                                            URL url = new URL("https://www.marverrefacciones.mx/android/fotos/" + pedido.folio + "c" + pedido.comprobante + ".jpg");
                                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                            conexion.setDoInput(true);
                                            conexion.connect();
                                            InputStream input = conexion.getInputStream();

                                            pedido.bitmapFoto = BitmapFactory.decodeStream(input);
                                            ((Aplicacion)actividad.getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    notifyDataSetChanged();
                                                }
                                            });
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            }
                        }
                        pedido.visibilidad = View.VISIBLE;
                    }
                }else{
                    pedido.visibilidad = View.GONE;
                }
                notifyDataSetChanged();
            }
        });
    }

    public void ColocarEscuchadorClickLocalizarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickLocalizarPedido = escuchadorClickPedido;
    }

    public void ColocarEscuchadorClickEliminarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickEliminarPedido = escuchadorClickPedido;
    }

    public void ColocarEscuchadorClickFotografiarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickFotografiarPedido = escuchadorClickPedido;
    }

    public void ColocarEscuchadorClickEntregarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickEntregarPedido = escuchadorClickPedido;
    }

    public void ColocarEscuchadorClickNotificarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickNotificarPedido = escuchadorClickPedido;
    }

    public interface EscuchadorClickPedido{
        void pedidoClickeado(int indice, Pedido pedido);
    }

    @Override
    public int getItemCount() {
        return pedidosFiltrados.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView fecha, comprobante, folio, cliente, vendedor, codigos, piezas, total, feria, lblFeria;
        ImageView barra, foto;

        ProgressBar pgrBarra;

        Button btnLocalizarPedido, btnEntregarPedido, btnFotografiarPedido, btnEliminarPedido, btnNotificarPedido;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fecha = itemView.findViewById(R.id.pedido_fecha);
            comprobante = itemView.findViewById(R.id.pedido_comprobante);
            folio = itemView.findViewById(R.id.pedido_folio);
            cliente = itemView.findViewById(R.id.pedido_cliente);
            vendedor = itemView.findViewById(R.id.pedido_vendedor);
            codigos = itemView.findViewById(R.id.pedido_codigos);
            piezas = itemView.findViewById(R.id.pedido_piezas);
            total = itemView.findViewById(R.id.pedido_total);
            barra = itemView.findViewById(R.id.pedido_barra);
            foto = itemView.findViewById(R.id.pedido_fotografia);
            pgrBarra = itemView.findViewById(R.id.pgrBarra);
            btnLocalizarPedido = itemView.findViewById(R.id.btnLocalizarPedido);
            btnNotificarPedido = itemView.findViewById(R.id.btnNotificarPedido);
            btnEliminarPedido = itemView.findViewById(R.id.btnEliminarPedido);
            btnEntregarPedido = itemView.findViewById(R.id.btnEntregarPedido);
            btnFotografiarPedido = itemView.findViewById(R.id.btnFotografiarPedido);
            feria = itemView.findViewById(R.id.pedido_feria);
            lblFeria = itemView.findViewById(R.id.pedido_etiqueta_feria);
        }
    }
}
