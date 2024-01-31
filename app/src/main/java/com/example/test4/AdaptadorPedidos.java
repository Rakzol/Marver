package com.example.test4;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdaptadorPedidos extends RecyclerView.Adapter<AdaptadorPedidos.ViewHolder> {

    private List<Pedido> pedidos;
    private EscuchadorClickPedido escuchadorClickPedido;

    public AdaptadorPedidos(List<Pedido> pedidos){
        this.pedidos = pedidos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedidos, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pedido pedido = pedidos.get(position);
        holder.fecha.setText(pedido.fecha);
        holder.comprobante.setText( pedido.comprobante == 1 ? "FACTURA" : pedido.comprobante == 2 ? "RECIBO" : pedido.comprobante == 3 ? "PREVENTA" : "SIN TIPO" );
        holder.folio.setText(pedido.folio.toString());
        holder.cliente.setText(pedido.cliente_clave.toString() + " " + pedido.cliente_nombre);
        holder.vendedor.setText(pedido.vendedor.toString());
        holder.codigos.setText(pedido.codigos.toString());
        holder.piezas.setText(pedido.piezas.toString());
        holder.total.setText(pedido.total.toString());
        holder.barra.setImageBitmap(pedido.bitmap);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( escuchadorClickPedido != null ){
                    escuchadorClickPedido.pedidoClickeado( holder.getAdapterPosition(), pedido, holder );
                }
            }
        });
    }

    public void ColocarEscuchadorClickPedido(EscuchadorClickPedido escuchadorClickPedido){
        this.escuchadorClickPedido = escuchadorClickPedido;
    }

    public interface EscuchadorClickPedido{
        void pedidoClickeado(int indice, Pedido pedido, ViewHolder holder);
    }

    @Override
    public int getItemCount() {
        return pedidos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView fecha, comprobante, folio, cliente, vendedor, codigos, piezas, total;
        ImageView barra;

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
        }
    }
}
