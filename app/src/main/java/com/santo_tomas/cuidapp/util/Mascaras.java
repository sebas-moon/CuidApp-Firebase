package com.santo_tomas.cuidapp.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/*
 * NUEVO - MÁSCARAS DE ENTRADA: dan formato MIENTRAS el usuario escribe.
 *   - RUT:      "123456785"  -> "12345678-5"  (el guion se pone solo)
 *   - Estatura: "180"        -> "1,80"        (la coma se pone sola; se guarda como 180 cm)
 *   - Fecha:    "15011950"   -> "15/01/1950"
 *   - Peso:     "11111"      -> "111"         (máximo 3 enteros y 1 decimal, con coma)
 *
 * Un TextWatcher avisa cada vez que cambia el texto. Para no entrar en un ciclo infinito
 * (cambiar el texto vuelve a llamar al watcher), se usa la bandera "editando".
 */
public final class Mascaras {

    private Mascaras() { }

    /** Interfaz para la regla de formato de cada máscara. */
    private interface Formato {
        String aplicar(String texto);
    }

    public static void rut(EditText campo) {
        aplicar(campo, Mascaras::formatoRut);
    }

    public static void estatura(EditText campo) {
        aplicar(campo, Mascaras::formatoEstatura);
    }

    public static void fecha(EditText campo) {
        aplicar(campo, Mascaras::formatoFecha);
    }

    public static void peso(EditText campo) {
        aplicar(campo, Mascaras::formatoPeso);
    }

    private static void aplicar(EditText campo, Formato formato) {
        campo.addTextChangedListener(new TextWatcher() {
            private boolean editando = false;

            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (editando) return;
                String nuevo = formato.aplicar(s.toString());
                if (!nuevo.equals(s.toString())) {
                    editando = true;
                    s.replace(0, s.length(), nuevo); // el cursor queda al final
                    editando = false;
                }
            }
        });
    }

    // ---- Reglas de formato (públicas para poder probarlas sin Android) ----

    /** Solo dígitos y K al final, máximo 9 caracteres, guion antes del último. */
    public static String formatoRut(String texto) {
        StringBuilder limpio = new StringBuilder();
        for (char c : texto.toUpperCase().toCharArray()) {
            if (Character.isDigit(c) || c == 'K') limpio.append(c);
        }
        // La K solo puede ser el dígito verificador (último carácter)
        String sinK = limpio.toString();
        int k = sinK.indexOf('K');
        if (k >= 0) sinK = sinK.substring(0, k + 1);
        if (sinK.length() > 9) sinK = sinK.substring(0, 9);
        if (sinK.length() < 2) return sinK;
        return sinK.substring(0, sinK.length() - 1) + "-" + sinK.charAt(sinK.length() - 1);
    }

    /**
     * Peso en kilos: máximo 3 enteros y 1 decimal, con coma.
     * "11111" -> "111", "72.55" -> "72,5", ",5" -> "5" (la coma inicial se ignora).
     */
    public static String formatoPeso(String texto) {
        StringBuilder enteros = new StringBuilder();
        StringBuilder decimal = new StringBuilder();
        boolean coma = false;
        for (char c : texto.toCharArray()) {
            if (c == ',' || c == '.') {
                if (!coma && enteros.length() > 0) coma = true;   // un solo separador
            } else if (Character.isDigit(c)) {
                if (!coma) {
                    if (enteros.length() < 3) enteros.append(c);
                } else if (decimal.length() < 1) {
                    decimal.append(c);
                }
            }
        }
        return coma ? enteros + "," + decimal : enteros.toString();
    }

    /** Máximo 3 dígitos: "1" -> "1", "18" -> "1,8", "180" -> "1,80". */
    public static String formatoEstatura(String texto) {
        String d = soloDigitos(texto, 3);
        if (d.length() <= 1) return d;
        return d.charAt(0) + "," + d.substring(1);
    }

    /** Máximo 8 dígitos: "15011950" -> "15/01/1950". */
    public static String formatoFecha(String texto) {
        String d = soloDigitos(texto, 8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            if (i == 2 || i == 4) sb.append('/');
            sb.append(d.charAt(i));
        }
        return sb.toString();
    }

    private static String soloDigitos(String texto, int maximo) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (Character.isDigit(c) && sb.length() < maximo) sb.append(c);
        }
        return sb.toString();
    }
}
