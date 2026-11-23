package com.santo_tomas.cuidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.santo_tomas.cuidapp.viewmodel.PacienteViewModel;

/*
 * NUEVO - VISTA: "Mis Pacientes".
 * Muestra EN TIEMPO REAL los pacientes del cuidador conectado (READ),
 * permite agregar uno nuevo (CREATE) y abrir su ficha.
 *
 * INTENTS EXPLÍCITOS: abre FormularioPacienteActivity y FichaPacienteActivity (con el ID como extra).
 */
public class PacientesActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "PACIENTE_ID";

    private TextView tvContador, tvVacio;
    private ProgressBar progreso;
    private PacienteAdapter adapter;

    private PacienteViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pacientes);
        InsetsUtil.aplicar(this);

        viewModel = new ViewModelProvider(this).get(PacienteViewModel.class);

        // PROTECCIÓN: sin sesión volvemos al menú (que a su vez manda al login)
        if (viewModel.getUsuario() == null) {
            volverAlMenu();
            return;
        }

        tvContador = findViewById(R.id.tvContador);
        tvVacio = findViewById(R.id.tvVacio);
        progreso = findViewById(R.id.progreso);
        RecyclerView rv = findViewById(R.id.rvPacientes);

        // --- RecyclerView: tocar una tarjeta abre la ficha del paciente ---
        adapter = new PacienteAdapter(paciente -> {
            Intent intent = new Intent(this, FichaPacienteActivity.class);
            intent.putExtra(EXTRA_PACIENTE_ID, paciente.getId());
            startActivity(intent);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // --- Observadores ---
        viewModel.getPacientes().observe(this, lista -> {
            adapter.setDatos(lista);
            int total = lista == null ? 0 : lista.size();
            tvContador.setText(total == 1
                    ? getString(R.string.pacientes_contador_uno)
                    : getString(R.string.pacientes_contador, total));
            tvVacio.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
        });
        viewModel.getCargando().observe(this, cargando ->
                progreso.setVisibility(cargando ? View.VISIBLE : View.GONE));
        viewModel.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        // --- CREATE: abre el formulario vacío ---
        configurarBoton(R.id.fabAgregar, v ->
                startActivity(new Intent(this, FormularioPacienteActivity.class)));
        configurarBoton(R.id.btnVolverPacientes, v -> volverAlMenu()); // Intent #2

        // --- READ: empieza a escuchar Firestore en tiempo real ---
        viewModel.cargarPacientes();
    }
}
