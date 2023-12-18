package com.example.test4;

import com.google.android.gms.maps.model.Marker;

import java.util.List;

public class Usuario {

    public Integer id;
    public String nombre;
    public Marker marcador;

    Usuario(Integer id, String nombre, Marker marcador){
        this.id = id;
        this.nombre = nombre;
        this.marcador = marcador;
    }

    public static Usuario get(List<Usuario> usuarios, int id){
        for( Usuario usuario : usuarios ){
            if(usuario.id == id){
                return usuario;
            }
        }
        return null;
    }
}
