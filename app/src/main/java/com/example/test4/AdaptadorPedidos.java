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
        holder.textFechaItemPedidos.setText(pedido.fecha);
        holder.textTipoComprobanteItemPedidos.setText( pedido.tipoComprobante == 1 ? "FACTURA" : pedido.tipoComprobante == 2 ? "RECIBO" : pedido.tipoComprobante == 5 ? "PREVENTA" : "ESPECIAL" );
        holder.textFolioComprobanteItemPedidos.setText(pedido.folioComprobante);
        holder.textPedidoItemPedidos.setText(pedido.pedido);
        holder.textClienteItemPedidos.setText(pedido.clienteClave + " " + pedido.clienteNombre);
        holder.textRepartidorItemPedidos.setText(pedido.repartidor);
        holder.textCodigosItemPedidos.setText(pedido.codigos);
        holder.textPiezasItemPedidos.setText(pedido.piezas);
        holder.textTotalItemPedidos.setText(pedido.total.toString());
        if(pedido.feria != null && !Double.isNaN(pedido.feria)){
            holder.textFeriaItemPedidos.setText(pedido.feria.toString());
        }else{
            holder.textFeriaItemPedidos.setVisibility(View.GONE);
            holder.textEtiquetaFeriaItemPedidos.setVisibility(View.GONE);
        }
        holder.textObservacionesItemPedidos.setText(pedido.observacionesPedido);

        holder.imageBarrasItemPedidos.setImageBitmap(pedido.bitmapBarra);
        holder.imageFotografiaItemPedidos.setImageBitmap(pedido.bitmapFoto);

        holder.imageBarrasItemPedidos.setVisibility(pedido.visibilidad == View.VISIBLE && pedido.bitmapBarra != null ? View.VISIBLE : View.GONE);
        holder.imageFotografiaItemPedidos.setVisibility(pedido.visibilidad == View.VISIBLE && pedido.bitmapFoto != null ? View.VISIBLE : View.GONE );

        holder.progressBarraItemPedidos.setVisibility(pedido.visibilidadPgr);

        if(pedido.folioComprobante > 0){
            holder.layoutDatosNormalesItemPedidos.setVisibility(View.VISIBLE);
            holder.textObservacionesItemPedidos.setVisibility(pedido.visibilidad);
        }else{
            holder.textObservacionesItemPedidos.setVisibility(View.VISIBLE);
        }

        if(pedido.tipoPedido == Pedidos.PENDIENTES){
            holder.buttonEliminarItemPedidos.setVisibility(View.VISIBLE);
        }

        if(pedido.tipoPedido == Pedidos.EN_RUTA){
            holder.buttonFotografiarItemPedidos.setVisibility(pedido.visibilidad);

            if(pedido.bitmapFoto != null){
                holder.buttonEntregarItemPedidos.setVisibility(pedido.visibilidad);
                holder.viewUnoItemPedidos.setVisibility(pedido.visibilidad);
                holder.buttonNoEntregarItemPedidos.setVisibility(pedido.visibilidad);
                holder.buttonRechazarItemPedidos.setVisibility(pedido.visibilidad);
            }
        }

        if(pedido.tipoPedido == Pedidos.ENTREGADOS || pedido.tipoPedido == Pedidos.NO_ENTREGADOS || pedido.tipoPedido == Pedidos.RECHAZADOS){
            if(pedido.folioComprobante == 0){
                holder.buttonFinalizarItemPedidos.setVisibility(pedido.visibilidad);
            }

            if(pedido.tipoPedido == Pedidos.ENTREGADOS){
                holder.buttonNotificarItemPedidos.setVisibility(pedido.visibilidad);
            }
        }

        holder.buttonNotificarItemPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickNotificarPedido != null){
                    escuchadorClickNotificarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.buttonEliminarItemPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickEliminarPedido != null){
                    escuchadorClickEliminarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.buttonFotografiarItemPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickFotografiarPedido != null){
                    escuchadorClickFotografiarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.buttonEntregarItemPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickEntregarPedido != null){
                    escuchadorClickEntregarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.buttonNoEntregarItemPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickNoEntregarPedido != null){
                    escuchadorClickNoEntregarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.buttonFinalizarItemPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickFinalizarPedido != null){
                    escuchadorClickFinalizarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
                }
            }
        });

        holder.buttonRechazarItemPedidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(escuchadorClickRechazarPedido != null){
                    escuchadorClickRechazarPedido.pedidoClickeado( holder.getAdapterPosition(), pedido );
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

    public void ColocarEscuchadorClickFinalizarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickFinalizarPedido = escuchadorClickPedido;
    }

    public void ColocarEscuchadorClickRechazarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickRechazarPedido = escuchadorClickPedido;
    }

    public void ColocarEscuchadorClickNoEntregarPedido(EscuchadorClickPedido escuchadorClickPedido){
        escuchadorClickNoEntregarPedido = escuchadorClickPedido;
    }

    public interface EscuchadorClickPedido{
        void pedidoClickeado(int indice, Pedido pedido);
    }

    @Override
    public int getItemCount() {
        return pedidosFiltrados.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView textFechaItemPedidos, textTipoComprobanteItemPedidos, textFolioComprobanteItemPedidos, textPedidoItemPedidos, textClienteItemPedidos, textRepartidorItemPedidos, textCodigosItemPedidos, textPiezasItemPedidos, textTotalItemPedidos, textFeriaItemPedidos, textEtiquetaFeriaItemPedidos, textObservacionesItemPedidos;
        ImageView imageBarrasItemPedidos, imageFotografiaItemPedidos;

        ProgressBar progressBarraItemPedidos;

        Button buttonEliminarItemPedidos, buttonFotografiarItemPedidos, buttonFinalizarItemPedidos, buttonEntregarItemPedidos, buttonNoEntregarItemPedidos, buttonRechazarItemPedidos, buttonNotificarItemPedidos;

        View viewUnoItemPedidos;

        LinearLayout layoutDatosNormalesItemPedidos;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFechaItemPedidos = itemView.findViewById(R.id.textFechaItemPedidos);
            textTipoComprobanteItemPedidos = itemView.findViewById(R.id.textTipoComprobanteItemPedidos);
            textFolioComprobanteItemPedidos = itemView.findViewById(R.id.textFolioComprobanteItemPedidos);
            textPedidoItemPedidos = itemView.findViewById(R.id.textPedidoItemPedidos);
            textClienteItemPedidos = itemView.findViewById(R.id.textClienteItemPedidos);
            textRepartidorItemPedidos = itemView.findViewById(R.id.textRepartidorItemPedidos);
            textCodigosItemPedidos = itemView.findViewById(R.id.textCodigosItemPedidos);
            textPiezasItemPedidos = itemView.findViewById(R.id.textPiezasItemPedidos);
            textTotalItemPedidos = itemView.findViewById(R.id.textTotalItemPedidos);
            textFeriaItemPedidos = itemView.findViewById(R.id.textFeriaItemPedidos);
            textEtiquetaFeriaItemPedidos = itemView.findViewById(R.id.textEtiquetaFeriaItemPedidos);
            textObservacionesItemPedidos = itemView.findViewById(R.id.textObservacionesItemPedidos);
            imageBarrasItemPedidos = itemView.findViewById(R.id.imageBarrasItemPedidos);
            imageFotografiaItemPedidos = itemView.findViewById(R.id.imageFotografiaItemPedidos);
            progressBarraItemPedidos = itemView.findViewById(R.id.progressBarraItemPedidos);
            buttonEliminarItemPedidos = itemView.findViewById(R.id.buttonEliminarItemPedidos);
            buttonFotografiarItemPedidos = itemView.findViewById(R.id.buttonFotografiarItemPedidos);
            buttonFinalizarItemPedidos = itemView.findViewById(R.id.buttonFinalizarItemPedidos);
            buttonEntregarItemPedidos = itemView.findViewById(R.id.buttonEntregarItemPedidos);
            buttonNoEntregarItemPedidos = itemView.findViewById(R.id.buttonNoEntregarItemPedidos);
            viewUnoItemPedidos = itemView.findViewById(R.id.viewUnoItemPedidos);
            buttonRechazarItemPedidos = itemView.findViewById(R.id.buttonRechazarItemPedidos);
            buttonNotificarItemPedidos = itemView.findViewById(R.id.buttonNotificarItemPedidos);
            layoutDatosNormalesItemPedidos = itemView.findViewById(R.id.layoutDatosNormalesItemPedidos);
        }
    }
}
