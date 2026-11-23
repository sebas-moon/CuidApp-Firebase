package com.santo_tomas.cuidapp.viewmodel;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.santo_tomas.cuidapp.model.Rutina;

/*
 * NUEVO - VIEWMODEL del temporizador de ejercicio.
 * El CountDownTimer vive aquí (no en la Activity) para que NO se reinicie al girar la pantalla.
 * Cada segundo publica el tiempo restante y el paso actual de la rutina.
 */
public class EjercicioViewModel extends ViewModel {

    private final MutableLiveData<Rutina> rutina = new MutableLiveData<>();
    private final MutableLiveData<Long> restanteMs = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> paso = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> pausado = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> minutosTerminados = new MutableLiveData<>();

    private CountDownTimer timer;
    private long totalMs;

    public LiveData<Rutina> getRutina() { return rutina; }
    public LiveData<Long> getRestanteMs() { return restanteMs; }
    public LiveData<Integer> getPaso() { return paso; }
    public LiveData<Boolean> getPausado() { return pausado; }
    /** Se publica al terminar una rutina: minutos completos realizados (para sumarlos). */
    public LiveData<Integer> getMinutosTerminados() { return minutosTerminados; }

    public boolean enCurso() {
        return rutina.getValue() != null;
    }

    public void iniciar(Rutina r) {
        if (enCurso()) return;
        rutina.setValue(r);
        totalMs = r.minutos * 60_000L;
        pausado.setValue(false);
        arrancar(totalMs);
    }

    public void pausarOReanudar() {
        if (!enCurso()) return;
        if (Boolean.TRUE.equals(pausado.getValue())) {
            pausado.setValue(false);
            arrancar(restanteMs.getValue() == null ? totalMs : restanteMs.getValue());
        } else {
            pausado.setValue(true);
            if (timer != null) timer.cancel();
        }
    }

    /** Minutos COMPLETOS que lleva la rutina en curso. */
    public int minutosHechos() {
        if (!enCurso()) return 0;
        long hecho = totalMs - (restanteMs.getValue() == null ? totalMs : restanteMs.getValue());
        return (int) (hecho / 60_000L);
    }

    /** Termina antes de tiempo: se suman solo los minutos completos que alcanzó a hacer. */
    public void terminar() {
        if (!enCurso()) return;
        finalizar(minutosHechos());
    }

    private void arrancar(long desdeMs) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(desdeMs, 1000) {
            @Override
            public void onTick(long ms) {
                restanteMs.setValue(ms);
                Rutina r = rutina.getValue();
                if (r != null && r.pasos.length > 0) {
                    // Los pasos se reparten en partes iguales del tiempo total
                    long transcurrido = totalMs - ms;
                    int i = (int) (transcurrido * r.pasos.length / totalMs);
                    paso.setValue(Math.min(i, r.pasos.length - 1));
                }
            }

            @Override
            public void onFinish() {
                Rutina r = rutina.getValue();
                finalizar(r == null ? 0 : r.minutos);
            }
        }.start();
    }

    private void finalizar(int minutos) {
        if (timer != null) timer.cancel();
        timer = null;
        rutina.setValue(null);
        restanteMs.setValue(0L);
        paso.setValue(0);
        pausado.setValue(false);
        minutosTerminados.setValue(minutos);
    }

    /** Marca el aviso de "minutos terminados" como leído (para que no se repita al rotar). */
    public void avisoMostrado() {
        minutosTerminados.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (timer != null) timer.cancel(); // la pantalla se cerró del todo
    }
}
