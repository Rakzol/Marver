package com.example.test4;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class AdaptadorUsuarios extends RecyclerView.Adapter<AdaptadorUsuarios.ViewHolder>{

    private List<Usuario> usuarios;
    private OnClickListener onClickListener;

    public AdaptadorUsuarios(List<Usuario> usuarios){
        this.usuarios = usuarios;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.textViewId.setText(usuario.id.toString());
        holder.textViewNombre.setText(usuario.nombre);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    onClickListener.onClick(holder.getAdapterPosition(), usuario);
                }
            }
        });
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(int position, Usuario usuario);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

     static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewId;
        TextView textViewNombre;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewId = itemView.findViewById(R.id.idUsuario);
            textViewNombre = itemView.findViewById(R.id.nombreUsuario);
        }
    }

}
