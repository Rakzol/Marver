package com.example.test4;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;

public class Usuario {

    public Integer id;
    public String nombre;
    public Marker marcador;
    public LatLng posicion_inicial;
    public LatLng posicion_final;
    public LatLng posicion_nueva;

    Usuario(Integer id, String nombre, Marker marcador, LatLng posicion_inicial, LatLng posicion_final, LatLng posicion_nueva){
        this.id = id;
        this.nombre = nombre;
        this.marcador = marcador;
        this.posicion_inicial = posicion_inicial;
        this.posicion_final = posicion_final;
        this.posicion_nueva = posicion_nueva;
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
