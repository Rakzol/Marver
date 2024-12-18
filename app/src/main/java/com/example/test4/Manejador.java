package com.example.test4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.test4.databinding.ManejadorBinding;
import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
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

    SearchView searchView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manejador = ManejadorBinding.inflate(getLayoutInflater());
        setContentView(manejador.getRoot());

        Intent intent_servicioGPS = new Intent( this, ServicioGPS.class);
        startService(intent_servicioGPS);

        manejador.buttonCamaraAsignarManejador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

                if(activeNetwork == null){
                    Toast.makeText(Manejador.this, "Conectese a WIFI, por favor", Toast.LENGTH_LONG).show();
                    return;
                }

                if(!activeNetwork.isConnectedOrConnecting()){
                    Toast.makeText(Manejador.this, "Conectese a WIFI, por favor", Toast.LENGTH_LONG).show();
                    return;
                }

                if (activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
                    Toast.makeText(Manejador.this, "Conectese a WIFI, por favor", Toast.LENGTH_LONG).show();
                    return;
                }

                GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(Manejador.this);

                ModuleInstallClient moduleInstallClient = ModuleInstall.getClient(Manejador.this);

                ModuleInstallRequest moduleInstallRequest =
                        ModuleInstallRequest.newBuilder()
                                .addApi(scanner)
                                // Add more API if you would like to request multiple optional modules
                                //.addApi(...)
                                // Set the listener if you need to monitor the download progress
                                //.setListener(listener)
                                .build();

                moduleInstallClient.installModules(moduleInstallRequest)
                        .addOnSuccessListener(
                                response -> {
                                    if (response.areModulesAlreadyInstalled()) {
                                        // Modules are already installed when the request is sent.
                                        //Toast.makeText(Manejador.this, "Modules are already installed", Toast.LENGTH_LONG).show();
                                    }
                                    //Toast.makeText(Manejador.this, "Modules successfully installed", Toast.LENGTH_LONG).show();
                                    scanner
                                            .startScan()
                                            .addOnSuccessListener(
                                                    barcode -> {

                                                        AlertDialog.Builder builder = new AlertDialog.Builder(Manejador.this);
                                                        View dialogView = getLayoutInflater().inflate(R.layout.dialogo_procesar_pedido, null);

                                                        builder.setView(dialogView);

                                                        ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText( "Asignando pedido. . ." );

                                                        AlertDialog alertDialog = builder.create();
                                                        alertDialog.show();

                                                        ((Button) dialogView.findViewById(R.id.buttonCerrarProcesarPedido)).setOnClickListener(new View.OnClickListener() {
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
                                                                    output_sream.write(( "clave=" + preferencias_compartidas.getInt("clave", 0) + "&contraseña=" + preferencias_compartidas.getString("contraseña", "") + "&folio=" + barcode.getRawValue() + "&sucursal="+preferencias_compartidas.getString("sucursal", "Mochis") ).getBytes());
                                                                    output_sream.flush();
                                                                    output_sream.close();

                                                                    BufferedReader bufer_lectura = new BufferedReader( new InputStreamReader( conexion.getInputStream() ) );

                                                                    String linea;
                                                                    StringBuilder constructor_cadena = new StringBuilder();
                                                                    while( (linea = bufer_lectura.readLine()) != null ){
                                                                        constructor_cadena.append(linea).append("\n");
                                                                    }

                                                                    JSONObject json_resultado = new JSONObject( constructor_cadena.toString() );

                                                                    ((Aplicacion)getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            try {
                                                                                if (json_resultado.getInt("status") != 0) {
                                                                                    ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                                } else {
                                                                                    manejador.bottomNavigationViewManejador.setSelectedItemId(R.id.itemPendientesBarraNavegacionInferior);
                                                                                }
                                                                                ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText(json_resultado.getString("mensaje"));
                                                                            } catch (Exception e) {
                                                                                ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                                ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                                                e.printStackTrace();
                                                                            }
                                                                            ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                            ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                                        }
                                                                    });
                                                                }catch (Exception e){
                                                                    ((Aplicacion)getApplication()).controladorHiloPrincipal.post(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            ((ProgressBar) dialogView.findViewById(R.id.progressProcesarPedido)).setVisibility(View.GONE);
                                                                            ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setImageResource(R.drawable.error);
                                                                            ((ImageView) dialogView.findViewById(R.id.imageResultadoProcesarPedido)).setVisibility(View.VISIBLE);
                                                                            ((TextView) dialogView.findViewById(R.id.textResultadoProcesarPedido)).setText("Error con la conexion");
                                                                        }
                                                                    });
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
                                })
                        .addOnFailureListener(
                                e -> {
                                    // Handle failure...
                                    Toast.makeText(Manejador.this, "Instalando Scanner. . .", Toast.LENGTH_LONG).show();
                                });
            }
        });

        setSupportActionBar(manejador.toolBarManejador);

        ActionBarDrawerToggle mostrador = new ActionBarDrawerToggle(this, manejador.layoutManejador, manejador.toolBarManejador, R.string.nav_abrir, R.string.nav_cerrar );
        manejador.layoutManejador.addDrawerListener(mostrador);
        mostrador.syncState();

        manejador.navigationViewManejador.setNavigationItemSelectedListener(this);

        ((TextView)manejador.navigationViewManejador.getHeaderView(0).findViewById(R.id.textNombreUsuarioNavLateral)).setText( getSharedPreferences("credenciales", MODE_PRIVATE).getString("usuario", "") );
        ((TextView)manejador.navigationViewManejador.getHeaderView(0).findViewById(R.id.textClaveUsuarioNavLateral)).setText( String.valueOf(getSharedPreferences("credenciales", MODE_PRIVATE).getInt("clave", 0)) );

        manejador.bottomNavigationViewManejador.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                searchView.setIconified(true);
                searchView.onActionViewCollapsed();

                int id = item.getItemId();
                if( id == R.id.itemPendientesBarraNavegacionInferior ){
                    manejador.bottomNavigationViewManejador.getMenu().getItem(0).setChecked(true);
                    manejador.toolBarManejador.setTitle("Pedidos Pendientes");
                    abrirFragmento(Pedidos.NuevoPedido(Pedidos.PENDIENTES));
                    return true;
                }
                else if( id == R.id.itemEnRutaBarraNavegacionInferior ){
                    manejador.bottomNavigationViewManejador.getMenu().getItem(1).setChecked(true);
                    manejador.toolBarManejador.setTitle("Pedidos en Ruta");
                    abrirFragmento(Pedidos.NuevoPedido(Pedidos.EN_RUTA));
                    return true;
                }
                else if( id == R.id.itemEntregadosBarraNavegacionInferior ){
                    manejador.bottomNavigationViewManejador.getMenu().getItem(2).setChecked(true);
                    manejador.toolBarManejador.setTitle("Pedidos Entregados");
                    abrirFragmento(Pedidos.NuevoPedido(Pedidos.ENTREGADOS));
                    return true;
                }
                else if( id == R.id.itemFinalizadosBarraNavegacionInferior ){
                    manejador.bottomNavigationViewManejador.getMenu().getItem(3).setChecked(true);
                    manejador.toolBarManejador.setTitle("Pedidos Finalizados");
                    abrirFragmento(Pedidos.NuevoPedido(Pedidos.FINALIZADOS));
                    return true;
                }
                return false;
            }
        });

        manejadorFragmentos = getSupportFragmentManager();
        abrirFragmento(Pedidos.NuevoPedido(Pedidos.PENDIENTES));

        manejador.bottomNavigationViewManejador.getMenu().getItem(0).setChecked(true);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if( id == R.id.itemPendientesBarraNavegacionLateral ){
            manejador.bottomNavigationViewManejador.setSelectedItemId(R.id.itemPendientesBarraNavegacionInferior);
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            return true;
        }
        else if( id == R.id.itemEnRutaBarraNavegacionLateral ){
            manejador.bottomNavigationViewManejador.setSelectedItemId(R.id.itemEnRutaBarraNavegacionInferior);
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            return true;
        }
        else if( id == R.id.itemEntregadosBarraNavegacionLateral ){
            manejador.bottomNavigationViewManejador.setSelectedItemId(R.id.itemEntregadosBarraNavegacionInferior);
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            return true;
        }
        else if( id == R.id.itemFinalizadosBarraNavegacionLateral ){
            manejador.bottomNavigationViewManejador.setSelectedItemId(R.id.itemFinalizadosBarraNavegacionInferior);
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            return true;
        }
        else if( id == R.id.itemRutaBarraNavegacionLateral ){
            manejador.toolBarManejador.setTitle("Ruta De Pedidos");
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            abrirFragmento(new Ruta());
            return true;
        }
        else if( id == R.id.itemNoPagadosBarraNavegacionLateral ){
            manejador.toolBarManejador.setTitle("Pedidos no pagados");
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            abrirFragmento(Pedidos.NuevoPedido(Pedidos.NO_PAGADOS));
            return true;
        }
        else if( id == R.id.itemNoEntregadosBarraNavegacionLateral ){
            manejador.toolBarManejador.setTitle("Pedidos no entregados");
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            abrirFragmento(Pedidos.NuevoPedido(Pedidos.NO_ENTREGADOS));
            return true;
        }
        else if( id == R.id.itemRechazadosBarraNavegacionLateral ){
            manejador.toolBarManejador.setTitle("Pedidos rechazados");
            manejador.layoutManejador.closeDrawer(GravityCompat.START);
            abrirFragmento(Pedidos.NuevoPedido(Pedidos.RECHAZADOS));
            return true;
        }
        else if( id == R.id.itemSalirBarraNavegacionLateral ){
            cerrar_sesion();
            return true;
        }
        return false;
    }

    private void abrirFragmento( Fragment fragmento ){
        FragmentTransaction transaccion = manejadorFragmentos.beginTransaction();
        transaccion.replace(R.id.layoutFragmentosManejador, fragmento);
        transaccion.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.barra_herramientas_mapa, menu);

        MenuItem menuItem = menu.findItem(R.id.itemBuscarBarraHerramientasMapa);
        searchView = (SearchView) menuItem.getActionView();

        EditText searchEditText = (EditText) searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setHintTextColor(getResources().getColor(R.color.rojo_salmon));

        // Cambiar el color del ícono de cerrar
        ImageView closeIcon = (ImageView) searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeIcon.setColorFilter(getResources().getColor(R.color.blanco));

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Fragment fragmento_actual = manejadorFragmentos.findFragmentById(R.id.layoutFragmentosManejador);
                if( fragmento_actual instanceof fragmentoBuscador ){
                    fragmentoBuscador fragmento_busador = (fragmentoBuscador) fragmento_actual;
                    fragmento_busador.buscador_cerrado();
                }
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragmento_actual = manejadorFragmentos.findFragmentById(R.id.layoutFragmentosManejador);
                if( fragmento_actual instanceof fragmentoBuscador ){
                    fragmentoBuscador fragmento_busador = (fragmentoBuscador) fragmento_actual;
                    fragmento_busador.buscador_clickeado();
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Fragment fragmento_actual = manejadorFragmentos.findFragmentById(R.id.layoutFragmentosManejador);
                if( fragmento_actual instanceof fragmentoBuscador ){
                    fragmentoBuscador fragmento_busador = (fragmentoBuscador) fragmento_actual;
                    fragmento_busador.buscador_enviado(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Fragment fragmento_actual = manejadorFragmentos.findFragmentById(R.id.layoutFragmentosManejador);
                if( fragmento_actual instanceof fragmentoBuscador ){
                    fragmentoBuscador fragmento_busador = (fragmentoBuscador) fragmento_actual;
                    fragmento_busador.buscador_escrito(newText);
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.itemCerrarSessionBarraHerramientasMapa){
            cerrar_sesion();
        }

        return super.onOptionsItemSelected(item);
    }

    private void cerrar_sesion(){
        SharedPreferences.Editor preferencias_compartidas_editor = getSharedPreferences("credenciales", MODE_PRIVATE).edit();
        preferencias_compartidas_editor.remove("usuario");
        preferencias_compartidas_editor.remove("contraseña");
        preferencias_compartidas_editor.remove("clave");
        preferencias_compartidas_editor.apply();

        getSharedPreferences("procesos", Context.MODE_PRIVATE).edit().putBoolean("subiendo_fotos", false).apply();

        Intent intent = new Intent(this, Inicio.class);
        startActivity(intent);
        finish();
    }
}
