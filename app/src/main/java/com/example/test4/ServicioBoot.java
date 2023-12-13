package com.example.test4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServicioBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            // Reiniciar el servicio aquí
            //Intent serviceIntent = new Intent(context, ServicioGPS.class);
            //context.startService(serviceIntent);
        }
    }
}
