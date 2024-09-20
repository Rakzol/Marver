package com.example.test4;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServicioGPS extends Service {

    private static final String ID_CANAL = "CanalServicioGPS";

    private ReceptorRed receptorRed;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        receptorRed = new ReceptorRed();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receptorRed, filter);

        crearCanalNotificaciones();

        Notification notification = new NotificationCompat.Builder(this, ID_CANAL)
                .setSmallIcon(R.mipmap.ic_launcher_foreground_big_capa)
                .setContentTitle("Marver GPS")
                .setContentText("Compartiendo ubicación en tiempo real con Marver")
                .setPriority(NotificationCompat.PRIORITY_MAX).build();

        startForeground(1, notification);

        FusedLocationProviderClient proveedor_locacion_fusionada = LocationServices.getFusedLocationProviderClient(this);

        // Create location request
        LocationRequest solicitud_posicion = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMaxUpdateAgeMillis(0)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            proveedor_locacion_fusionada.requestLocationUpdates(solicitud_posicion,
                    new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

                            if( preferencias_compartidas.getInt("clave", 0) == 0 ){
                                return;
                            }

                            Location locacion = locationResult.getLastLocation();

                            //System.out.println("3:" + locacion.getAccuracy() + ": " + locacion.getLatitude() + "," + locacion.getLongitude() );

                            Float velocidad = 0f;
                            if(locacion.hasSpeed()){
                                velocidad = locacion.getSpeed();
                            }
                            SharedPreferences.Editor editor_preferencias_compartidas_credenciales = preferencias_compartidas.edit();
                            editor_preferencias_compartidas_credenciales.putString("latitud", String.valueOf(locacion.getLatitude()));
                            editor_preferencias_compartidas_credenciales.putString("longitud", String.valueOf(locacion.getLongitude()));
                            editor_preferencias_compartidas_credenciales.apply();

                            String salida = "u=" + preferencias_compartidas.getInt("clave", 0) + "&c=" + preferencias_compartidas.getString("contraseña", "") + "&la=" + locacion.getLatitude() + "&ln=" + locacion.getLongitude() + "&v=" + velocidad;

                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        URL url = new URL("https://www.marverrefacciones.mx/android/posicion");
                                        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                        conexion.setRequestMethod("POST");
                                        //conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                                        conexion.setDoInput(false);
                                        conexion.setDoOutput(true);

                                        conexion.getOutputStream().write(salida.getBytes());

                                        conexion.getResponseCode();

                                        editor_preferencias_compartidas_credenciales.putLong("timeStamp", System.currentTimeMillis() );
                                        editor_preferencias_compartidas_credenciales.apply();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }, getMainLooper()
            );
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receptorRed);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void crearCanalNotificaciones(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    ID_CANAL,
                    "Canal de ServicioGPS Marver",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}
