package com.santo_tomas.cuidapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.santo_tomas.cuidapp.model.RegistroDiario;
import com.santo_tomas.cuidapp.repositorio.RegistroRepositorio;
import com.santo_tomas.cuidapp.util.RangosClinicos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * NUEVO - VIEWMODEL de registros diarios.
 * Lo usan RegistrosActivity (lista) y FormularioRegistroActivity (crear / editar / ver).
 */
public class RegistroViewModel extends ViewModel {

    private final RegistroRepositorio repositorio = new RegistroRepositorio();

    private final MutableLiveData<List<RegistroDiario>> registros = new MutableLiveData<>();
    private final MutableLiveData<RegistroDiario> registro = new MutableLiveData<>();
    private final MutableLiveData<Boolean> existe = new MutableLiveData<>();
    private final MutableLiveData<String> mensaje = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> guardado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> eliminado = new MutableLiveData<>();

    private ListenerRegistration escucha;

    public LiveData<List<RegistroDiario>> getRegistros() { return registros; }
    public LiveData<RegistroDiario> getRegistro() { return registro; }
    public LiveData<Boolean> getExiste() { return existe; }
    public LiveData<String> getMensaje() { return mensaje; }
    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<Boolean> getGuardado() { return guardado; }
    public LiveData<Boolean> getEliminado() { return eliminado; }

    /** Fecha de hoy en formato yyyy-MM-dd (es también el ID del documento). */
    public static String hoy() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** "2026-11-18" -> "18/11/2026" (solo para mostrar; en Firestore queda yyyy-MM-dd). */
    public static String fechaLegible(String fecha) {
        if (fecha == null || fecha.length() != 10) return fecha == null ? "" : fecha;
        return fecha.substring(8, 10) + "/" + fecha.substring(5, 7) + "/" + fecha.substring(0, 4);
    }

    private FirebaseUser usuario() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    // READ lista
    public void cargarRegistros(String pacienteId) {
        FirebaseUser u = usuario();
        if (u == null || escucha != null) return;
        escucha = repositorio.escucharRegistros(pacienteId, u.getUid(), registros, mensaje);
    }

    // READ uno (si no existe, el formulario queda en modo "nuevo")
    public void cargarRegistro(String pacienteId, String fecha) {
        repositorio.obtener(pacienteId, fecha, registro, existe, mensaje);
    }

    // CREATE o UPDATE
    public void guardar(RegistroDiario r, boolean esNuevo) {
        FirebaseUser u = usuario();
        if (u == null) {
            mensaje.setValue("Tu sesión expiró, vuelve a iniciar sesión");
            return;
        }
        if (!esNuevo) {
            RegistroDiario original = registro.getValue();
            if (original != null && !original.esEditable()) {
                mensaje.setValue("🔒 Este registro tiene más de 24 horas y es de solo lectura");
                return;
            }
        }
        r.setUid(u.getUid());
        r.setAlertas(RangosClinicos.calcularAlertas(r)); // revisa los rangos clínicos

        if (esNuevo) repositorio.crear(r, guardado, mensaje, cargando);
        else repositorio.actualizar(r, guardado, mensaje, cargando);
    }

    // DELETE
    public void eliminar(RegistroDiario r) {
        if (!r.esEditable()) {
            mensaje.setValue("🔒 Los registros con más de 24 horas no se pueden eliminar");
            return;
        }
        repositorio.eliminar(r, eliminado, mensaje);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (escucha != null) escucha.remove();
    }
}
