package com.example.test4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ReceptorRed extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                System.out.println("WIFI");
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                System.out.println("TELEFONO");
            }
        }
    }
}
