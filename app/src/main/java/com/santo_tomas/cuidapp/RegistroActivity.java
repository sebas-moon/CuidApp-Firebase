package com.santo_tomas.cuidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.santo_tomas.cuidapp.util.Textos;
import com.santo_tomas.cuidapp.viewmodel.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/*
 * NUEVO - VISTA: REGISTRO del cuidador (crear cuenta con correo y contraseña).
 */
public class RegistroActivity extends BaseActivity {

    private TextInputEditText etNombre, etCorreo, etClave, etConfirmar;
    private MaterialButton btnRegistrar;
    private ProgressBar progreso;

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);
        InsetsUtil.aplicar(this); // evita que el contenido quede bajo las barras del sistema

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etNombre = findViewById(R.id.etNombre);
        Textos.bloquearNumeros(etNombre);   // el nombre del cuidador no lleva números
        etCorreo = findViewById(R.id.etCorreo);
        etClave = findViewById(R.id.etClave);
        etConfirmar = findViewById(R.id.etConfirmar);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progreso = findViewById(R.id.progreso);
        TextView tvIrLogin = findViewById(R.id.tvIrLogin);

        viewModel.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        viewModel.getCargando().observe(this, cargando -> {
            progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
            btnRegistrar.setEnabled(!cargando);
        });

        // Al crear la cuenta, Firebase deja al usuario con la sesión YA iniciada
        viewModel.getAutenticado().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) {
                Intent intent = new Intent(this, MenuPrincipalActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        btnRegistrar.setOnClickListener(v -> viewModel.registrar(
                texto(etNombre), texto(etCorreo), texto(etClave), texto(etConfirmar)));

        tvIrLogin.setOnClickListener(v -> finish()); // vuelve al login
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
