package com.example.test4;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AdaptadorUsuarios extends RecyclerView.Adapter<AdaptadorUsuarios.ViewHolder>{

    private String[] datos;

    // Constructor que recibe los datos
    public AdaptadorUsuarios(String[] datos) {
        this.datos = datos;
    }

    // Crear nuevas vistas (invocadas por el layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Crear una nueva vista
        return new ViewHolder(null);
    }

    // Reemplazar el contenido de una vista (invocada por el layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - obtener el elemento del dataset en esta posición
        // - reemplazar el contenido de la vista con ese elemento

    }

    // Devolver el tamaño del dataset (invocado por el layout manager)
    @Override
    public int getItemCount() {
        return datos.length;
    }

    // Clase ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        // Cada elemento de datos es solo una cadena en este caso

    }
}
