package com.example.test4;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test4.databinding.ManejadorBinding;
import com.example.test4.databinding.PedidosBinding;
import com.google.android.material.navigation.NavigationView;

public class Manejador extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ManejadorBinding manejador;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manejador = ManejadorBinding.inflate(getLayoutInflater());
        setContentView(manejador.getRoot());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}
