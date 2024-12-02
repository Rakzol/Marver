package com.example.test4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
//
public class ReceptorRed extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Pedidos.subir_fotos(context);
    }
}
