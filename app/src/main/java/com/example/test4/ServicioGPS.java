package com.example.test4;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        /*if(intent.getAction() == "cerrar"){
            System.out.println("CERRARARARCERRARARARCERRARARARCERRARARARCERRARARARCERRARARAR");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }*/

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

        FusedLocationProviderClient proveedor_locacion_fusionada = LocationServices.getFusedLocationProviderClient(this);

        // Create location request
        LocationRequest solicitud_posicion = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(1000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            proveedor_locacion_fusionada.requestLocationUpdates(solicitud_posicion,
                    new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                                SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

                                Location locacion = locationResult.getLastLocation();
                                String salida = "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&latitud=" + locacion.getLatitude() + "&longitud=" + locacion.getLongitude();

                                if( preferencias_compartidas.getString("usuario", null) == null ){
                                    System.out.println("Sin usuario para subir GPS");
                                    return;
                                }

                                Executors.newSingleThreadExecutor().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            URL url = new URL("https://www.marverrefacciones.mx/android/posicion");
                                            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                            conexion.setRequestMethod("POST");
                                            conexion.setDoOutput(true);

                                            OutputStream output_sream = conexion.getOutputStream();
                                            output_sream.write(salida.getBytes());
                                            output_sream.flush();
                                            output_sream.close();

                                            BufferedReader bufer_lectura = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

                                            String linea;
                                            StringBuilder constructor_cadena = new StringBuilder();
                                            while ((linea = bufer_lectura.readLine()) != null) {
                                                constructor_cadena.append(linea).append("\n");
                                            }

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
