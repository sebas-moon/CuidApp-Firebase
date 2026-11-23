package com.santo_tomas.cuidapp.repositorio;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/*
 * REPOSITORIO DE AUTENTICACIÓN: es la ÚNICA clase que habla con Firebase Authentication.
 *
 * Firebase Authentication guarda el correo y la contraseña (cifrada) en los servidores de Google.
 * NOSOTROS NUNCA guardamos la contraseña en Firestore: solo datos del perfil del cuidador.
 */
public class AuthRepositorio {

    private static final String TAG = "AuthRepositorio";
    private static final String COLECCION_USUARIOS = "usuarios";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Usuario conectado actualmente (null si nadie inició sesión).
    // Firebase recuerda la sesión aunque se cierre la app.
    public FirebaseUser getUsuarioActual() {
        return auth.getCurrentUser();
    }

    // REGISTRO con correo y contraseña
    public void registrar(String nombre, String correo, String clave,
                          MutableLiveData<Boolean> exito,
                          MutableLiveData<String> mensaje,
                          MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        auth.createUserWithEmailAndPassword(correo, clave)
                .addOnSuccessListener(resultado -> {
                    FirebaseUser usuario = resultado.getUser();
                    if (usuario == null) {
                        cargando.setValue(false);
                        mensaje.setValue("No se pudo crear la cuenta");
                        return;
                    }
                    Log.d(TAG, "Usuario creado con UID: " + usuario.getUid());

                    // 1) Guardamos el nombre en el perfil de Authentication (displayName)
                    UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre)
                            .build();
                    usuario.updateProfile(perfil);

                    // 2) Creamos el documento usuarios/{UID} SIN contraseña.
                    //    El ID del documento ES el UID: así se relacionan Authentication y Firestore.
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("nombre", nombre);
                    datos.put("correo", correo);
                    datos.put("fechaCreacion", FieldValue.serverTimestamp());

                    db.collection(COLECCION_USUARIOS).document(usuario.getUid())
                            .set(datos)
                            .addOnCompleteListener(tarea -> {
                                cargando.setValue(false);
                                mensaje.setValue("Cuenta creada. ¡Bienvenido a CuidApp, " + nombre + "!");
                                exito.setValue(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al registrar", e);
                    cargando.setValue(false);
                    mensaje.setValue(traducirError(e));
                });
    }

    // INICIO DE SESIÓN con correo y contraseña
    public void iniciarSesion(String correo, String clave,
                              MutableLiveData<Boolean> exito,
                              MutableLiveData<String> mensaje,
                              MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        auth.signInWithEmailAndPassword(correo, clave)
                .addOnSuccessListener(resultado -> {
                    cargando.setValue(false);
                    mensaje.setValue("Sesión iniciada");
                    exito.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al iniciar sesión", e);
                    cargando.setValue(false);
                    mensaje.setValue(traducirError(e));
                });
    }

    // CERRAR SESIÓN: borra la sesión guardada en el teléfono
    public void cerrarSesion() {
        auth.signOut();
    }

    // Convierte los errores de Firebase (en inglés) a mensajes claros para el usuario
    private String traducirError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Ese correo ya está registrado";
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es muy débil (mínimo 6 caracteres)";
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            return "No existe una cuenta con ese correo";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Correo o contraseña incorrectos";
        } else if (e instanceof FirebaseNetworkException) {
            return "Sin conexión a internet";
        }
        return "Error: " + e.getMessage();
    }
}
