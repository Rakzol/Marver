package com.example.test4;

import android.graphics.Bitmap;

public class Pedido {
    public String fecha, cliente_nombre;
    public Integer comprobante, folio, cliente_clave, vendedor, codigos, piezas;
    public Double total;

    public Bitmap bitmap;
    public Pedido(String fecha, Integer comprobante, Integer folio, Integer cliente_clave, String cliente_nombre, Integer vendedor, Integer codigos, Integer piezas, Double total, Bitmap bitmap){
        this.fecha = fecha;
        this.comprobante = comprobante;
        this.folio = folio;
        this.cliente_clave = cliente_clave;
        this.cliente_nombre = cliente_nombre;
        this.vendedor = vendedor;
        this.codigos = codigos;
        this.piezas = piezas;
        this.total = total;
        this.bitmap = bitmap;
    }
}
