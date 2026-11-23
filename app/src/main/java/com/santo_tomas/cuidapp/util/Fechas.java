package com.santo_tomas.cuidapp.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*
 * NUEVO - Fechas en formato chileno dd/MM/yyyy (así se guardan en Firestore).
 * Se usan para la fecha de nacimiento y las fechas de la cédula.
 */
public final class Fechas {

    private Fechas() { }

    public static final String FORMATO = "dd/MM/yyyy";

    /** Convierte "15/01/1950" en Date, o null si la fecha no existe (ej. 31/02/2020). */
    public static Date leer(String texto) {
        if (texto == null || !texto.matches("\\d{2}/\\d{2}/\\d{4}")) return null;
        SimpleDateFormat sdf = new SimpleDateFormat(FORMATO, Locale.US);
        sdf.setLenient(false); // sin esto, 31/02 se "corrige" a 03/03
        try {
            return sdf.parse(texto);
        } catch (ParseException e) {
            return null;
        }
    }

    public static boolean esValida(String texto) {
        return leer(texto) != null;
    }

    /** Años cumplidos a la fecha de hoy, o -1 si la fecha no es válida. */
    public static int edad(String fechaNacimiento) {
        return edad(fechaNacimiento, new Date());
    }

    public static int edad(String fechaNacimiento, Date hoy) {
        Date nacimiento = leer(fechaNacimiento);
        if (nacimiento == null) return -1;
        Calendar n = Calendar.getInstance();
        n.setTime(nacimiento);
        Calendar h = Calendar.getInstance();
        h.setTime(hoy);
        int edad = h.get(Calendar.YEAR) - n.get(Calendar.YEAR);
        if (h.get(Calendar.DAY_OF_YEAR) < n.get(Calendar.DAY_OF_YEAR)) edad--; // aún no cumple años
        return edad;
    }

    /** true si la fecha ya pasó (ej. cédula vencida). */
    public static boolean yaPaso(String fecha) {
        Date d = leer(fecha);
        return d != null && d.before(new Date());
    }

    /** true si la fecha es posterior a hoy (una fecha de nacimiento no puede serlo). */
    public static boolean esFutura(String fecha) {
        Date d = leer(fecha);
        return d != null && d.after(new Date());
    }
}
