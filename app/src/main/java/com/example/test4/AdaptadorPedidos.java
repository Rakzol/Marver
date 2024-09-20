package com.example.test4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AdaptadorPedidos extends RecyclerView.Adapter<AdaptadorPedidos.ViewHolder> {

    public List<Pedido> pedidos;
    public List<Pedido> pedidosFiltrados;
    public EscuchadorClickPedido escuchadorClickFotografiarPedido;
    public EscuchadorClickPedido escuchadorClickNotificarPedido;
    public EscuchadorClickPedido escuchadorClickEntregarPedido;
    public EscuchadorClickPedido escuchadorClickEliminarPedido;
    public EscuchadorClickPedido escuchadorClickFinalizarPedido;
    public EscuchadorClickPedido escuchadorClickRechazarPedido;
    public EscuchadorClickPedido escuchadorClickNoEntregarPedido;

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
                if ( ( pedido.clienteClave + " " + pedido.clienteNombre + " " + pedido.pedido + " " + pedido.folioComprobante ).toLowerCase().contains( filtro.toLowerCase() ) ) {
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
        holder.tipoComprobante.setText( pedido.tipoComprobante == 1 ? "FACTURA" : pedido.tipoComprobante == 2 ? "RECIBO" : pedido.tipoComprobante == 5 ? "PREVENTA" : "ESPECIAL" );
        holder.folioComprobante.setText(pedido.folioComprobante);
        holder.pedido.setText(pedido.pedido);
        holder.cliente.setText(pedido.clienteClave + " " + pedido.clienteNombre);
        holder.repartidor.setText(pedido.repartidor);
        holder.codigos.setText(pedido.codigos);
        holder.piezas.setText(pedido.piezas);
        holder.total.setText(pedido.total.toString());
        if(pedido.feria != null && !Double.isNaN(pedido.feria)){
            holder.feria.setText(pedido.feria.toString());
        }else{
            holder.feria.setVisibility(View.GONE);
            holder.lblFeria.setVisibility(View.GONE);
        }
        holder.observaciones.setText(pedido.observacionesPedido);

        holder.observaciones.setVisibility(pedido.visibilidad);

        holder.barra.setImageBitmap(pedido.bitmapBarra);
        holder.foto.setImageBitmap(pedido.bitmapFoto);

        holder.barra.setVisibility(pedido.visibilidad == View.VISIBLE && pedido.bitmapBarra != null ? View.VISIBLE : View.GONE);
        holder.foto.setVisibility(pedido.visibilidad == View.VISIBLE && pedido.bitmapFoto != null ? View.VISIBLE : View.GONE );

        holder.pgrBarra.setVisibility(pedido.visibilidadPgr);

        if(pedido.folioComprobante > 0){
            holder.layoutDatosNormales.setVisibility(View.VISIBLE);
            holder.observaciones.setVisibility(pedido.visibilidad);
        }else{
            holder.observaciones.setVisibility(View.VISIBLE);
        }

        if(pedido.tipoPedido == Pedidos.PENDIENTES){
            holder.btnEliminarPedido.setVisibility(View.VISIBLE);
        }

        if(pedido.tipoPedido == Pedidos.EN_RUTA){
            holder.btnFotografiarPedido.setVisibility(pedido.visibilidad);

            if(pedido.bitmapFoto != null){
                holder.btnEntregarPedido.setVisibility(pedido.visibilidad);
                holder.viewPedidosUno.setVisibility(pedido.visibilidad);
                holder.btnNoEntregarPedido.setVisibility(pedido.visibilidad);
                holder.btnRechazarPedido.setVisibility(pedido.visibilidad);
                holder.viewPedidosDos.setVisibility(pedido.visibilidad);
                holder.btnNotificarPedido.setVisibility(pedido.visibilidad);
            }
        }

        if(pedido.tipoPedido == Pedidos.ENTREGADOS || pedido.tipoPedido == Pedidos.NO_ENTREGADOS || pedido.tipoPedido == Pedidos.RECHAZADOS){
            if(pedido.folioComprobante == 0){
                holder.btnFinalizarPedido.setVisibility(pedido.visibilidad);
            }

            holder.btnNotificarPedido.setVisibility(pedido.visibilidad);
        }

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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( pedido.visibilidad == View.GONE ){
                    pedido.visibilidad = View.VISIBLE;

                    if( pedido.bitmapBarra == null ){
                        pedido.visibilidadPgr = View.VISIBLE;

                        Executors.newSingleThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Code128Writer writer = new Code128Writer();

                                WindowManager windowManager = (WindowManager) v.getContext().getSystemService(Context.WINDOW_SERVICE);
                                DisplayMetrics metrics = new DisplayMetrics();
                                windowManager.getDefaultDisplay().getMetrics(metrics);

                                BitMatrix bitMatrix = writer.encode( pedido.folioComprobante + "c" + pedido.tipoComprobante, BarcodeFormat.CODE_128, metrics.widthPixels-50, (metrics.widthPixels-50)/2);

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
                                        notifyDataSetChanged();
                                    }
                                });
                            }
                        });
                    }

                    if(pedido.bitmapFoto == null){
                        pedido.bitmapFoto = BitmapFactory.decodeFile(new File( actividad.getExternalFilesDir(Environment.DIRECTORY_PICTURES), pedido.pedido + ".jpg" ).getAbsolutePath() );
                        if( pedido.bitmapFoto == null ){

                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    try{

                                        ConnectivityManager connectivityManager = (ConnectivityManager)  v.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                                        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

                                        if( activeNetwork == null ){
                                            //System.out.println("Ya no hay conexion para actualizar");
                                            return;
                                        }
                                        if(!activeNetwork.isConnectedOrConnecting()){
                                            //System.out.println("Ya no hay conexion para actualizar");
                                            return;
                                        }
                                        if ( activeNetwork.getType() != ConnectivityManager.TYPE_WIFI ) {
                                            //System.out.println("Ya no hay wifi para actualizar");
                                            return;
                                        }

                                        URL url = new URL("https://www.marverrefacciones.mx/android/fotos/" + pedido.pedido + ".jpg");
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

                }else{
                    pedido.visibilidad = View.GONE;
                }
                notifyDataSetChanged();
            }
        });
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
        TextView fecha, tipoComprobante, folioComprobante, pedido, cliente, repartidor, codigos, piezas, total, feria, lblFeria, observaciones;
        ImageView barra, foto;

        ProgressBar pgrBarra;

        Button btnEntregarPedido, btnFotografiarPedido, btnEliminarPedido, btnNotificarPedido, btnNoEntregarPedido, btnFinalizarPedido, btnRechazarPedido;

        View viewPedidosUno, viewPedidosDos;

        LinearLayout layoutDatosNormales;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fecha = itemView.findViewById(R.id.pedido_fecha);
            tipoComprobante = itemView.findViewById(R.id.pedidoTipoComprobante);
            folioComprobante = itemView.findViewById(R.id.pedidoFolioComprobante);
            pedido = itemView.findViewById(R.id.pedidoPedido);
            cliente = itemView.findViewById(R.id.pedido_cliente);
            repartidor = itemView.findViewById(R.id.pedido_repartidor);
            codigos = itemView.findViewById(R.id.pedido_codigos);
            piezas = itemView.findViewById(R.id.pedido_piezas);
            total = itemView.findViewById(R.id.pedido_total);
            feria = itemView.findViewById(R.id.pedido_feria);
            lblFeria = itemView.findViewById(R.id.pedido_etiqueta_feria);
            observaciones = itemView.findViewById(R.id.pedidoObservaciones);
            barra = itemView.findViewById(R.id.pedido_barra);
            foto = itemView.findViewById(R.id.pedido_fotografia);
            pgrBarra = itemView.findViewById(R.id.pgrBarra);
            btnEliminarPedido = itemView.findViewById(R.id.btnEliminarPedido);
            btnFotografiarPedido = itemView.findViewById(R.id.btnFotografiarPedido);
            btnFinalizarPedido = itemView.findViewById(R.id.btnFinalizarPedido);
            btnEntregarPedido = itemView.findViewById(R.id.btnEntregarPedido);
            btnNoEntregarPedido = itemView.findViewById(R.id.btnNoEntregarPedido);
            viewPedidosUno = itemView.findViewById(R.id.viewPedidosUno);
            viewPedidosDos = itemView.findViewById(R.id.viewPedidosDos);
            btnRechazarPedido = itemView.findViewById(R.id.btnRechazarPedido);
            btnNotificarPedido = itemView.findViewById(R.id.btnNotificarPedido);
            layoutDatosNormales = itemView.findViewById(R.id.pedidoLayoutDatosNormales);
        }
    }
}
