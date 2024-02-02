package com.example.test4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.test4.databinding.ManejadorBinding;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class Manejador extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ManejadorBinding manejador;

    FragmentManager manejadorFragmentos;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manejador = ManejadorBinding.inflate(getLayoutInflater());
        setContentView(manejador.getRoot());

        manejador.btnCamaraAsignar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(Manejador.this);

                scanner
                        .startScan()
                        .addOnSuccessListener(
                                barcode -> {
                                    String codigo = barcode.getRawValue();
                                    System.out.println(codigo);

                                    AlertDialog.Builder builder = new AlertDialog.Builder(Manejador.this);
                                    View dialogView = getLayoutInflater().inflate(R.layout.asignar_pedido, null);

                                    builder.setView(dialogView);

                                    AlertDialog alertDialog = builder.create();
                                    alertDialog.show();

                                    ((Button) dialogView.findViewById(R.id.btnRegresarAsigarPedido)).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            alertDialog.dismiss();
                                        }
                                    });

                                    Executors.newSingleThreadExecutor().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            try{
                                                URL url = new URL("https://www.marverrefacciones.mx/android/asignar_pedido");
                                                HttpURLConnection conexion = (HttpURLConnection) url.openConnection();

                                                conexion.setRequestMethod("POST");
                                                conexion.setDoOutput(true);

                                                SharedPreferences preferencias_compartidas = getSharedPreferences("credenciales", MODE_PRIVATE);

                                                OutputStream output_sream = conexion.getOutputStream();
                                                output_sream.write(( "usuario=" + preferencias_compartidas.getString("usuario", "") + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + barcode.getRawValue() ).getBytes());
                                                output_sream.flush();
                                                output_sream.close();

                                                BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                                                String linea;
                                                StringBuilder constructor_cadena = new StringBuilder();
                                                while( (linea = bufer_lectura.readLine()) != null ){
                                                    constructor_cadena.append(linea).append("\n");
                                                }

                                                JSONObject json_resultado = new JSONObject( constructor_cadena.toString() );

                                                ((Aplicacion)getApplication()).controlador_hilo_princpal.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {

                                                            ((ProgressBar) dialogView.findViewById(R.id.prgAsignarPedido)).setVisibility( View.GONE );
                                                            if( json_resultado.getInt("status") != 0 ){
                                                                ((ImageView) dialogView.findViewById(R.id.imgResultadoAsignarPedido)).setImageResource(R.drawable.error);
                                                            }
                                                            ((ImageView) dialogView.findViewById(R.id.imgResultadoAsignarPedido)).setVisibility(View.VISIBLE);

                                                            ((TextView) dialogView.findViewById(R.id.txtResultadoPedido)).setText( json_resultado.getString("mensaje") );

                                                        }catch (Exception e){
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                });
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                })
                        .addOnCanceledListener(
                                () -> {
                                    // Task canceled
                                })
                        .addOnFailureListener(
                                e -> {
                                    System.out.println(e.getMessage());
                                    System.out.println(e.getLocalizedMessage());
                                    e.printStackTrace();
                                    Toast.makeText(Manejador.this, "Intentelo nuevamente", Toast.LENGTH_LONG).show();
                                });
            }
        });

        setSupportActionBar(manejador.barraHerramientasSuperiorMapa);

        ActionBarDrawerToggle mostrador = new ActionBarDrawerToggle(this, manejador.layoutManejador, manejador.barraHerramientasSuperiorMapa, R.string.nav_abrir, R.string.nav_cerrar );
        manejador.layoutManejador.addDrawerListener(mostrador);
        mostrador.syncState();

        manejador.drawerNavegacion.setNavigationItemSelectedListener(this);

        manejador.barraVistaNavegacionInferior.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if( id == R.id.nav_inferior_mapa){
                    manejador.drawerNavegacion.getMenu().getItem(0).setChecked(true);
                    manejador.barraHerramientasSuperiorMapa.setTitle("Mapa en Vivo");
                    abrirFragmento(new Mapa());
                    return true;
                }else if( id == R.id.nav_inferior_pendientes ){
                    manejador.drawerNavegacion.getMenu().getItem(2).setChecked(true);
                    manejador.barraHerramientasSuperiorMapa.setTitle("Pedidos Pendientes");
                    abrirFragmento(new Pedidos());
                    return true;
                }
                return false;
            }
        });

        manejadorFragmentos = getSupportFragmentManager();
        abrirFragmento( new Mapa() );

        manejador.drawerNavegacion.getMenu().getItem(0).setChecked(true);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if( id == R.id.nav_lateral_mapa){
            manejador.barraVistaNavegacionInferior.setSelectedItemId(R.id.nav_inferior_mapa);
            //abrirFragmento(new Mapa());
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            return true;
        }else if( id == R.id.nav_lateral_pendientes ){
            manejador.barraVistaNavegacionInferior.setSelectedItemId(R.id.nav_inferior_pendientes);
            //abrirFragmento(new Pedidos());
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    private void abrirFragmento( Fragment fragmento ){
        FragmentTransaction transaccion = manejadorFragmentos.beginTransaction();
        transaccion.replace(R.id.contenedor_fragmentos, fragmento);
        transaccion.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.barra_herramientas_mapa, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.cerrarSesion){
            SharedPreferences.Editor preferencias_compartidas_editor = getSharedPreferences("credenciales", MODE_PRIVATE).edit();
            preferencias_compartidas_editor.remove("usuario");
            preferencias_compartidas_editor.remove("contraseña");
            preferencias_compartidas_editor.remove("id");
            preferencias_compartidas_editor.apply();

            Intent intent = new Intent(this, Inicio.class);
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
