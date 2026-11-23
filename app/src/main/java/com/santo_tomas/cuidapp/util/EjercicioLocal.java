package com.santo_tomas.cuidapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.santo_tomas.cuidapp.viewmodel.RegistroViewModel;

/*
 * NUEVO - Minutos de ejercicio del día, guardados en el teléfono (SharedPreferences).
 * Una clave por día ("min_2026-11-23"), así cada día parte en 0.
 * Lo escribe EjercicioActivity al terminar una rutina y lo lee el Dashboard para su anillo.
 */
public final class EjercicioLocal {

    private EjercicioLocal() { }

    public static final int META_MINUTOS = 30; // recomendación: 30 min de actividad suave al día

    private static final String ARCHIVO = "ejercicio";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    private static String claveHoy() {
        return "min_" + RegistroViewModel.hoy();
    }

    public static int minutosHoy(Context ctx) {
        return prefs(ctx).getInt(claveHoy(), 0);
    }

    public static void sumarMinutos(Context ctx, int minutos) {
        if (minutos <= 0) return;
        prefs(ctx).edit().putInt(claveHoy(), minutosHoy(ctx) + minutos).apply();
    }
}
