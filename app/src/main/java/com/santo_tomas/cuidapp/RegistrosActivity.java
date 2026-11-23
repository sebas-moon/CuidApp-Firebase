package com.santo_tomas.cuidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.santo_tomas.cuidapp.model.RegistroDiario;
import com.santo_tomas.cuidapp.util.Condiciones;
import com.santo_tomas.cuidapp.viewmodel.PacienteViewModel;
import com.santo_tomas.cuidapp.viewmodel.RegistroViewModel;

/*
 * NUEVO - VISTA: "Registros diarios" de UN paciente (idea de la demo web de CuidApp).
 *
 * READ en tiempo real de la subcolección pacientes/{id}/registros.
 * El botón + abre el registro de HOY: si ya existe se edita, si no se crea (un registro por día).
 * Tocar una tarjeta abre ese día (editable si tiene menos de 24 h, si no, solo lectura).
 *
 * Usa DOS ViewModels: PacienteViewModel (nombre y condiciones) y RegistroViewModel (la lista).
 */
public class RegistrosActivity extends BaseActivity {

    public static final String EXTRA_FECHA = "FECHA";

    private TextView tvSubtitulo, tvContador, tvVacio;
    private RegistroAdapter adapter;
    private String idPaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registros);
        InsetsUtil.aplicar(this);

        idPaciente = getIntent().getStringExtra(PacientesActivity.EXTRA_PACIENTE_ID);
        PacienteViewModel pacienteVm = new ViewModelProvider(this).get(PacienteViewModel.class);
        if (idPaciente == null || pacienteVm.getUsuario() == null) {
            finish();
            return;
        }
        RegistroViewModel registroVm = new ViewModelProvider(this).get(RegistroViewModel.class);

        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        tvContador = findViewById(R.id.tvContador);
        tvVacio = findViewById(R.id.tvVacio);
        RecyclerView rv = findViewById(R.id.rvRegistros);

        adapter = new RegistroAdapter(r -> abrirRegistro(r.getFecha()));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Datos del paciente para el subtítulo. Si lo eliminan, esta pantalla ya no tiene sentido.
        pacienteVm.getPacienteSeleccionado().observe(this, p -> {
            if (p == null) {
                finish();
                return;
            }
            tvSubtitulo.setText(getString(R.string.registros_subtitulo,
                    p.getNombre(), Condiciones.texto(p.getCondiciones())));
        });

        registroVm.getRegistros().observe(this, lista -> {
            adapter.setDatos(lista);
            int total = lista == null ? 0 : lista.size();
            int conAlertas = 0;
            if (lista != null) {
                for (RegistroDiario r : lista) if (r.tieneAlertas()) conAlertas++;
            }
            tvContador.setText(getString(R.string.registros_contador, total, conAlertas));
            tvVacio.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
        });
        registroVm.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        configurarBoton(R.id.fabRegistroHoy, v -> abrirRegistro(RegistroViewModel.hoy()));
        configurarBoton(R.id.btnVolverRegistros, v -> finish());

        pacienteVm.cargarPaciente(idPaciente);   // READ paciente
        registroVm.cargarRegistros(idPaciente);  // READ registros (tiempo real)
    }

    // Intent explícito con DOS extras: qué paciente y qué día
    private void abrirRegistro(String fecha) {
        Intent intent = new Intent(this, FormularioRegistroActivity.class);
        intent.putExtra(PacientesActivity.EXTRA_PACIENTE_ID, idPaciente);
        intent.putExtra(EXTRA_FECHA, fecha);
        startActivity(intent);
    }
}
