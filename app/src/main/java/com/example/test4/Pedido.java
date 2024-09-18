package com.example.test4;

import android.graphics.Bitmap;
import android.view.View;

public class Pedido {

    public String fecha;
    public Integer pedido;
    public Integer tipoComprobante;
    public Integer folioComprovante;
    public Integer clienteClave;
    public String clienteNombre;
    public Integer repartidor;
    public Integer codigos;
    public Integer piezas;
    public Double total;
    public Double latitud;
    public Double longitud;
    public String codigoPostal;
    public String calle;
    public String numeroExterior;
    public String numeroInterior;
    public String observaciones;
    public Double feria;
    public Bitmap bitmapBarra;
    public Bitmap bitmapFoto;
    public Integer visibilidad;
    public Integer visibilidadPgr;
    public Boolean entregable;
    public Boolean eliminable;

    public Pedido(String fecha, Integer pedido, Integer tipoComprobante, Integer folioComprovante, Integer clienteClave, String clienteNombre, Integer repartidor, Integer codigos, Integer piezas, Double total, Double latitud, Double longitud, String codigoPostal, String calle, String numeroExterior, String numeroInterior, String observaciones, Double feria, Bitmap bitmapBarra, Bitmap bitmapFoto, Integer visibilidad, Integer visibilidadPgr, Boolean entregable, Boolean eliminable){
        this.fecha = fecha;
        this.pedido = pedido;
        this.tipoComprobante = tipoComprobante;
        this.folioComprovante = folioComprovante;
        this.clienteClave = clienteClave;
        this.clienteNombre = clienteNombre;
        this.repartidor = repartidor;
        this.codigos = codigos;
        this.piezas = piezas;
        this.total = total;
        this.latitud = latitud;
        this.longitud = longitud;
        this.codigoPostal = codigoPostal;
        this.calle = calle;
        this.numeroExterior = numeroExterior;
        this.numeroInterior = numeroInterior;
        this.observaciones = observaciones;
        this.feria = feria;
        this.bitmapBarra = bitmapBarra;
        this.bitmapFoto = bitmapFoto;
        this.visibilidad = visibilidad;
        this.visibilidadPgr = visibilidadPgr;
        this.entregable = entregable;
        this.eliminable = eliminable;
    }
}
