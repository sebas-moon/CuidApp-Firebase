package com.santo_tomas.cuidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.santo_tomas.cuidapp.viewmodel.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/*
 * NUEVO - VISTA: pantalla de INICIO DE SESIÓN (Firebase Authentication). Es el Launcher.
 * Solo lee lo que escribe el usuario y muestra resultados. La lógica está en AuthViewModel.
 */
public class LoginActivity extends BaseActivity {

    private TextInputEditText etCorreo, etClave;
    private MaterialButton btnIngresar;
    private ProgressBar progreso;

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Si ya hay sesión guardada, saltamos directo al menú (no pedimos login de nuevo)
        if (viewModel.haySesionActiva()) {
            irAPrincipal();
            return;
        }

        setContentView(R.layout.activity_login);
        InsetsUtil.aplicar(this); // evita que el contenido quede bajo las barras del sistema

        // --- Enlazar vistas ---
        etCorreo = findViewById(R.id.etCorreo);
        etClave = findViewById(R.id.etClave);
        btnIngresar = findViewById(R.id.btnIngresar);
        progreso = findViewById(R.id.progreso);
        TextView tvIrRegistro = findViewById(R.id.tvIrRegistro);

        // --- Observadores ---
        viewModel.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        viewModel.getCargando().observe(this, cargando -> {
            progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
            btnIngresar.setEnabled(!cargando); // evita doble clic
        });

        viewModel.getAutenticado().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) irAPrincipal();
        });

        // --- Listeners ---
        btnIngresar.setOnClickListener(v -> viewModel.iniciarSesion(
                texto(etCorreo), texto(etClave)));

        tvIrRegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
    }

    private void irAPrincipal() {
        Intent intent = new Intent(this, MenuPrincipalActivity.class);
        // Limpiamos el historial: al presionar "atrás" en el menú NO se vuelve al login
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
