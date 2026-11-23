package com.santo_tomas.cuidapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.santo_tomas.cuidapp.model.DocumentoIdentidad;
import com.santo_tomas.cuidapp.model.Paciente;
import com.santo_tomas.cuidapp.repositorio.AuthRepositorio;
import com.santo_tomas.cuidapp.repositorio.PacienteRepositorio;
import com.santo_tomas.cuidapp.util.Fechas;
import com.santo_tomas.cuidapp.util.RutUtil;

import java.util.List;

/*
 * VIEWMODEL de pacientes: intermediario entre las pantallas y el repositorio.
 * Las Activities NUNCA llaman a Firestore directamente: le piden las cosas a este ViewModel.
 * Lo usan PacientesActivity (lista), FichaPacienteActivity (detalle) y FormularioPacienteActivity.
 */
public class PacienteViewModel extends ViewModel {

    private final PacienteRepositorio repositorio = new PacienteRepositorio();
    private final AuthRepositorio authRepositorio = new AuthRepositorio();

    private final MutableLiveData<List<Paciente>> pacientes = new MutableLiveData<>();
    private final MutableLiveData<Paciente> pacienteSeleccionado = new MutableLiveData<>();
    private final MutableLiveData<String> mensaje = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> guardado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> eliminado = new MutableLiveData<>();

    // "Escuchadores" en tiempo real; se guardan para poder detenerlos
    private ListenerRegistration escuchaLista;
    private ListenerRegistration escuchaPaciente;

    // --- Getters para la Vista (LiveData = solo lectura) ---
    public LiveData<List<Paciente>> getPacientes() { return pacientes; }
    public LiveData<Paciente> getPacienteSeleccionado() { return pacienteSeleccionado; }
    public LiveData<String> getMensaje() { return mensaje; }
    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<Boolean> getGuardado() { return guardado; }
    public LiveData<Boolean> getEliminado() { return eliminado; }

    // Usuario conectado (Firebase Authentication)
    public FirebaseUser getUsuario() {
        return authRepositorio.getUsuarioActual();
    }

    // READ: empezar a escuchar los pacientes del cuidador conectado
    public void cargarPacientes() {
        FirebaseUser usuario = getUsuario();
        if (usuario == null || escuchaLista != null) return; // sin sesión o ya escuchando
        escuchaLista = repositorio.escucharPacientes(usuario.getUid(), pacientes, mensaje, cargando);
    }

    // READ (uno): para la ficha y para llenar el formulario al editar
    public void cargarPaciente(String id) {
        if (escuchaPaciente != null) return;
        escuchaPaciente = repositorio.escucharPaciente(id, pacienteSeleccionado, mensaje);
    }

    // CREATE o UPDATE según si recibimos un ID o no
    public void guardar(String id, String nombre, String rut, int edad, String sexo, double peso,
                        int estatura, String tipoSangre, List<String> condiciones, String alergias,
                        String contactoNombre, String contactoRelacion, String contactoTelefono,
                        DocumentoIdentidad documento) {
        FirebaseUser usuario = getUsuario();
        if (usuario == null) {
            mensaje.setValue("Tu sesión expiró, vuelve a iniciar sesión");
            return;
        }
        // Validación de respaldo (la Activity ya validó campo por campo)
        if (nombre.isEmpty() || sexo.isEmpty() || tipoSangre.isEmpty()
                || contactoNombre.isEmpty() || contactoRelacion.isEmpty()) {
            mensaje.setValue("Completa todos los campos obligatorios");
            return;
        }
        if (!RutUtil.esValido(rut)) {
            mensaje.setValue("El RUT no es válido");
            return;
        }

        DocumentoIdentidad doc = documento == null ? new DocumentoIdentidad() : documento;
        if (!fechaVaciaOValida(doc.fechaNacimiento) || !fechaVaciaOValida(doc.fechaEmision)
                || !fechaVaciaOValida(doc.fechaVencimiento)) {
            mensaje.setValue("Revisa las fechas de la cédula (dd/mm/aaaa)");
            return;
        }

        // Datos normalizados: RUT con formato estándar y alergias "Ninguna" si va vacío
        String rutFormateado = RutUtil.formatear(rut);
        String alergiasFinal = alergias.isEmpty() ? "Ninguna" : alergias;

        // El UID del cuidador conectado queda guardado DENTRO del paciente
        Paciente paciente = new Paciente(usuario.getUid(), nombre, rutFormateado, edad, sexo,
                peso, estatura, tipoSangre, condiciones, alergiasFinal,
                contactoNombre, contactoRelacion, contactoTelefono);

        paciente.setNacionalidad(doc.nacionalidad);
        paciente.setFechaNacimiento(doc.fechaNacimiento);
        paciente.setNumeroDocumento(doc.numeroDocumento);
        paciente.setFechaEmision(doc.fechaEmision);
        paciente.setFechaVencimiento(doc.fechaVencimiento);

        if (id == null) {
            repositorio.crear(paciente, guardado, mensaje, cargando);        // C
        } else {
            paciente.setId(id);
            repositorio.actualizar(paciente, guardado, mensaje, cargando);   // U
        }
    }

    private boolean fechaVaciaOValida(String fecha) {
        return fecha == null || fecha.isEmpty() || Fechas.esValida(fecha);
    }

    // DELETE
    public void eliminar(Paciente paciente) {
        repositorio.eliminar(paciente, eliminado, mensaje);
    }

    // Cerrar sesión: dejamos de escuchar Firestore y salimos de Authentication
    public void cerrarSesion() {
        detenerEscuchas();
        authRepositorio.cerrarSesion();
    }

    private void detenerEscuchas() {
        if (escuchaLista != null) { escuchaLista.remove(); escuchaLista = null; }
        if (escuchaPaciente != null) { escuchaPaciente.remove(); escuchaPaciente = null; }
    }

    // Se llama automáticamente cuando la pantalla se destruye definitivamente
    @Override
    protected void onCleared() {
        super.onCleared();
        detenerEscuchas();
    }
}
