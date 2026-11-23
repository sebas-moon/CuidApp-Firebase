package com.santo_tomas.cuidapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.santo_tomas.cuidapp.model.ResumenDashboard;
import com.santo_tomas.cuidapp.model.Rutina;
import com.santo_tomas.cuidapp.util.EjercicioLocal;
import com.santo_tomas.cuidapp.viewmodel.EjercicioViewModel;

import java.util.Locale;

/*
 * NUEVO - Pestaña "Ejercicio" del Monitoreo Diario.
 * Lista de rutinas suaves con temporizador guiado paso a paso.
 * Al terminar, los minutos se suman al día y el anillo "Ejercicio" del Dashboard avanza.
 */
public class EjercicioActivity extends BaseActivity {

    private TextView tvHoy, tvRutinaActual, tvPaso, tvNumeroPaso, tvTiempo;
    private View cardTemporizador;
    private LinearProgressIndicator progresoDia;
    private MaterialButton btnPausar;
    private EjercicioViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ejercicio);
        InsetsUtil.aplicar(this);

        tvHoy = findViewById(R.id.tvEjercicioHoy);
        progresoDia = findViewById(R.id.progresoEjercicioDia);
        cardTemporizador = findViewById(R.id.cardTemporizador);
        tvRutinaActual = findViewById(R.id.tvRutinaActual);
        tvPaso = findViewById(R.id.tvPasoActual);
        tvNumeroPaso = findViewById(R.id.tvNumeroPaso);
        tvTiempo = findViewById(R.id.tvTiempoRestante);
        btnPausar = findViewById(R.id.btnPausarRutina);

        viewModel = new ViewModelProvider(this).get(EjercicioViewModel.class);

        RecyclerView rv = findViewById(R.id.rvRutinas);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new RutinaAdapter(Rutina.catalogo(), rutina -> {
            if (viewModel.enCurso()) {
                Toast.makeText(this, R.string.ejercicio_en_curso, Toast.LENGTH_SHORT).show();
            } else {
                viewModel.iniciar(rutina);
            }
        }));

        // --- Observadores del temporizador ---
        viewModel.getRutina().observe(this, r -> {
            cardTemporizador.setVisibility(r == null ? View.GONE : View.VISIBLE);
            if (r != null) tvRutinaActual.setText(r.emoji + " " + r.nombre);
        });
        viewModel.getRestanteMs().observe(this, ms -> {
            long s = ms / 1000;
            tvTiempo.setText(String.format(Locale.US, "%02d:%02d", s / 60, s % 60));
        });
        viewModel.getPaso().observe(this, i -> {
            Rutina r = viewModel.getRutina().getValue();
            if (r == null) return;
            tvPaso.setText(r.pasos[i]);
            tvNumeroPaso.setText(getString(R.string.ejercicio_paso, i + 1, r.pasos.length));
        });
        viewModel.getPausado().observe(this, p ->
                btnPausar.setText(Boolean.TRUE.equals(p) ? R.string.ejercicio_reanudar : R.string.ejercicio_pausar));
        viewModel.getMinutosTerminados().observe(this, min -> {
            if (min == null) return;
            if (min > 0) {
                EjercicioLocal.sumarMinutos(this, min);
                Toast.makeText(this, getString(R.string.ejercicio_sumado, min), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.ejercicio_muy_corto, Toast.LENGTH_SHORT).show();
            }
            viewModel.avisoMostrado();
            mostrarProgresoDia();
        });

        configurarBoton(R.id.btnPausarRutina, v -> viewModel.pausarOReanudar());
        configurarBoton(R.id.btnTerminarRutina, v -> viewModel.terminar());
        configurarBoton(R.id.btnVolverEjercicio, v -> finish());

        mostrarProgresoDia();
    }

    @Override
    protected void onDestroy() {
        // Si sale de la pantalla con una rutina a medias, no se pierden los minutos ya hechos
        if (isFinishing() && viewModel != null && viewModel.enCurso()) {
            EjercicioLocal.sumarMinutos(this, viewModel.minutosHechos());
        }
        super.onDestroy();
    }

    private void mostrarProgresoDia() {
        int minutos = EjercicioLocal.minutosHoy(this);
        tvHoy.setText(getString(R.string.ejercicio_hoy, minutos, EjercicioLocal.META_MINUTOS));
        progresoDia.setProgressCompat(ResumenDashboard.porcentaje(minutos, EjercicioLocal.META_MINUTOS), true);
    }
}
