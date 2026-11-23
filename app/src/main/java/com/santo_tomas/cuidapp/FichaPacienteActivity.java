package com.santo_tomas.cuidapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.santo_tomas.cuidapp.model.Paciente;
import com.santo_tomas.cuidapp.util.Condiciones;
import com.santo_tomas.cuidapp.util.Fechas;
import com.santo_tomas.cuidapp.util.Formatos;
import com.santo_tomas.cuidapp.viewmodel.PacienteViewModel;

import java.util.Locale;

/**
 * Ficha del paciente.
 *
 * MEJORA CON FIREBASE: antes mostraba UN paciente guardado en SharedPreferences.
 * Ahora recibe el ID de un paciente, lo lee de Cloud Firestore EN TIEMPO REAL (READ)
 * y permite editarlo (UPDATE, en FormularioPacienteActivity) o eliminarlo (DELETE).
 *
 * INTENTS EN ESTA CLASE:
 *  - EXPLÍCITO:    btnEditarDatos     -> FormularioPacienteActivity con el ID del paciente
 *  - EXPLÍCITO:    btnRegistros       -> RegistrosActivity (registros diarios de ESTE paciente)
 *  - EXPLÍCITO:    btnVolverFicha     -> finish() vuelve a "Mis Pacientes"
 *  - #4 IMPLÍCITO: llamarEmergencia() -> ACTION_DIAL al 131
 *  - NUEVO IMPLÍCITO: llamarContacto() -> ACTION_DIAL al contacto de emergencia guardado
 *  - #5 IMPLÍCITO: enviarCorreo()     -> ACTION_SENDTO (mailto:) con la ficha en el cuerpo
 *  - #6 IMPLÍCITO: abrirCamara()      -> ACTION_IMAGE_CAPTURE (la foto vuelve y se muestra en pantalla)
 *
 * PERMISO EN TIEMPO DE EJECUCIÓN: CAMERA.
 * Si el Manifest declara CAMERA y la app NO lo tiene concedido, lanzar ACTION_IMAGE_CAPTURE
 * provoca SecurityException. Por eso se verifica antes de abrir la cámara.
 */
public class FichaPacienteActivity extends BaseActivity {

    private ImageView ivFoto;
    private TextView tvNombre, tvRut, tvEdad, tvMedidas, tvSangre, tvAlergias, tvCondiciones, tvContacto,
            tvNacimiento, tvDocumento;
    private MaterialButton btnLlamarContacto;

    private PacienteViewModel viewModel;
    private String idPaciente;
    private Paciente pacienteActual; // último dato recibido de Firestore
    private boolean eliminando = false; // true cuando ESTE usuario pidió borrar

    /** Respuesta del diálogo de permiso de cámara: si lo aceptó, abre la cámara de inmediato. */
    private final ActivityResultLauncher<String> solicitarPermisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    abrirCamara();
                } else {
                    mostrarMensaje(R.string.msg_camara_denegada);
                }
            });

    /** Resultado de la cámara: llega la miniatura de la foto en el extra "data". */
    private final ActivityResultLauncher<Intent> lanzadorCamara =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                // Validación 1: el usuario pudo cancelar la captura
                if (resultado.getResultCode() != RESULT_OK) {
                    mostrarMensaje(R.string.msg_foto_cancelada);
                    return;
                }
                Bitmap miniatura = extraerMiniatura(resultado.getData());

                // Validación 2: la cámara puede devolver el resultado sin imagen
                if (miniatura == null || ivFoto == null) {
                    mostrarMensaje(R.string.err_foto);
                    return;
                }
                ivFoto.setImageBitmap(miniatura);
                ivFoto.setVisibility(View.VISIBLE);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficha_paciente);
        InsetsUtil.aplicar(this); // evita que el contenido quede bajo las barras del sistema

        // El ID llega desde PacientesActivity. Sin ID no hay nada que mostrar.
        idPaciente = getIntent().getStringExtra(PacientesActivity.EXTRA_PACIENTE_ID);
        if (idPaciente == null) {
            mostrarMensaje(R.string.ficha_sin_id);
            finish();
            return;
        }

        ivFoto     = findViewById(R.id.iv_foto_perfil);
        tvNombre   = findViewById(R.id.tv_nombre);
        tvRut      = findViewById(R.id.tv_rut);
        tvEdad     = findViewById(R.id.tv_edad);
        tvMedidas  = findViewById(R.id.tv_medidas);
        tvSangre   = findViewById(R.id.tv_sangre);
        tvAlergias = findViewById(R.id.tv_alergias);
        tvCondiciones = findViewById(R.id.tv_condiciones);
        tvNacimiento = findViewById(R.id.tv_nacimiento);
        tvDocumento = findViewById(R.id.tv_documento);
        tvContacto = findViewById(R.id.tv_contacto);
        btnLlamarContacto = findViewById(R.id.btnLlamarContacto);

        // --- MVVM: la ficha observa al paciente en Firestore ---
        viewModel = new ViewModelProvider(this).get(PacienteViewModel.class);

        viewModel.getPacienteSeleccionado().observe(this, paciente -> {
            if (paciente == null) {
                // El documento ya no existe. Si no lo borramos nosotros, fue desde otro dispositivo.
                mostrarMensaje(eliminando ? R.string.ficha_eliminado_ok : R.string.ficha_eliminado);
                finish();
                return;
            }
            pacienteActual = paciente;
            mostrarDatos(paciente);
        });
        viewModel.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
        viewModel.getEliminado().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) finish();
        });

        viewModel.cargarPaciente(idPaciente); // READ (tiempo real)

        configurarBoton(R.id.btnLlamar,           v -> llamarEmergencia());
        configurarBoton(R.id.btnLlamarContacto,   v -> llamarContacto());
        configurarBoton(R.id.btnCorreo,           v -> enviarCorreo());
        configurarBoton(R.id.btnFoto,             v -> tomarFoto());
        configurarBoton(R.id.btnEditarDatos,      v -> editarPaciente());
        configurarBoton(R.id.btnEliminarPaciente, v -> confirmarEliminar());
        configurarBoton(R.id.btnRegistros,        v -> abrirRegistros());
        configurarBoton(R.id.btnVolverFicha,      v -> finish()); // vuelve a Mis Pacientes
    }

    /** Pinta los datos del paciente (vienen de Firestore) usando los formatos de strings.xml. */
    private void mostrarDatos(Paciente p) {
        tvNombre.setText(getString(R.string.ficha_fmt_nombre, p.getNombre()));
        tvRut.setText(getString(R.string.ficha_fmt_rut, p.getRut()));
        tvEdad.setText(getString(R.string.ficha_fmt_edad, p.getEdad(), texto(p.getSexo())));
        tvMedidas.setText(getString(R.string.ficha_fmt_medidas,
                String.format(Locale.US, "%.1f", p.getPeso()).replace('.', ','),
                Formatos.estaturaMetros(p.getEstatura())));
        tvSangre.setText(getString(R.string.ficha_fmt_sangre, p.getTipoSangre()));
        tvAlergias.setText(getString(R.string.ficha_fmt_alergias, p.getAlergias()));
        tvCondiciones.setText(getString(R.string.ficha_fmt_condiciones, Condiciones.texto(p.getCondiciones())));
        tvContacto.setText(getString(R.string.ficha_fmt_contacto,
                p.getContactoNombre(), texto(p.getContactoRelacion()),
                Formatos.telefonoLegible(p.getContactoTelefono())));
        mostrarDocumento(p);
        btnLlamarContacto.setText(getString(R.string.ficha_btn_llamar_contacto, p.getContactoNombre()));
    }

    /** Datos de la cédula: solo se muestran si existen. Avisa en rojo si la cédula está vencida. */
    private void mostrarDocumento(Paciente p) {
        String nacimiento = p.getFechaNacimiento();
        if (nacimiento != null && !nacimiento.isEmpty()) {
            tvNacimiento.setText(getString(R.string.ficha_fmt_nacimiento, nacimiento, texto(p.getNacionalidad())));
            tvNacimiento.setVisibility(View.VISIBLE);
        } else {
            tvNacimiento.setVisibility(View.GONE);
        }

        String numero = p.getNumeroDocumento();
        String vence = p.getFechaVencimiento();
        if (vence != null && !vence.isEmpty() && Fechas.yaPaso(vence)) {
            tvDocumento.setText(getString(R.string.ficha_cedula_vencida, vence));
            tvDocumento.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_move));
            tvDocumento.setVisibility(View.VISIBLE);
        } else if (numero != null && !numero.isEmpty()) {
            tvDocumento.setText(getString(R.string.ficha_fmt_documento, numero, texto(vence)));
            tvDocumento.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
            tvDocumento.setVisibility(View.VISIBLE);
        } else {
            tvDocumento.setVisibility(View.GONE);
        }
    }

    // Pacientes creados antes de la versión 2.0 no tienen sexo/relación: mostramos "-"
    private String texto(String valor) {
        return valor == null || valor.isEmpty() ? "-" : valor;
    }

    // =====================================================================
    // NUEVO: registros diarios del paciente (subcolección en Firestore)
    // =====================================================================
    private void abrirRegistros() {
        Intent intent = new Intent(this, RegistrosActivity.class);
        intent.putExtra(PacientesActivity.EXTRA_PACIENTE_ID, idPaciente);
        startActivity(intent);
    }

    // =====================================================================
    // UPDATE: abre el formulario con el ID (Intent explícito con extra)
    // =====================================================================
    private void editarPaciente() {
        Intent intent = new Intent(this, FormularioPacienteActivity.class);
        intent.putExtra(PacientesActivity.EXTRA_PACIENTE_ID, idPaciente);
        startActivity(intent);
    }

    // =====================================================================
    // DELETE: confirma antes de borrar (no se puede deshacer)
    // =====================================================================
    private void confirmarEliminar() {
        if (pacienteActual == null) return; // aún no cargan los datos
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialogo_eliminar_titulo)
                .setMessage(getString(R.string.dialogo_eliminar_mensaje, pacienteActual.getNombre()))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.si_eliminar, (d, w) -> {
                    eliminando = true;
                    viewModel.eliminar(pacienteActual);
                })
                .show();
    }

    // =====================================================================
    // INTENT #4 (IMPLÍCITO): Llamada.
    // ACTION_DIAL solo abre el marcador con el número escrito: no requiere permiso CALL_PHONE.
    // =====================================================================
    private void llamarEmergencia() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL,
                Uri.parse(getString(R.string.intent_tel_emergencia)));
        iniciarSeguro(callIntent);
    }

    // =====================================================================
    // NUEVO (IMPLÍCITO): llamar al contacto de emergencia guardado en Firestore.
    // =====================================================================
    private void llamarContacto() {
        if (pacienteActual == null) return;
        Intent intent = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + pacienteActual.getContactoTelefono()));
        iniciarSeguro(intent);
    }

    // =====================================================================
    // INTENT #5 (IMPLÍCITO): Correo.
    // ACTION_SENDTO + mailto: filtra para que solo respondan clientes de correo reales.
    // MEJORA: el asunto y el cuerpo llevan la ficha del paciente leída desde Firestore.
    // =====================================================================
    private void enviarCorreo() {
        if (pacienteActual == null) return;
        Paciente p = pacienteActual;
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                Uri.parse(getString(R.string.intent_mailto_cuidador)));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject_paciente, p.getNombre()));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_cuerpo_paciente,
                p.getNombre(), p.getRut(), p.getEdad(), p.getTipoSangre(), p.getAlergias(),
                p.getContactoNombre(), p.getContactoTelefono(), Condiciones.texto(p.getCondiciones())));
        iniciarSeguro(emailIntent);
    }

    // =====================================================================
    // INTENT #6 (IMPLÍCITO): Cámara. Primero se valida hardware y permiso.
    // =====================================================================
    private void tomarFoto() {
        // Validación 1: ¿el equipo tiene cámara? (el Manifest la declara como no obligatoria)
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            mostrarMensaje(R.string.err_sin_camara);
            return;
        }
        // Validación 2: ¿tenemos el permiso en tiempo de ejecución?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    /** Lanza la cámara esperando resultado. Si no hay app de cámara, avisa en vez de crashear. */
    private void abrirCamara() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            lanzadorCamara.launch(cameraIntent);
        } catch (ActivityNotFoundException e) {
            mostrarMensaje(R.string.err_no_app);
        }
    }

    /**
     * Saca la miniatura (Bitmap) del resultado de la cámara, o null si no viene.
     * MEJORA: Se evita el uso exclusivo del método deprecated, bifurcando
     * según la versión del sistema operativo (API 33+).
     */
    @SuppressWarnings("deprecation")
    private Bitmap extraerMiniatura(Intent datos) {
        if (datos == null || datos.getExtras() == null) return null;

        Bitmap miniatura = null;

        // A partir de Android 13 (Tiramisu, API 33), se debe usar el método con clase explícita
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            miniatura = datos.getExtras().getParcelable("data", Bitmap.class);
        } else {
            // Para versiones anteriores, mantenemos la lógica clásica
            Object dato = datos.getExtras().get("data");
            if (dato instanceof Bitmap) {
                miniatura = (Bitmap) dato;
            }
        }

        return miniatura;
    }
}