package com.santo_tomas.cuidapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.santo_tomas.cuidapp.model.DocumentoIdentidad;
import com.santo_tomas.cuidapp.model.Paciente;
import com.santo_tomas.cuidapp.util.CedulaParser;
import com.santo_tomas.cuidapp.util.Condiciones;
import com.santo_tomas.cuidapp.util.Fechas;
import com.santo_tomas.cuidapp.util.Formatos;
import com.santo_tomas.cuidapp.util.Mascaras;
import com.santo_tomas.cuidapp.util.Textos;
import com.santo_tomas.cuidapp.util.RutUtil;
import com.santo_tomas.cuidapp.viewmodel.PacienteViewModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * NUEVO - VISTA: formulario que sirve para DOS operaciones:
 *   - CREATE: se abre SIN id  -> "Nuevo Paciente"
 *   - UPDATE: se abre CON id  -> "Editar Paciente" (carga los datos desde Firestore)
 *
 * MEJORAS DESPUÉS DE LA PRUEBA EN EL CELULAR:
 *   - Máscaras: el RUT queda "12345678-5" y la estatura "1,80" mientras se escribe.
 *   - Topes de caracteres (maxLength) en edad, peso, estatura y teléfono.
 *   - Teléfono con el prefijo "+56 9" fijo: solo se escriben los 8 dígitos.
 *   - Botón Cancelar (y botón atrás) con confirmación para no perder lo escrito.
 *   - ESCANEAR CÉDULA: foto -> ML Kit reconoce el texto EN EL TELÉFONO -> CedulaParser
 *     saca los datos -> se llenan los campos. El usuario revisa y guarda.
 */
public class FormularioPacienteActivity extends BaseActivity {

    private static final String TAG = "FormularioPaciente";
    private static final String ESTADO_CAMPOS_LLENOS = "campos_llenos";
    private static final String ESTADO_URI_FOTO = "uri_foto";

    private TextInputLayout tilNombre, tilRut, tilEdad, tilSexo, tilPeso, tilEstatura, tilSangre,
            tilAlergias, tilContactoNombre, tilRelacion, tilContactoTelefono,
            tilNacionalidad, tilFechaNacimiento, tilNumeroDocumento, tilFechaEmision, tilFechaVencimiento;
    private TextInputEditText etNombre, etRut, etEdad, etPeso, etEstatura, etAlergias,
            etContactoNombre, etContactoTelefono,
            etNacionalidad, etFechaNacimiento, etNumeroDocumento, etFechaEmision, etFechaVencimiento;
    private MaterialAutoCompleteTextView etSexo, etSangre, etRelacion;
    private MaterialCheckBox cbHipertension, cbDiabetes, cbEpoc, cbErc;
    private MaterialButton btnGuardar, btnEscanear;
    private ProgressBar progreso;

    private PacienteViewModel viewModel;
    private String idPaciente;           // null = crear, con valor = editar
    private boolean camposLlenos = false; // para no sobrescribir lo que el usuario ya escribió
    private Uri uriFoto;                  // dónde la cámara guarda la foto de la cédula

    // --- Escaneo de cédula: 3 "lanzadores" (Activity Result API) ---

    /** La app de cámara guarda la foto en uriFoto (resolución completa, mejor para leer texto). */
    private final ActivityResultLauncher<Uri> tomarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
                if (Boolean.TRUE.equals(ok) && uriFoto != null) leerCedula(uriFoto);
            });

    /** Alternativa: elegir una foto ya tomada (útil en el emulador). */
    private final ActivityResultLauncher<String> elegirImagen =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) leerCedula(uri);
            });

    /** El Manifest declara CAMERA, así que hay que pedir el permiso antes de abrir la cámara. */
    private final ActivityResultLauncher<String> permisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) abrirCamara();
                else mostrarMensaje(R.string.msg_camara_denegada);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario_paciente);
        InsetsUtil.aplicar(this);

        viewModel = new ViewModelProvider(this).get(PacienteViewModel.class);
        idPaciente = getIntent().getStringExtra(PacientesActivity.EXTRA_PACIENTE_ID);
        boolean esEdicion = idPaciente != null;
        if (savedInstanceState != null) {
            camposLlenos = savedInstanceState.getBoolean(ESTADO_CAMPOS_LLENOS, false);
            uriFoto = savedInstanceState.getParcelable(ESTADO_URI_FOTO);
        }

        enlazarVistas();
        configurarMascaras();

        TextView tvTitulo = findViewById(R.id.tvTituloFormulario);
        tvTitulo.setText(esEdicion ? R.string.form_titulo_editar : R.string.form_titulo_nuevo);
        btnGuardar.setText(esEdicion ? R.string.btn_actualizar : R.string.btn_guardar);

        // Menús desplegables (las opciones están en strings.xml)
        etSexo.setAdapter(lista(R.array.sexos));
        etSangre.setAdapter(lista(R.array.tipos_sangre));
        etRelacion.setAdapter(lista(R.array.relaciones));

        // --- Observadores ---
        viewModel.getMensaje().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        viewModel.getCargando().observe(this, cargando -> {
            progreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
            btnGuardar.setEnabled(!cargando); // evita guardar dos veces
        });

        // Firestore confirmó el guardado -> cerramos el formulario
        viewModel.getGuardado().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) finish();
        });

        // EDITAR: llenamos los campos con los datos de la nube (solo UNA vez)
        viewModel.getPacienteSeleccionado().observe(this, p -> {
            if (p == null || camposLlenos) return;
            llenarCampos(p);
            camposLlenos = true;
        });

        if (esEdicion) {
            viewModel.cargarPaciente(idPaciente);
        }

        configurarBoton(R.id.btnGuardar, v -> guardar());
        configurarBoton(R.id.btnEscanearCedula, v -> elegirOrigenFoto());
        configurarBoton(R.id.btnCancelarFormulario, v -> confirmarSalir());
        configurarBoton(R.id.btnVolverFormulario, v -> confirmarSalir());

        // El botón/gesto "atrás" del sistema también pregunta antes de perder lo escrito
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmarSalir();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_CAMPOS_LLENOS, camposLlenos);
        if (uriFoto != null) outState.putParcelable(ESTADO_URI_FOTO, uriFoto);
    }

    private ArrayAdapter<String> lista(int arrayId) {
        return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(arrayId));
    }

    private void enlazarVistas() {
        tilNombre = findViewById(R.id.tilNombre);
        tilRut = findViewById(R.id.tilRut);
        tilEdad = findViewById(R.id.tilEdad);
        tilSexo = findViewById(R.id.tilSexo);
        tilPeso = findViewById(R.id.tilPeso);
        tilEstatura = findViewById(R.id.tilEstatura);
        tilSangre = findViewById(R.id.tilSangre);
        tilAlergias = findViewById(R.id.tilAlergias);
        tilContactoNombre = findViewById(R.id.tilContactoNombre);
        tilRelacion = findViewById(R.id.tilRelacion);
        tilContactoTelefono = findViewById(R.id.tilContactoTelefono);
        tilNacionalidad = findViewById(R.id.tilNacionalidad);
        tilFechaNacimiento = findViewById(R.id.tilFechaNacimiento);
        tilNumeroDocumento = findViewById(R.id.tilNumeroDocumento);
        tilFechaEmision = findViewById(R.id.tilFechaEmision);
        tilFechaVencimiento = findViewById(R.id.tilFechaVencimiento);
        etNombre = findViewById(R.id.etNombre);
        etRut = findViewById(R.id.etRut);
        etEdad = findViewById(R.id.etEdad);
        etSexo = findViewById(R.id.etSexo);
        etPeso = findViewById(R.id.etPeso);
        etEstatura = findViewById(R.id.etEstatura);
        etSangre = findViewById(R.id.etSangre);
        etAlergias = findViewById(R.id.etAlergias);
        etContactoNombre = findViewById(R.id.etContactoNombre);
        etRelacion = findViewById(R.id.etRelacion);
        etContactoTelefono = findViewById(R.id.etContactoTelefono);
        etNacionalidad = findViewById(R.id.etNacionalidad);
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento);
        etNumeroDocumento = findViewById(R.id.etNumeroDocumento);
        etFechaEmision = findViewById(R.id.etFechaEmision);
        etFechaVencimiento = findViewById(R.id.etFechaVencimiento);
        cbHipertension = findViewById(R.id.cbHipertension);
        cbDiabetes = findViewById(R.id.cbDiabetes);
        cbEpoc = findViewById(R.id.cbEpoc);
        cbErc = findViewById(R.id.cbErc);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnEscanear = findViewById(R.id.btnEscanearCedula);
        progreso = findViewById(R.id.progreso);
    }

    // =====================================================================
    // MÁSCARAS: formato automático mientras se escribe
    // =====================================================================
    private void configurarMascaras() {
        // El error rojo se borra apenas el usuario corrige ese campo (antes quedaba
        // pegado hasta volver a presionar Guardar, aunque el dato ya estuviera bien)
        TextInputLayout[] tils = campos();
        for (TextInputLayout til : tils) limpiarErrorAlEditar(til);

        Mascaras.rut(etRut);                 // 123456785 -> 12345678-5
        Mascaras.estatura(etEstatura);       // 180 -> 1,80
        Mascaras.peso(etPeso);               // 11111 -> 111 (máx. 3 enteros y 1 decimal)
        Textos.bloquearNumeros(etNombre);          // los nombres no llevan números
        Textos.bloquearNumeros(etContactoNombre);
        Textos.bloquearNumeros(etAlergias);        // las alergias tampoco
        Textos.bloquearNumeros(etNacionalidad);    // ni la nacionalidad
        Mascaras.fecha(etFechaNacimiento);   // 15011950 -> 15/01/1950
        Mascaras.fecha(etFechaEmision);
        Mascaras.fecha(etFechaVencimiento);

        // Aviso inmediato si el valor ya pasó el máximo (no espera a "Guardar")
        limiteEnVivo(tilEdad, etEdad, 120, "La edad máxima es 120 años");
        limiteEnVivo(tilPeso, etPeso, 300, "El peso máximo es 300 kg");
        limiteEnVivo(tilEstatura, etEstatura, 2.5, "La estatura máxima es 2,50 m");

        // Si se escribe (o escanea) la fecha de nacimiento, la edad se calcula sola
        etFechaNacimiento.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                int edad = Fechas.edad(s.toString());
                if (edad >= 0 && edad <= 120) etEdad.setText(String.valueOf(edad));
            }
        });
    }

    /** Todos los campos del formulario, en el orden en que aparecen en pantalla. */
    private TextInputLayout[] campos() {
        return new TextInputLayout[]{tilNombre, tilRut, tilEdad, tilSexo, tilPeso, tilEstatura,
                tilSangre, tilAlergias, tilNacionalidad, tilFechaNacimiento, tilNumeroDocumento,
                tilFechaEmision, tilFechaVencimiento, tilContactoNombre, tilRelacion,
                tilContactoTelefono};
    }

    private void limpiarErrorAlEditar(TextInputLayout til) {
        if (til.getEditText() == null) return;
        til.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (til.getError() != null) til.setError(null);
            }
        });
    }

    /** Lleva al usuario al primer campo con error (el ScrollView baja o sube solo). */
    private void enfocarPrimerError() {
        for (TextInputLayout til : campos()) {
            if (til.getError() != null && til.getEditText() != null) {
                til.getEditText().requestFocus();
                return;
            }
        }
    }

    /**
     * Muestra el error apenas el número supera el máximo. Solo revisa el máximo, porque
     * mientras se escribe un número válido pasa por valores chicos (1 -> 17 -> 172).
     */
    private void limiteEnVivo(TextInputLayout til, TextInputEditText et, double max, String mensaje) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                double valor;
                try {
                    valor = Double.parseDouble(s.toString().trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    return;     // vacío o incompleto: se revisa al guardar
                }
                if (valor > max) til.setError(mensaje);
                else if (til.getError() != null) til.setError(null);
            }
        });
    }

    private void llenarCampos(Paciente p) {
        etNombre.setText(p.getNombre());
        etRut.setText(p.getRut());
        etEdad.setText(String.valueOf(p.getEdad()));
        etSexo.setText(p.getSexo(), false);       // false = no filtrar la lista
        if (p.getPeso() > 0) etPeso.setText(formatoPeso(p.getPeso()));
        if (p.getEstatura() > 0) etEstatura.setText(Formatos.estaturaMetros(p.getEstatura()));
        etSangre.setText(p.getTipoSangre(), false);
        etAlergias.setText(p.getAlergias());
        cbHipertension.setChecked(p.tieneCondicion(Condiciones.HIPERTENSION));
        cbDiabetes.setChecked(p.tieneCondicion(Condiciones.DIABETES));
        cbEpoc.setChecked(p.tieneCondicion(Condiciones.EPOC));
        cbErc.setChecked(p.tieneCondicion(Condiciones.ERC));
        etContactoNombre.setText(p.getContactoNombre());
        etRelacion.setText(p.getContactoRelacion(), false);
        etContactoTelefono.setText(Formatos.telefonoLocal(p.getContactoTelefono()));
        etNacionalidad.setText(p.getNacionalidad());
        etFechaNacimiento.setText(p.getFechaNacimiento());
        etNumeroDocumento.setText(p.getNumeroDocumento());
        etFechaEmision.setText(p.getFechaEmision());
        etFechaVencimiento.setText(p.getFechaVencimiento());
    }

    // 72.0 -> "72" y 72.5 -> "72,5" (para que el campo no muestre decimales innecesarios)
    private String formatoPeso(double peso) {
        return peso == Math.floor(peso)
                ? String.valueOf((int) peso)
                : String.format(Locale.US, "%.1f", peso).replace('.', ',');
    }

    // Lee los checkbox y arma la lista de claves que se guarda en Firestore
    private List<String> condicionesMarcadas() {
        List<String> lista = new ArrayList<>();
        if (cbHipertension.isChecked()) lista.add(Condiciones.HIPERTENSION);
        if (cbDiabetes.isChecked()) lista.add(Condiciones.DIABETES);
        if (cbEpoc.isChecked()) lista.add(Condiciones.EPOC);
        if (cbErc.isChecked()) lista.add(Condiciones.ERC);
        return lista;
    }

    private void guardar() {
        if (!validar()) {
            Toast.makeText(this, R.string.form_revisar, Toast.LENGTH_SHORT).show();
            enfocarPrimerError();
            return;
        }

        DocumentoIdentidad documento = new DocumentoIdentidad(
                texto(etNacionalidad), texto(etFechaNacimiento), texto(etNumeroDocumento),
                texto(etFechaEmision), texto(etFechaVencimiento));

        viewModel.guardar(
                idPaciente,
                texto(etNombre),
                texto(etRut),
                Integer.parseInt(texto(etEdad)),
                etSexo.getText().toString().trim(),
                Double.parseDouble(texto(etPeso).replace(',', '.')),
                Formatos.estaturaCm(texto(etEstatura)),           // "1,80" -> 180 cm
                etSangre.getText().toString().trim(),
                condicionesMarcadas(),
                texto(etAlergias),
                texto(etContactoNombre),
                etRelacion.getText().toString().trim(),
                Formatos.telefonoParaGuardar(texto(etContactoTelefono)), // -> +569XXXXXXXX
                documento);
    }

    // =====================================================================
    // CANCELAR: si hay algo escrito, se pregunta antes de salir
    // =====================================================================
    private void confirmarSalir() {
        if (!hayDatosEscritos()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialogo_descartar_titulo)
                .setMessage(R.string.dialogo_descartar_mensaje)
                .setNegativeButton(R.string.seguir_editando, null)
                .setPositiveButton(R.string.descartar, (d, w) -> finish())
                .show();
    }

    private boolean hayDatosEscritos() {
        TextInputEditText[] campos = {etNombre, etRut, etEdad, etPeso, etEstatura, etAlergias,
                etContactoNombre, etContactoTelefono, etNacionalidad, etFechaNacimiento};
        for (TextInputEditText c : campos) if (!texto(c).isEmpty()) return true;
        return false;
    }

    // =====================================================================
    // ESCANEAR CÉDULA (cámara o galería -> ML Kit -> CedulaParser -> campos)
    // =====================================================================
    private void elegirOrigenFoto() {
        String[] opciones = {getString(R.string.escanear_camara), getString(R.string.escanear_galeria)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.escanear_titulo)
                .setItems(opciones, (d, cual) -> {
                    if (cual == 0) pedirCamara();
                    else elegirImagen.launch("image/*");
                })
                .show();
    }

    private void pedirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            permisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * La foto se guarda en la caché de la app y se comparte con la cámara mediante un FileProvider
     * (Android no permite pasar rutas "file://" a otras apps).
     */
    private void abrirCamara() {
        File carpeta = new File(getCacheDir(), "fotos");
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            mostrarMensaje(R.string.err_foto);
            return;
        }
        File archivo = new File(carpeta, "cedula.jpg");
        uriFoto = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
        try {
            tomarFoto.launch(uriFoto);
        } catch (ActivityNotFoundException e) {
            mostrarMensaje(R.string.err_no_app);
        }
    }

    /** ML Kit reconoce el texto SIN internet (el modelo viene dentro de la app). */
    private void leerCedula(Uri uri) {
        InputImage imagen;
        try {
            imagen = InputImage.fromFilePath(this, uri);
        } catch (IOException e) {
            Log.w(TAG, "No se pudo abrir la imagen", e);
            mostrarMensaje(R.string.err_foto);
            return;
        }

        progreso.setVisibility(View.VISIBLE);
        btnEscanear.setEnabled(false);
        Toast.makeText(this, R.string.escanear_leyendo, Toast.LENGTH_SHORT).show();

        TextRecognizer reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        reconocedor.process(imagen)
                .addOnSuccessListener(texto -> aplicarDatosCedula(CedulaParser.leer(texto.getText())))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error de ML Kit", e);
                    mostrarMensaje(R.string.escanear_sin_datos);
                })
                .addOnCompleteListener(t -> {
                    progreso.setVisibility(View.GONE);
                    btnEscanear.setEnabled(true);
                    reconocedor.close(); // libera el modelo
                });
    }

    private void aplicarDatosCedula(CedulaParser.Resultado r) {
        if (r.cantidad() == 0) {
            mostrarMensaje(R.string.escanear_sin_datos);
            return;
        }
        String nombre = r.nombreCompleto();
        if (!nombre.isEmpty()) etNombre.setText(nombre.length() > 60 ? nombre.substring(0, 60) : nombre);
        if (!r.run.isEmpty()) etRut.setText(r.run);
        if (!r.sexo.isEmpty()) etSexo.setText(r.sexo, false);
        if (!r.nacionalidad.isEmpty()) etNacionalidad.setText(r.nacionalidad);
        if (!r.fechaNacimiento.isEmpty()) etFechaNacimiento.setText(r.fechaNacimiento); // calcula la edad
        if (!r.numeroDocumento.isEmpty()) etNumeroDocumento.setText(r.numeroDocumento);
        if (!r.fechaEmision.isEmpty()) etFechaEmision.setText(r.fechaEmision);
        if (!r.fechaVencimiento.isEmpty()) etFechaVencimiento.setText(r.fechaVencimiento);

        Toast.makeText(this, getString(R.string.escanear_ok, r.cantidad()), Toast.LENGTH_LONG).show();
        if (r.run.isEmpty() && r.runConError) mostrarMensaje(R.string.escanear_rut_invalido);
    }

    // =====================================================================
    // VALIDACIONES: cada error aparece debajo de su campo (TextInputLayout.setError)
    // =====================================================================
    private boolean validar() {
        boolean ok = true;

        // Nombre: obligatorio, sin números, 3 a 60 caracteres, solo letras y espacios
        String errorNombre = Textos.errorNombre(texto(etNombre), "El nombre es obligatorio");
        if (errorNombre != null) ok = error(tilNombre, errorNombre);
        else tilNombre.setError(null);

        // RUT: formato 12345678-5 (7 u 8 dígitos, guion y dígito verificador) y módulo 11
        String rut = texto(etRut);
        if (TextUtils.isEmpty(rut)) {
            ok = error(tilRut, "El RUT es obligatorio");
        } else if (!rut.matches("\\d{7,8}-[\\dK]")) {
            ok = error(tilRut, "Formato: 12345678-5");
        } else if (!RutUtil.esValido(rut)) {
            ok = error(tilRut, "RUT inválido: revisa el dígito verificador");
        } else {
            tilRut.setError(null);
        }

        // Edad: número entero entre 0 y 120 años
        if (!validarEntero(tilEdad, texto(etEdad), 0, 120, "La edad es obligatoria")) ok = false;

        // Sexo: debe elegirse de la lista
        if (!validarSeleccion(tilSexo, etSexo, "Selecciona el sexo")) ok = false;

        // Peso: decimal entre 2 y 300 kg (acepta coma o punto, un solo separador)
        String pesoTexto = texto(etPeso).replace(',', '.');
        if (TextUtils.isEmpty(pesoTexto)) {
            ok = error(tilPeso, "El peso es obligatorio");
        } else if (!pesoTexto.matches("\\d{1,3}(\\.\\d)?")) {
            ok = error(tilPeso, "Ingresa el peso así: 72 o 72,5");
        } else {
            double peso = Double.parseDouble(pesoTexto);
            if (peso < 2 || peso > 300) ok = error(tilPeso, "Ingresa un peso real, entre 2 y 300 kg");
            else tilPeso.setError(null);
        }

        // Estatura: en metros "1,80" -> entre 0,30 y 2,50
        String estatura = texto(etEstatura);
        int cm = Formatos.estaturaCm(estatura);
        if (TextUtils.isEmpty(estatura)) {
            ok = error(tilEstatura, "La estatura es obligatoria");
        } else if (cm < 30 || cm > 250) {
            ok = error(tilEstatura, "Ingresa una estatura entre 0,30 y 2,50 m");
        } else {
            tilEstatura.setError(null);
        }

        // Tipo de sangre: debe elegirse de la lista
        if (!validarSeleccion(tilSangre, etSangre, "Selecciona el tipo de sangre")) ok = false;

        // Alergias: opcional, sin números, máximo 100 caracteres
        String errorAlergias = Textos.errorAlergias(texto(etAlergias));
        if (errorAlergias != null) ok = error(tilAlergias, errorAlergias);
        else tilAlergias.setError(null);

        // Documento de identidad: todo opcional, pero si se escribe debe tener formato correcto
        String errorNacionalidad = Textos.errorNacionalidad(texto(etNacionalidad));
        if (errorNacionalidad != null) ok = error(tilNacionalidad, errorNacionalidad);
        else tilNacionalidad.setError(null);
        String nacimiento = texto(etFechaNacimiento);
        if (!fechaOpcionalValida(tilFechaNacimiento, nacimiento)) {
            ok = false;
        } else if (Fechas.esFutura(nacimiento)) {
            ok = error(tilFechaNacimiento, "La fecha de nacimiento no puede ser futura");
        }
        String documento = texto(etNumeroDocumento);
        if (!documento.isEmpty() && !documento.replace(".", "").matches("\\d{9}")) {
            ok = error(tilNumeroDocumento, "Debe tener 9 dígitos (ej: 123.456.789)");
        } else {
            tilNumeroDocumento.setError(null);
        }
        if (!fechaOpcionalValida(tilFechaEmision, texto(etFechaEmision))) ok = false;
        if (!fechaOpcionalValida(tilFechaVencimiento, texto(etFechaVencimiento))) ok = false;

        // Condiciones: opcionales (sin ninguna = "Monitoreo general")

        // Contacto: nombre obligatorio
        String errorContacto = Textos.errorNombre(texto(etContactoNombre),
                "El contacto de emergencia es obligatorio");
        if (errorContacto != null) ok = error(tilContactoNombre, errorContacto);
        else tilContactoNombre.setError(null);

        // Relación con el paciente: debe elegirse de la lista
        if (!validarSeleccion(tilRelacion, etRelacion, "Selecciona la relación")) ok = false;

        // Celular: el prefijo +56 9 ya está; se escriben exactamente 8 dígitos
        String telefono = texto(etContactoTelefono);
        if (TextUtils.isEmpty(telefono)) {
            ok = error(tilContactoTelefono, "El celular es obligatorio");
        } else if (!telefono.matches("\\d{8}")) {
            ok = error(tilContactoTelefono, "Deben ser 8 dígitos después de +56 9");
        } else {
            tilContactoTelefono.setError(null);
        }

        return ok;
    }

    // Fecha opcional: vacía está bien; si tiene algo, debe ser una fecha real dd/mm/aaaa
    private boolean fechaOpcionalValida(TextInputLayout campo, String valor) {
        if (!valor.isEmpty() && !Fechas.esValida(valor)) {
            return error(campo, "Fecha inválida (dd/mm/aaaa)");
        }
        campo.setError(null);
        return true;
    }

    // Valida un número entero obligatorio dentro de un rango [min, max]
    private boolean validarEntero(TextInputLayout campo, String valor, int min, int max, String siVacio) {
        if (TextUtils.isEmpty(valor)) {
            return error(campo, siVacio);
        }
        try {
            int numero = Integer.parseInt(valor);
            if (numero < min || numero > max) {
                return error(campo, "Ingresa un valor entre " + min + " y " + max);
            }
        } catch (NumberFormatException e) {
            return error(campo, "Ingresa solo números enteros");
        }
        campo.setError(null);
        return true;
    }

    // Valida que en un menú desplegable se haya elegido una opción
    private boolean validarSeleccion(TextInputLayout campo, MaterialAutoCompleteTextView menu, String mensaje) {
        if (TextUtils.isEmpty(menu.getText().toString().trim())) {
            return error(campo, mensaje);
        }
        campo.setError(null);
        return true;
    }

    // Muestra el error y devuelve false (así se puede escribir: ok = error(...))
    private boolean error(TextInputLayout campo, String mensaje) {
        campo.setError(mensaje);
        return false;
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
