package com.santo_tomas.cuidapp.util;

import java.util.Locale;

/*
 * NUEVO - Conversiones entre lo que se ESCRIBE en pantalla y lo que se GUARDA en Firestore.
 *   Estatura: pantalla "1,80" (metros)        <-> Firestore 180 (centímetros, número entero)
 *   Teléfono: pantalla "+56 9" + "12345678"   <-> Firestore "+56912345678"
 */
public final class Formatos {

    private Formatos() { }

    public static final String PREFIJO_CELULAR = "+569";

    /** 180 -> "1,80" */
    public static String estaturaMetros(int cm) {
        return String.format(Locale.US, "%d,%02d", cm / 100, cm % 100);
    }

    /** "1,80" -> 180 ; "1,8" -> 180 ; texto inválido -> -1 */
    public static int estaturaCm(String metros) {
        if (metros == null || !metros.matches("\\d([,.]\\d{1,2})?")) return -1;
        double m = Double.parseDouble(metros.replace(',', '.'));
        return (int) Math.round(m * 100);
    }

    /** 8 dígitos del celular -> "+56912345678" */
    public static String telefonoParaGuardar(String ochoDigitos) {
        return PREFIJO_CELULAR + ochoDigitos;
    }

    /** "+56912345678" -> "12345678" (para mostrarlo en el campo, que ya tiene el prefijo +56 9) */
    public static String telefonoLocal(String guardado) {
        if (guardado == null) return "";
        String d = guardado.replaceAll("\\D", "");
        return d.length() > 8 ? d.substring(d.length() - 8) : d;
    }

    /** "+56912345678" -> "+56 9 1234 5678" */
    public static String telefonoLegible(String guardado) {
        String d = telefonoLocal(guardado);
        if (d.length() != 8) return guardado == null ? "" : guardado;
        return "+56 9 " + d.substring(0, 4) + " " + d.substring(4);
    }
}
