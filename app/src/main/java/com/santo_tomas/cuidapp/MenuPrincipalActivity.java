package com.santo_tomas.cuidapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.santo_tomas.cuidapp.viewmodel.AuthViewModel;

/**
 * Pantalla principal. Se llega aquí DESPUÉS de iniciar sesión (el Launcher ahora es LoginActivity).
 *
 * NUEVO CON FIREBASE:
 *  - Saluda al usuario conectado (FirebaseUser.getDisplayName()).
 *  - "Mis Pacientes" abre el CRUD en Cloud Firestore.
 *  - Botón "Cerrar sesión" (FirebaseAuth.signOut()).
 *
 * INTENTS EN ESTA CLASE:
 *  - #1 EXPLÍCITO: navegarA()            -> navegación hacia Mis Pacientes, Expedientes y Dashboard
 *  - #7 IMPLÍCITO: abrirMapaHospitales() -> ACTION_VIEW con esquema geo: (app de mapas)
 *  - #8 IMPLÍCITO: abrirPortalMinsal()   -> ACTION_VIEW con https: (navegador web)
 *
 * PERMISO EN TIEMPO DE EJECUCIÓN: ubicación (se pide al tocar "Buscar Hospital Cercano").
 */
public class MenuPrincipalActivity extends BaseActivity {

    private static final String TAG = "MenuPrincipal";

    private AuthViewModel authViewModel;

    /**
     * Launcher moderno (Activity Result API) para pedir permisos.
     * Debe declararse como campo ANTES de que la Activity llegue a STARTED.
     * Cuando el usuario responde el diálogo del sistema, se ejecuta este callback.
     */
    private final ActivityResultLauncher<String[]> solicitarPermisosUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                if (!tienePermisoUbicacion()) {
                    // Si lo rechazó, la app no se rompe: abre el mapa igual, pero sin usar su posición
                    mostrarMensaje(R.string.msg_ubicacion_denegada);
                }
                abrirMapaHospitales();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // PROTECCIÓN: sin sesión iniciada no se puede usar el menú
        FirebaseUser usuario = authViewModel.getUsuario();
        if (usuario == null) {
            irALogin();
            return;
        }

        setContentView(R.layout.activity_menu_principal);
        InsetsUtil.aplicar(this); // evita que el contenido quede bajo las barras del sistema

        // Saludo con el nombre guardado al registrarse (o el correo si no hay nombre)
        TextView tvSaludo = findViewById(R.id.tv_saludo);
        if (tvSaludo != null) {
            String nombre = usuario.getDisplayName();
            tvSaludo.setText(getString(R.string.menu_saludo,
                    (nombre != null && !nombre.isEmpty()) ? nombre : usuario.getEmail()));
        }

        // --- Navegación interna (Intent explícito #1, un único punto de código) ---
        configurarBoton(R.id.btnFicha, v -> navegarA(PacientesActivity.class)); // NUEVO: CRUD Firestore
        configurarBoton(R.id.btnExpedientes, v -> navegarA(MisExpedientesActivity.class));
        configurarBoton(R.id.btnMonitoreo, v -> navegarA(DashboardActivity.class));

        // --- Servicios externos (Intents implícitos #7 y #8) ---
        configurarBoton(R.id.btnHospital, v -> buscarHospitalCercano());
        configurarBoton(R.id.btnWeb, v -> abrirPortalMinsal());

        // --- NUEVO: cerrar sesión ---
        configurarBoton(R.id.btnCerrarSesion, v -> confirmarCerrarSesion());
    }

    // =====================================================================
    // NUEVO: CERRAR SESIÓN con confirmación.
    // signOut() borra la sesión guardada; se vuelve al Login limpiando el historial
    // para que el botón "atrás" no regrese a pantallas privadas.
    // =====================================================================
    private void confirmarCerrarSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialogo_cerrar_titulo)
                .setMessage(R.string.dialogo_cerrar_mensaje)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.si_salir, (d, w) -> {
                    authViewModel.cerrarSesion();
                    mostrarMensaje(R.string.msg_sesion_cerrada);
                    irALogin();
                })
                .show();
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // =====================================================================
    // INTENT #1 (EXPLÍCITO): navegación entre pantallas de la app.
    // "Explícito" = se indica la clase destino exacta (Activity.class).
    // =====================================================================
    private void navegarA(Class<?> destino) {
        Intent intent = new Intent(this, destino);
        startActivity(intent);
    }

    // =====================================================================
    // INTENT #7 (IMPLÍCITO): Mapas.
    // Primero se valida el permiso de ubicación; luego se abre el mapa.
    // =====================================================================
    private void buscarHospitalCercano() {
        if (tienePermisoUbicacion()) {
            abrirMapaHospitales();
        } else {
            // Pide FINE y COARSE: el usuario puede conceder solo la aproximada (Android 12+)
            solicitarPermisosUbicacion.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /** Si hay ubicación conocida centra la búsqueda ahí; si no, usa geo:0,0. */
    private void abrirMapaHospitales() {
        String consulta = Uri.encode(getString(R.string.intent_busqueda_hospital));
        Location ubicacion = obtenerUltimaUbicacion();

        String uri = (ubicacion != null)
                ? "geo:" + ubicacion.getLatitude() + "," + ubicacion.getLongitude() + "?q=" + consulta
                : "geo:0,0?q=" + consulta;

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        iniciarSeguro(mapIntent); // si no hay app de mapas, muestra aviso en vez de crashear
    }

    // =====================================================================
    // INTENT #8 (IMPLÍCITO): Navegador web.
    // =====================================================================
    private void abrirPortalMinsal() {
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.intent_url_minsal)));
        iniciarSeguro(webIntent);
    }

    // ------------------------- Utilidades de ubicación -------------------------

    /** true si el usuario concedió ubicación precisa O aproximada. */
    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Devuelve la mejor última ubicación conocida del equipo, o null si no hay.
     * @SuppressLint se justifica porque tienePermisoUbicacion() se verifica antes.
     */
    @SuppressLint("MissingPermission")
    private Location obtenerUltimaUbicacion() {
        if (!tienePermisoUbicacion()) return null;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return null;

        Location mejor = null;
        for (String proveedor : lm.getProviders(true)) {
            try {
                Location l = lm.getLastKnownLocation(proveedor);
                if (l != null && (mejor == null || l.getAccuracy() < mejor.getAccuracy())) {
                    mejor = l;
                }
            } catch (SecurityException e) {
                // Ej.: solo se concedió ubicación aproximada y el proveedor GPS exige precisa
                Log.w(TAG, "Proveedor sin acceso: " + proveedor);
            }
        }
        return mejor;
    }
}