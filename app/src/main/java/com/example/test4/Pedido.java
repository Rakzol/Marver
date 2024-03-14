package com.example.test4;

import android.graphics.Bitmap;
import android.view.View;

public class Pedido {
    public String fecha, cliente_nombre, numero_exterior, numero_interior, observaciones;
    public Integer comprobante, folio, cliente_clave, vendedor, codigos, piezas;
    public Double total, latitud, longitud, feria;

    public Bitmap bitmapBarra, bitmapFoto;

    public Integer visibilidad;

    public Integer visibilidadPgr;

    public Boolean entregable;
    public Pedido(String fecha, Integer comprobante, Integer folio, Integer cliente_clave, String cliente_nombre, Integer vendedor, Integer codigos, Integer piezas, Double total, Bitmap bitmapBarra, Bitmap bitmapFoto, Integer visibilidad, Integer visibilidadPgr, Boolean entregable, Double latitud, Double longitud, String numero_exterior, String numero_interior, String observaciones, Double feria){
        this.fecha = fecha;
        this.comprobante = comprobante;
        this.folio = folio;
        this.cliente_clave = cliente_clave;
        this.cliente_nombre = cliente_nombre;
        this.vendedor = vendedor;
        this.codigos = codigos;
        this.piezas = piezas;
        this.total = total;
        this.bitmapBarra = bitmapBarra;
        this.bitmapFoto = bitmapFoto;
        this.visibilidad = visibilidad;
        this.visibilidadPgr = visibilidadPgr;
        this.entregable = entregable;
        this.latitud = latitud;
        this.longitud = longitud;
        this.numero_exterior = numero_exterior;
        this.numero_interior = numero_interior;
        this.observaciones = observaciones;
        this.feria = feria;
    }
}
