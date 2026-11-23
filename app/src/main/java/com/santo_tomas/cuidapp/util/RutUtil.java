package com.santo_tomas.cuidapp.util;

/*
 * Utilidades para el RUT chileno.
 * Valida el DÍGITO VERIFICADOR con el algoritmo "módulo 11", así no se aceptan RUT inventados.
 *
 * Formato que usa la app: 12345678-5 (sin puntos, con guion antes del dígito verificador).
 *
 * Algoritmo (ej. 12345678-5):
 *   1. Se toman los dígitos del cuerpo de DERECHA a IZQUIERDA: 8,7,6,5,4,3,2,1
 *   2. Se multiplican por 2,3,4,5,6,7 y se repite la serie: 8*2 + 7*3 + 6*4 + ...
 *   3. resto = 11 - (suma % 11)  ->  11 = "0", 10 = "K", otro = ese número
 */
public final class RutUtil {

    private RutUtil() { } // clase de utilidades: no se instancia

    /** Quita puntos, guion y espacios, y pasa la K a mayúscula: "12.345.678-k" -> "12345678K" */
    public static String limpiar(String rut) {
        if (rut == null) return "";
        return rut.replace(".", "").replace("-", "").replace(" ", "").trim().toUpperCase();
    }

    /** true si el RUT tiene formato correcto y su dígito verificador coincide. */
    public static boolean esValido(String rut) {
        String limpio = limpiar(rut);
        if (limpio.length() < 8 || limpio.length() > 9) return false; // 7 u 8 dígitos + DV

        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.matches("\\d+")) return false;

        return calcularDv(cuerpo) == dv;
    }

    /** Calcula el dígito verificador de un cuerpo de RUT (solo números). */
    public static char calcularDv(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return '0';
        if (resto == 10) return 'K';
        return (char) ('0' + resto);
    }

    /** Da formato estándar SIN puntos: "12.345.678-5" o "123456785" -> "12345678-5". Se guarda así en Firestore. */
    public static String formatear(String rut) {
        String limpio = limpiar(rut);
        if (limpio.length() < 2) return limpio;
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }
}
