package com.example.test4;

import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.test4.databinding.FotografiarBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


public class Fotografiar extends AppCompatActivity {

    FotografiarBinding fotografiar;

    ImageCapture imageCapture;

    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fotografiar = FotografiarBinding.inflate(getLayoutInflater());
        setContentView(fotografiar.getRoot());

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(fotografiar.vistaCamara.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        fotografiar.btnCapturarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fotografiar.btnCapturarFoto.setClickable(false);
                fotografiar.btnCancelarFoto.setClickable(false);

                File file = new File( getExternalFilesDir(Environment.DIRECTORY_PICTURES), getIntent().getIntExtra("pedidoRepartidor", 0) + ".jpg" );

                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

                //ContextCompat.getMainExecutor(Fotografiar.this) <-- para ejecutar la captura en el hilo principal y no ocupar
                //((Aplicacion)Fotografiar.this.getApplication()).controlador_hilo_princpal.post(new Runnable() { para usar el Toast o la UI
                imageCapture.takePicture(outputFileOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("ruta", file.getAbsolutePath());
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        ((Aplicacion)Fotografiar.this.getApplication()).controlador_hilo_princpal.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Fotografiar.this, exception.getMessage(), Toast.LENGTH_LONG).show();

                                fotografiar.btnCapturarFoto.setClickable(true);
                                fotografiar.btnCancelarFoto.setClickable(true);
                            }
                        });
                    }
                });
            }
        });

        fotografiar.btnCancelarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
