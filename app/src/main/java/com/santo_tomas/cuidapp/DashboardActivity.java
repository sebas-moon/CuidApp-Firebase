package com.santo_tomas.cuidapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.santo_tomas.cuidapp.model.ResumenDashboard;
import com.santo_tomas.cuidapp.util.EjercicioLocal;
import com.santo_tomas.cuidapp.viewmodel.DashboardViewModel;
import com.santo_tomas.cuidapp.viewmodel.RegistroViewModel;

/**
 * Monitoreo Diario (Dashboard).
 *
 * CAMBIO: antes un Service con un Thread inventaba un BPM AL AZAR cada 3 segundos.
 * Ahora TODO viene de datos reales:
 *  - Último pulso: la frecuencia cardíaca más reciente de los registros diarios (Firestore).
 *  - Anillo exterior: pacientes con registro hoy / total de pacientes.
 *  - Anillo medio: minutos de ejercicio de hoy (EjercicioActivity) / meta de 30.
 *  - Anillo interior: registros de hoy con la medicación tomada.
 *  - Chip: alertas de los registros de hoy.
 * (Cuando se integre el módulo ECG, el pulso vendrá del sensor en tiempo real.)
 *
 * INTENTS EN ESTA CLASE:
 *  - #2 EXPLÍCITO: btn_back_to_menu  -> volverAlMenu() heredado de BaseActivity
 *  - #3 EXPLÍCITO: pestañas Ejercicio y Web -> EjercicioActivity / WebCuidAppActivity
 *  - IMPLÍCITO:    pestaña Navegador abre NavegadorActivity, que lanza ACTION_VIEW (https)
 *  - IMPLÍCITO:    btn_share_report  -> ACTION_SEND con el resumen del día
 */
public class DashboardActivity extends BaseActivity {

    private BottomNavigationView bottomNavigation;
    private TextView tvBpm, tvBpmDetalle, tvValMove, tvValExercise, tvValStand;
    private CircularProgressIndicator ringMove, ringExercise, ringStand;
    private Chip chipAlertas;

    private DashboardViewModel viewModel;
    private ResumenDashboard ultimoResumen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        InsetsUtil.aplicar(this); // evita que el contenido quede bajo las barras del sistema

        inicializarVistas();
        configurarListeners();

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.getResumen().observe(this, this::mostrarResumen);
        viewModel.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver de Ejercicio o de un registro, se actualizan los datos
        if (bottomNavigation != null) bottomNavigation.getMenu().findItem(R.id.nav_health).setChecked(true);
        viewModel.cargar();
        mostrarEjercicio();
    }

    private void inicializarVistas() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        tvBpm = findViewById(R.id.tv_bpm);
        tvBpmDetalle = findViewById(R.id.tv_bpm_detalle);
        tvValMove = findViewById(R.id.tv_val_move);
        tvValExercise = findViewById(R.id.tv_val_exercise);
        tvValStand = findViewById(R.id.tv_val_stand);
        ringMove = findViewById(R.id.ring_move);
        ringExercise = findViewById(R.id.ring_exercise);
        ringStand = findViewById(R.id.ring_stand);
        chipAlertas = findViewById(R.id.chip_steps);

        // Se marca "Salud" ANTES de asignar el listener para que no se dispare al iniciar
        bottomNavigation.setSelectedItemId(R.id.nav_health);
    }

    private void configurarListeners() {
        configurarBoton(R.id.btn_back_to_menu, v -> volverAlMenu());
        configurarBoton(R.id.btn_share_report, v -> compartirReporte());

        // Cada pestaña abre su pantalla. Se devuelve false para que "Salud" siga marcada aquí.
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_health) return true;
            if (id == R.id.nav_exercise) startActivity(new Intent(this, EjercicioActivity.class));
            else if (id == R.id.nav_web) startActivity(new Intent(this, WebCuidAppActivity.class));
            else if (id == R.id.nav_navegador) startActivity(new Intent(this, NavegadorActivity.class));
            return false;
        });
    }

    // =====================================================================
    // Pinta el resumen que llegó de Firestore
    // =====================================================================
    private void mostrarResumen(ResumenDashboard r) {
        if (r == null) return;
        ultimoResumen = r;

        // Último pulso registrado (rojo si está fuera de 50-100 lpm)
        if (r.ultimoPulso != null) {
            tvBpm.setText(getString(R.string.bpm_lectura, r.ultimoPulso));
            boolean fueraDeRango = r.ultimoPulso > 100 || r.ultimoPulso < 50;
            tvBpm.setTextColor(ContextCompat.getColor(this,
                    fueraDeRango ? R.color.accent_move : R.color.accent_exercise));
            tvBpmDetalle.setText(getString(R.string.dash_pulso_detalle, r.ultimoPulsoPaciente,
                    RegistroViewModel.fechaLegible(r.ultimoPulsoFecha)));
        } else {
            tvBpm.setText(R.string.dash_sin_pulso);
            tvBpm.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvBpmDetalle.setText(R.string.dash_sin_pulso_detalle);
        }

        // Anillo exterior: pacientes con registro hoy
        tvValMove.setText(r.registrosHoy + "/" + r.pacientes);
        ringMove.setProgressCompat(ResumenDashboard.porcentaje(r.registrosHoy, r.pacientes), true);

        // Anillo interior: medicación tomada en los registros de hoy
        tvValStand.setText(r.medicacionHoy + "/" + r.registrosHoy);
        ringStand.setProgressCompat(ResumenDashboard.porcentaje(r.medicacionHoy, r.registrosHoy), true);

        // Chip de alertas
        chipAlertas.setText(r.alertasHoy > 0
                ? getString(R.string.dash_alertas, r.alertasHoy)
                : getString(R.string.dash_sin_alertas));
        chipAlertas.setTextColor(ContextCompat.getColor(this,
                r.alertasHoy > 0 ? R.color.accent_move : R.color.text_primary));
    }

    /** Anillo medio: minutos de ejercicio de hoy (guardados en el teléfono). */
    private void mostrarEjercicio() {
        int minutos = EjercicioLocal.minutosHoy(this);
        tvValExercise.setText(String.valueOf(minutos));
        ringExercise.setProgressCompat(
                ResumenDashboard.porcentaje(minutos, EjercicioLocal.META_MINUTOS), true);
    }

    // =====================================================================
    // IMPLÍCITO: compartir el resumen REAL del día (ACTION_SEND)
    // =====================================================================
    private void compartirReporte() {
        ResumenDashboard r = ultimoResumen != null ? ultimoResumen : new ResumenDashboard();
        String pulso = r.ultimoPulso != null
                ? r.ultimoPulso + " lpm (" + r.ultimoPulsoPaciente + ", "
                        + RegistroViewModel.fechaLegible(r.ultimoPulsoFecha) + ")"
                : "sin registros";
        String texto = getString(R.string.dash_reporte,
                RegistroViewModel.fechaLegible(RegistroViewModel.hoy()),
                r.registrosHoy, r.pacientes, r.medicacionHoy, r.registrosHoy, r.alertasHoy,
                EjercicioLocal.minutosHoy(this), pulso);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporte de Salud - CuidApp");
        shareIntent.putExtra(Intent.EXTRA_TEXT, texto);
        try {
            startActivity(Intent.createChooser(shareIntent, "Compartir reporte con..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay apps para compartir", Toast.LENGTH_SHORT).show();
        }
    }
}
