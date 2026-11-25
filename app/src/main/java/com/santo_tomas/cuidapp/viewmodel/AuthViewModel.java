package com.santo_tomas.cuidapp.viewmodel;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.santo_tomas.cuidapp.repositorio.AuthRepositorio;
import com.santo_tomas.cuidapp.util.Textos;

/*
 * VIEWMODEL de Login, Registro y cierre de sesión.
 * - Valida los datos ANTES de llamar a Firebase (no gastamos peticiones con datos malos).
 * - Expone LiveData que las Activities "observan" para reaccionar.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepositorio repositorio = new AuthRepositorio();

    private final MutableLiveData<Boolean> autenticado = new MutableLiveData<>();
    private final MutableLiveData<String> mensaje = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    // --- Getters para que la Vista observe (solo lectura: LiveData, no MutableLiveData) ---
    public LiveData<Boolean> getAutenticado() { return autenticado; }
    public LiveData<String> getMensaje() { return mensaje; }
    public LiveData<Boolean> getCargando() { return cargando; }

    // ¿Ya hay una sesión abierta? (Firebase la recuerda entre aperturas de la app)
    public boolean haySesionActiva() {
        return repositorio.getUsuarioActual() != null;
    }

    // Usuario conectado (para saludarlo por su nombre en el menú)
    public FirebaseUser getUsuario() {
        return repositorio.getUsuarioActual();
    }

    // CERRAR SESIÓN
    public void cerrarSesion() {
        repositorio.cerrarSesion();
    }

    public void iniciarSesion(String correo, String clave) {
        if (correo.isEmpty() || clave.isEmpty()) {
            mensaje.setValue("Ingresa tu correo y contraseña");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mensaje.setValue("El formato del correo no es válido");
            return;
        }
        repositorio.iniciarSesion(correo, clave, autenticado, mensaje, cargando);
    }

    public void registrar(String nombre, String correo, String clave, String confirmacion) {
        if (nombre.isEmpty() || correo.isEmpty() || clave.isEmpty() || confirmacion.isEmpty()) {
            mensaje.setValue("Completa todos los campos");
            return;
        }
        String errorNombre = Textos.errorNombre(nombre, "Escribe tu nombre");
        if (errorNombre != null) {
            mensaje.setValue(errorNombre);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mensaje.setValue("El formato del correo no es válido");
            return;
        }
        if (clave.length() < 6) {
            mensaje.setValue("La contraseña debe tener al menos 6 caracteres");
            return;
        }
        if (!clave.equals(confirmacion)) {
            mensaje.setValue("Las contraseñas no coinciden");
            return;
        }
        repositorio.registrar(nombre, correo, clave, autenticado, mensaje, cargando);
    }
}
