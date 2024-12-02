package com.example.test4;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdaptadorUsuarios extends RecyclerView.Adapter<AdaptadorUsuarios.ViewHolder>{
//
    private List<Usuario> usuarios;
    private List<Usuario> usuariosFiltrados;
    private OnClickListener onClickListener;

    public AdaptadorUsuarios(List<Usuario> usuarios){
        this.usuarios = usuarios;
        usuariosFiltrados = new ArrayList<>(usuarios);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Usuario usuario = usuariosFiltrados.get(position);
        holder.textIdItemLista.setText(usuario.id.toString());
        holder.textNombreItemLista.setText(usuario.nombre);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    onClickListener.onClick(holder.getAdapterPosition(), usuario);
                }
            }
        });
    }

    public void filtrar(String filtro){
        usuariosFiltrados.clear();
        if (filtro.isEmpty()) {
            usuariosFiltrados.addAll(usuarios);
        } else {
            for (Usuario usuario : usuarios) {
                if ( usuario.id.toString().toLowerCase().contains( filtro.toLowerCase() ) || usuario.nombre.toLowerCase().contains( filtro.toLowerCase() ) ) {
                    usuariosFiltrados.add(usuario);
                }
            }
        }
        System.out.println(usuariosFiltrados.size());
        notifyDataSetChanged();
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(int position, Usuario usuario);
    }

    @Override
    public int getItemCount() {
        return usuariosFiltrados.size();
    }

     static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textIdItemLista;
        TextView textNombreItemLista;

        public ViewHolder(View itemView) {
            super(itemView);
            textIdItemLista = itemView.findViewById(R.id.textIdItemLista);
            textNombreItemLista = itemView.findViewById(R.id.textNombreItemLista);
        }
    }

}
