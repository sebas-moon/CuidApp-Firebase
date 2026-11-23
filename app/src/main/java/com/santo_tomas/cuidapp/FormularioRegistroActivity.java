package com.santo_tomas.cuidapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.santo_tomas.cuidapp.model.Paciente;
import com.santo_tomas.cuidapp.model.RegistroDiario;
import com.santo_tomas.cuidapp.util.Condiciones;
import com.santo_tomas.cuidapp.util.Mascaras;
import com.santo_tomas.cuidapp.viewmodel.PacienteViewModel;
import com.santo_tomas.cuidapp.viewmodel.RegistroViewModel;

import java.util.Locale;

/*
 * NUEVO - VISTA: registro diario de UN paciente en UNA fecha. Hace las 4 operaciones:
 *   - CREATE: si ese día aún no tiene registro (solo se crea el de HOY)
 *   - READ:   si ya existe, se cargan sus valores
 *   - UPDATE: si existe y tiene MENOS de 24 horas
 *   - DELETE: botón eliminar, también solo dentro de las 24 horas
 *
 * FORMULARIO DINÁMICO (idea de la demo web): cada tarjeta se muestra solo si el paciente
 * tiene esa condición. Ej: un paciente con Diabetes ve "Control metabólico"; sin condiciones,
 * solo ve "Bienestar general". Solo se validan las tarjetas visibles.
 *
 * BLOQUEO DE 24 HORAS: pasado ese tiempo el registro queda de SOLO LECTURA (por seguridad
 * clínica nadie puede "corregir" datos antiguos). La app lo bloquea aquí y Firestore lo
 * vuelve a verificar en las reglas de seguridad, por si alguien intenta saltarse la app.
 */
public class FormularioRegistroActivity extends BaseActivity {

    private static final String ESTADO_CAMPOS_LLENOS = "campos_llenos";

    private TextView tvTitulo, tvPaciente, tvBloqueado, tvDolor;
    private View cardCardio, cardDiabetes, cardEpoc, cardErc;
    private TextInputLayout tilSistolica, tilDiastolica, tilFc, tilGlucemia, tilMomento, tilInsulina,
            tilSaturacion, tilFr, tilPesoDiario, tilDiuresis, tilNotas;
    private TextInputEditText etSistolica, etDiastolica, etFc, etGlucemia, etInsulina,
            etSaturacion, etFr, etPesoDiario, etDiuresis, etNotas;
    private MaterialAutoCompleteTextView etMomento;
    private MaterialCheckBox cbMedicacion;
    private Slider sliderDolor;
    private MaterialButton btnGuardar, btnEliminar;
    private ProgressBar progreso;

    private RegistroViewModel registroVm;
    private String idPaciente, fecha;
    private Paciente paciente;            // para saber qué tarjetas mostrar
    private RegistroDiario original;      // null = registro nuevo
    private boolean estadoConocido = false; // ¿ya sabemos si existe o no?
    private boolean bloqueado = false;      // true = más de 24 h, solo lectura
    private boolean camposLlenos = false;
    private boolean formularioValido;       // lo usan los métodos leerEntero / leerDecimal

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario_registro);
        InsetsUtil.aplicar(this);

        idPaciente = getIntent().getStringExtra(PacientesActivity.EXTRA_PACIENTE_ID);
        fecha = getIntent().getStringExtra(RegistrosActivity.EXTRA_FECHA);
        if (idPaciente == null || fecha == null) {
            finish();
            return;
        }
        if (savedInstanceState != null) {
            camposLlenos = savedInstanceState.getBoolean(ESTADO_CAMPOS_LLENOS, false);
        }

        enlazarVistas();
        Mascaras.peso(etPesoDiario);         // 11111 -> 111 (máx. 3 enteros y 1 decimal)
        // El error rojo se borra apenas se corrige el campo (no espera a Guardar)
        TextInputLayout[] tils = {tilSistolica, tilDiastolica, tilFc, tilGlucemia, tilMomento,
                tilInsulina, tilSaturacion, tilFr, tilPesoDiario, tilDiuresis, tilNotas};
        for (TextInputLayout til : tils) {
            if (til.getEditText() == null) continue;
            til.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override
                public void afterTextChanged(Editable s) {
                    if (til.getError() != null) til.setError(null);
                }
            });
        }
        etMomento.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.momentos_glucemia)));
        sliderDolor.addOnChangeListener((slider, valor, delUsuario) ->
                tvDolor.setText(getString(R.string.registro_dolor, (int) valor)));
        tvDolor.setText(getString(R.string.registro_dolor, 0));
        tvTitulo.setText(R.string.registro_titulo_nuevo);
        actualizarBotones(false);

        PacienteViewModel pacienteVm = new ViewModelProvider(this).get(PacienteViewModel.class);
        registroVm = new ViewModelProvider(this).get(RegistroViewModel.class);

        // 1) Paciente: nombre y condiciones (deciden qué tarjetas se ven)
        pacienteVm.getPacienteSeleccionado().observe(this, p -> {
            if (p == null) { // el paciente fue eliminado
                finish();
                return;
            }
            paciente = p;
            tvPaciente.setText(getString(R.string.registros_subtitulo,
                    p.getNombre(), Condiciones.texto(p.getCondiciones())));
            mostrarSecciones();
        });

        // 2) ¿Existe ya el registro de esta fecha?
        registroVm.getExiste().observe(this, existe -> {
            estadoConocido = true;
            if (Boolean.TRUE.equals(existe)) {
                original = registroVm.getRegistro().getValue();
                tvTitulo.setText(getString(R.string.registro_titulo_editar,
                        RegistroViewModel.fechaLegible(fecha)));
                if (original != null && !camposLlenos) {
                    llenarCampos(original);
                    camposLlenos = true;
                }
                bloqueado = original != null && !original.esEditable();
                if (bloqueado) bloquearFormulario();
                mostrarSecciones();
            } else if (!fecha.equals(RegistroViewModel.hoy())) {
                // Solo se pueden CREAR registros de hoy. Si un día antiguo no existe, fue borrado.
                mostrarMensaje(R.string.registro_no_existe);
                finish();
                return;
            }
            actualizarBotones(false);
        });

        registroVm.getCargando().observe(this, cargando -> {
            progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
            actualizarBotones(cargando);
        });
        registroVm.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
        registroVm.getGuardado().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) finish();
        });
        registroVm.getEliminado().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) finish();
        });

        configurarBoton(R.id.btnGuardarRegistro, v -> guardar());
        configurarBoton(R.id.btnEliminarRegistro, v -> confirmarEliminar());
        configurarBoton(R.id.btnVolverRegistro, v -> finish());

        pacienteVm.cargarPaciente(idPaciente);
        if (registroVm.getExiste().getValue() == null) { // al rotar no se vuelve a pedir
            registroVm.cargarRegistro(idPaciente, fecha);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_CAMPOS_LLENOS, camposLlenos);
    }

    private void enlazarVistas() {
        tvTitulo = findViewById(R.id.tvTituloRegistro);
        tvPaciente = findViewById(R.id.tvPacienteRegistro);
        tvBloqueado = findViewById(R.id.tvBloqueado);
        tvDolor = findViewById(R.id.tvDolor);
        cardCardio = findViewById(R.id.cardCardio);
        cardDiabetes = findViewById(R.id.cardDiabetes);
        cardEpoc = findViewById(R.id.cardEpoc);
        cardErc = findViewById(R.id.cardErc);
        tilSistolica = findViewById(R.id.tilSistolica);
        tilDiastolica = findViewById(R.id.tilDiastolica);
        tilFc = findViewById(R.id.tilFc);
        tilGlucemia = findViewById(R.id.tilGlucemia);
        tilMomento = findViewById(R.id.tilMomento);
        tilInsulina = findViewById(R.id.tilInsulina);
        tilSaturacion = findViewById(R.id.tilSaturacion);
        tilFr = findViewById(R.id.tilFr);
        tilPesoDiario = findViewById(R.id.tilPesoDiario);
        tilDiuresis = findViewById(R.id.tilDiuresis);
        tilNotas = findViewById(R.id.tilNotas);
        etSistolica = findViewById(R.id.etSistolica);
        etDiastolica = findViewById(R.id.etDiastolica);
        etFc = findViewById(R.id.etFc);
        etGlucemia = findViewById(R.id.etGlucemia);
        etMomento = findViewById(R.id.etMomento);
        etInsulina = findViewById(R.id.etInsulina);
        etSaturacion = findViewById(R.id.etSaturacion);
        etFr = findViewById(R.id.etFr);
        etPesoDiario = findViewById(R.id.etPesoDiario);
        etDiuresis = findViewById(R.id.etDiuresis);
        etNotas = findViewById(R.id.etNotas);
        cbMedicacion = findViewById(R.id.cbMedicacion);
        sliderDolor = findViewById(R.id.sliderDolor);
        btnGuardar = findViewById(R.id.btnGuardarRegistro);
        btnEliminar = findViewById(R.id.btnEliminarRegistro);
        progreso = findViewById(R.id.progresoRegistro);
    }

    // =====================================================================
    // FORMULARIO DINÁMICO: una tarjeta por condición del paciente.
    // Si un registro antiguo tiene datos de una sección, también se muestra (para no ocultarlos).
    // =====================================================================
    private void mostrarSecciones() {
        boolean hta = tiene(Condiciones.HIPERTENSION);
        boolean dm = tiene(Condiciones.DIABETES);
        boolean epoc = tiene(Condiciones.EPOC);
        boolean erc = tiene(Condiciones.ERC);
        RegistroDiario o = original;

        visible(cardCardio, hta || erc || (o != null && o.getSistolica() != null));
        visible(cardDiabetes, dm || (o != null && o.getGlucemia() != null));
        visible(cardEpoc, epoc || (o != null && o.getSaturacion() != null));
        visible(cardErc, erc || (o != null && o.getPesoDiario() != null));
    }

    private boolean tiene(String condicion) {
        return paciente != null && paciente.tieneCondicion(condicion);
    }

    private void visible(View vista, boolean mostrar) {
        vista.setVisibility(mostrar ? View.VISIBLE : View.GONE);
    }

    private boolean esVisible(View vista) {
        return vista.getVisibility() == View.VISIBLE;
    }

    private void actualizarBotones(boolean cargando) {
        // Hasta saber si el registro existe no dejamos guardar (evita pisar uno existente)
        btnGuardar.setEnabled(estadoConocido && !bloqueado && !cargando);
        btnGuardar.setVisibility(bloqueado ? View.GONE : View.VISIBLE);
        btnEliminar.setVisibility(original != null && !bloqueado ? View.VISIBLE : View.GONE);
        btnEliminar.setEnabled(!cargando);
    }

    // =====================================================================
    // READ: llenar los campos con el registro guardado
    // =====================================================================
    private void llenarCampos(RegistroDiario r) {
        poner(etSistolica, r.getSistolica());
        poner(etDiastolica, r.getDiastolica());
        poner(etFc, r.getFrecuenciaCardiaca());
        poner(etGlucemia, r.getGlucemia());
        if (r.getMomentoGlucemia() != null) etMomento.setText(r.getMomentoGlucemia(), false);
        if (r.getInsulina() != null) etInsulina.setText(decimal(r.getInsulina()));
        poner(etSaturacion, r.getSaturacion());
        poner(etFr, r.getFrecuenciaRespiratoria());
        if (r.getPesoDiario() != null) etPesoDiario.setText(decimal(r.getPesoDiario()));
        poner(etDiuresis, r.getDiuresis());
        cbMedicacion.setChecked(r.isMedicacionTomada());
        sliderDolor.setValue(Math.max(0, Math.min(10, r.getDolor())));
        etNotas.setText(r.getNotas());
    }

    private void poner(TextInputEditText campo, Integer valor) {
        if (valor != null) campo.setText(String.valueOf(valor));
    }

    private String decimal(double valor) {
        return valor == Math.floor(valor)
                ? String.valueOf((int) valor)
                : String.format(Locale.US, "%.1f", valor);
    }

    // Registro de más de 24 h: todo deshabilitado y se muestra el aviso
    private void bloquearFormulario() {
        tvBloqueado.setVisibility(View.VISIBLE);
        View[] campos = {tilSistolica, tilDiastolica, tilFc, tilGlucemia, tilMomento, tilInsulina,
                tilSaturacion, tilFr, tilPesoDiario, tilDiuresis, tilNotas, cbMedicacion, sliderDolor};
        for (View v : campos) v.setEnabled(false);
        TextInputEditText[] textos = {etSistolica, etDiastolica, etFc, etGlucemia, etInsulina,
                etSaturacion, etFr, etPesoDiario, etDiuresis, etNotas};
        for (TextInputEditText et : textos) et.setEnabled(false);
        etMomento.setEnabled(false);
    }

    // =====================================================================
    // CREATE / UPDATE
    // =====================================================================
    private void guardar() {
        if (bloqueado) return;
        formularioValido = true;
        RegistroDiario r = new RegistroDiario();
        r.setPacienteId(idPaciente);
        r.setFecha(fecha);

        // Solo se leen (y validan) las tarjetas visibles; las ocultas quedan null en Firestore
        if (esVisible(cardCardio)) {
            r.setSistolica(leerEntero(tilSistolica, etSistolica, 60, 250, true));
            r.setDiastolica(leerEntero(tilDiastolica, etDiastolica, 30, 150, true));
            r.setFrecuenciaCardiaca(leerEntero(tilFc, etFc, 30, 220, true));
            if (r.getSistolica() != null && r.getDiastolica() != null
                    && r.getDiastolica() >= r.getSistolica()) {
                formularioValido = error(tilDiastolica, "Debe ser menor que la sistólica");
            }
        }
        if (esVisible(cardDiabetes)) {
            r.setGlucemia(leerEntero(tilGlucemia, etGlucemia, 20, 600, true));
            String momento = etMomento.getText().toString().trim();
            if (TextUtils.isEmpty(momento)) {
                formularioValido = error(tilMomento, "Indica cuándo se midió");
            } else {
                tilMomento.setError(null);
                r.setMomentoGlucemia(momento);
            }
            r.setInsulina(leerDecimal(tilInsulina, etInsulina, 0, 100, false));
        }
        if (esVisible(cardEpoc)) {
            r.setSaturacion(leerEntero(tilSaturacion, etSaturacion, 50, 100, true));
            r.setFrecuenciaRespiratoria(leerEntero(tilFr, etFr, 5, 60, true));
        }
        if (esVisible(cardErc)) {
            r.setPesoDiario(leerDecimal(tilPesoDiario, etPesoDiario, 2, 300, true));
            r.setDiuresis(leerEntero(tilDiuresis, etDiuresis, 0, 5000, true));
        }

        // Bienestar general (siempre)
        r.setMedicacionTomada(cbMedicacion.isChecked());
        r.setDolor((int) sliderDolor.getValue());
        String notas = texto(etNotas);
        if (notas.length() > 300) {
            formularioValido = error(tilNotas, "Máximo 300 caracteres");
        } else {
            tilNotas.setError(null);
            r.setNotas(notas.isEmpty() ? null : notas);
        }

        if (!formularioValido) {
            mostrarMensaje(R.string.registro_revisa_campos);
            return;
        }
        // El ViewModel agrega el uid y calcula las ALERTAS antes de enviar a Firestore
        registroVm.guardar(r, original == null);
    }

    // Lee un entero en [min, max]. Si está mal, marca el error y devuelve null.
    private Integer leerEntero(TextInputLayout til, TextInputEditText et, int min, int max, boolean obligatorio) {
        String valor = texto(et);
        if (valor.isEmpty()) {
            if (obligatorio) formularioValido = error(til, "Campo obligatorio");
            else til.setError(null);
            return null;
        }
        try {
            int numero = Integer.parseInt(valor);
            if (numero < min || numero > max) {
                formularioValido = error(til, "Valor fuera de rango (" + min + " a " + max + ")");
                return null;
            }
            til.setError(null);
            return numero;
        } catch (NumberFormatException e) {
            formularioValido = error(til, "Ingresa solo números enteros");
            return null;
        }
    }

    // Igual que leerEntero pero acepta decimales (con punto o coma)
    private Double leerDecimal(TextInputLayout til, TextInputEditText et, double min, double max, boolean obligatorio) {
        String valor = texto(et).replace(',', '.');
        if (valor.isEmpty()) {
            if (obligatorio) formularioValido = error(til, "Campo obligatorio");
            else til.setError(null);
            return null;
        }
        try {
            double numero = Double.parseDouble(valor);
            if (numero < min || numero > max) {
                formularioValido = error(til, "Valor fuera de rango (" + decimal(min) + " a " + decimal(max) + ")");
                return null;
            }
            til.setError(null);
            return numero;
        } catch (NumberFormatException e) {
            formularioValido = error(til, "Ingresa un número (ej: 72.5)");
            return null;
        }
    }

    private boolean error(TextInputLayout campo, String mensaje) {
        campo.setError(mensaje);
        return false;
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    // =====================================================================
    // DELETE: con confirmación (solo dentro de las 24 h)
    // =====================================================================
    private void confirmarEliminar() {
        if (original == null || bloqueado) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.btn_eliminar_registro)
                .setMessage(getString(R.string.dialogo_eliminar_registro,
                        RegistroViewModel.fechaLegible(fecha)))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.si_eliminar, (d, w) -> registroVm.eliminar(original))
                .show();
    }
}
