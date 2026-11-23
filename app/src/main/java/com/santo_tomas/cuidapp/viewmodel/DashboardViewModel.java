package com.santo_tomas.cuidapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.santo_tomas.cuidapp.model.ResumenDashboard;
import com.santo_tomas.cuidapp.repositorio.RegistroRepositorio;

/*
 * NUEVO - VIEWMODEL del Monitoreo Diario.
 * Pide al repositorio el resumen del día (pacientes + registros) y lo entrega como LiveData.
 */
public class DashboardViewModel extends ViewModel {

    private final RegistroRepositorio repositorio = new RegistroRepositorio();

    private final MutableLiveData<ResumenDashboard> resumen = new MutableLiveData<>();
    private final MutableLiveData<String> mensaje = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public LiveData<ResumenDashboard> getResumen() { return resumen; }
    public LiveData<String> getMensaje() { return mensaje; }
    public LiveData<Boolean> getCargando() { return cargando; }

    /** Se llama en onResume: al volver de un registro, el resumen se actualiza. */
    public void cargar() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || Boolean.TRUE.equals(cargando.getValue())) return;
        repositorio.cargarResumen(u.getUid(), resumen, mensaje, cargando);
    }
}
