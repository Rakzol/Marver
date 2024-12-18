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
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.http.SslCertificate;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

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

    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        crearCanalNotificaciones();

        Notification notification = new NotificationCompat.Builder(this, ID_CANAL)
                .setSmallIcon(R.mipmap.ic_launcher_foreground_big_capa)
                .setContentTitle("Marver GPS")
                .setContentText("Compartiendo ubicación en tiempo real con Marver")
                .setPriority(NotificationCompat.PRIORITY_MAX).build();

        startForeground(1, notification);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            inicializarActualizacionDePosicion();
        }

        receptorRed = new ReceptorRed();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receptorRed, filter);
    }

    private void inicializarActualizacionDePosicion(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                enviarPosicion(location);
            }

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Solicitar actualizaciones de ubicación cada segundo
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, // Usar solo GPS
                    1000,      // Intervalo en milisegundos (1 segundo)
                    0,         // Distancia mínima de actualización en metros (0 para máxima precisión)
                    locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void enviarPosicion(Location location){
        SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

        if( preferencias_compartidas.getInt("clave", 0) == 0 ){
            return;
        }

        //System.out.println("3:" + locacion.getAccuracy() + ": " + locacion.getLatitude() + "," + locacion.getLongitude() );

        Float velocidad = 0f;
        if(location.hasSpeed()){
            velocidad = location.getSpeed();
        }
        SharedPreferences.Editor editor_preferencias_compartidas_credenciales = preferencias_compartidas.edit();
        editor_preferencias_compartidas_credenciales.putString("latitud", String.valueOf(location.getLatitude()));
        editor_preferencias_compartidas_credenciales.putString("longitud", String.valueOf(location.getLongitude()));
        editor_preferencias_compartidas_credenciales.putInt("intentosGPS", preferencias_compartidas.getInt("intentosGPS", 0) + 1 );
        editor_preferencias_compartidas_credenciales.apply();

        if( preferencias_compartidas.getInt("intentosGPS", 0) < 5 ){
            return;
        }
        editor_preferencias_compartidas_credenciales.putInt("intentosGPS", 0 );
        editor_preferencias_compartidas_credenciales.apply();

        String salida = "u=" + preferencias_compartidas.getInt("clave", 0) + "&c=" + preferencias_compartidas.getString("contraseña", "") + "&la=" + location.getLatitude() + "&ln=" + location.getLongitude() + "&v=" + velocidad + "&s=" + ( preferencias_compartidas.getString("sucursal", "Mochis").equalsIgnoreCase("Mochis") ? "M" : "G") ;

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

                                        /*editor_preferencias_compartidas_credenciales.putLong("timeStamp", System.currentTimeMillis() );
                                        editor_preferencias_compartidas_credenciales.apply();*/

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
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
