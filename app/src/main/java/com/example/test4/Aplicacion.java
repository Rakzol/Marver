package com.example.test4;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Aplicacion extends Application {
    //
    //public ExecutorService servicio_ejecucion = Executors.newFixedThreadPool(4);
    public Handler controladorHiloPrincipal = HandlerCompat.createAsync(Looper.getMainLooper());

}
