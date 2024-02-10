package com.example.test4;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    public EscuchadorClickPedido escuchadorClickPedido;
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
        holder.comprobante.setText( pedido.comprobante == 1 ? "FACTURA" : pedido.comprobante == 2 ? "RECIBO" : pedido.comprobante == 3 ? "PREVENTA" : "SIN TIPO" );
        holder.folio.setText(pedido.folio.toString());
        holder.cliente.setText(pedido.cliente_clave.toString() + " " + pedido.cliente_nombre);
        holder.vendedor.setText(pedido.vendedor.toString());
        holder.codigos.setText(pedido.codigos.toString());
        holder.piezas.setText(pedido.piezas.toString());
        holder.total.setText(pedido.total.toString());
        holder.barra.setImageBitmap(pedido.bitmapBarra);
        holder.barra.setVisibility(pedido.visibilidad);
        holder.pgrBarra.setVisibility(pedido.visibilidadPgr);

        holder.btnEntregarPedido.setVisibility( pedido.entregable && pedido.visibilidad == View.VISIBLE && pedido.bitmapFoto != null ? View.VISIBLE : View.GONE );
        holder.btnFotografiarPedido.setVisibility( pedido.entregable && pedido.visibilidad == View.VISIBLE ? View.VISIBLE : View.GONE );

        holder.foto.setImageBitmap(pedido.bitmapFoto);
        holder.foto.setVisibility( pedido.entregable && pedido.visibilidad == View.VISIBLE && pedido.bitmapFoto != null ? View.VISIBLE : View.GONE );

        if(pedido.entregable){
            holder.btnFotografiarPedido.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    escuchadorClickPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            });
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    /*holder.barra.setVisibility( holder.barra.getVisibility() == View.GONE ? View.VISIBLE : View.GONE );
                    System.out.println(holder.getAdapterPosition());*/

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
                                            pedido.visibilidadPgr = View.GONE;
                                            pedido.visibilidad = View.VISIBLE;
                                            notifyDataSetChanged();
                                        }
                                    });
                                }
                            });
                        }else{
                            pedido.visibilidad = View.VISIBLE;
                        }
                    }else{
                        pedido.visibilidad = View.GONE;
                    }
                    notifyDataSetChanged();
            }
        });
    }

    public void ColocarEscuchadorClickPedido(EscuchadorClickPedido escuchadorClickPedido){
        this.escuchadorClickPedido = escuchadorClickPedido;
    }

    public interface EscuchadorClickPedido{
        void pedidoClickeado(int indice, Pedido pedido);
    }

    @Override
    public int getItemCount() {
        return pedidosFiltrados.size();
    }



    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView fecha, comprobante, folio, cliente, vendedor, codigos, piezas, total;
        ImageView barra, foto;

        ProgressBar pgrBarra;

        Button btnEntregarPedido, btnFotografiarPedido;

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
            btnEntregarPedido = itemView.findViewById(R.id.btnEntregarPedido);
            btnFotografiarPedido = itemView.findViewById(R.id.btnFotografiarPedido);
        }
    }
}
