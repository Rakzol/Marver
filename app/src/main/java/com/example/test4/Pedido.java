package com.example.test4;

import android.graphics.Bitmap;
import android.view.View;

public class Pedido {
//
    public String tipoPedido;
    public String fecha;
    public Integer pedido;
    public Integer pedidoRepartidor;
    public String observacionesPedido;
    public Integer tipoComprobante;
    public Integer folioComprobante;
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
    public String colonia;
    public String numeroExterior;
    public String numeroInterior;
    public String observacionesUbicacion;
    public Double feria;
    public Bitmap bitmapBarra;
    public Bitmap bitmapFoto;
    public Integer visibilidad;
    public Integer visibilidadPgr;

    public Pedido(String tipoPedido, String fecha, Integer pedido, Integer pedidoRepartidor, String observacionesPedido, Integer tipoComprobante, Integer folioComprobante, Integer clienteClave, String clienteNombre, Integer repartidor, Integer codigos, Integer piezas, Double total, Double latitud, Double longitud, String codigoPostal, String calle, String colonia, String numeroExterior, String numeroInterior, String observacionesUbicacion, Double feria, Bitmap bitmapBarra, Bitmap bitmapFoto, Integer visibilidad, Integer visibilidadPgr){
        this.tipoPedido = tipoPedido;
        this.fecha = fecha;
        this.pedido = pedido;
        this.pedidoRepartidor = pedidoRepartidor;
        this.observacionesPedido = observacionesPedido;
        this.tipoComprobante = tipoComprobante;
        this.folioComprobante = folioComprobante;
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
        this.colonia = colonia;
        this.numeroExterior = numeroExterior;
        this.numeroInterior = numeroInterior;
        this.observacionesUbicacion = observacionesUbicacion;
        this.feria = feria;
        this.bitmapBarra = bitmapBarra;
        this.bitmapFoto = bitmapFoto;
        this.visibilidad = visibilidad;
        this.visibilidadPgr = visibilidadPgr;
    }
}
