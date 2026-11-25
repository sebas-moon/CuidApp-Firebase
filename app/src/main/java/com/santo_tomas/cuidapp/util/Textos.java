package com.santo_tomas.cuidapp.util;

import android.text.InputFilter;
import android.widget.EditText;

/*
 * NUEVO - TEXTOS SIN NÚMEROS: nombres de personas, nacionalidad y alergias.
 *   - Mientras se escribe: un InputFilter descarta los dígitos (no se pueden teclear ni pegar).
 *   - Al guardar: las reglas de abajo dan el mensaje de error (por si el texto llega
 *     de otra forma, por ejemplo al escanear la cédula o al editar un paciente antiguo).
 */
public final class Textos {

    private Textos() { }

    /** Nombre de persona: letras (con tildes y ñ), espacios, punto, apóstrofo y guion. */
    private static final String NOMBRE = "[\\p{L} .'-]+";
    /** Alergias: letras, espacios y signos para separar (coma, punto, punto y coma, guion, paréntesis). */
    private static final String ALERGIAS = "[\\p{L} ,.;:()/'-]*";

    public static boolean tieneNumeros(String texto) {
        return texto != null && texto.matches(".*\\d.*");
    }

    /** Devuelve el mensaje de error del nombre o null si está bien. */
    public static String errorNombre(String nombre, String siVacio) {
        if (nombre == null || nombre.trim().isEmpty()) return siVacio;
        String n = nombre.trim();
        if (tieneNumeros(n)) return "El nombre no puede tener números";
        if (n.length() < 3 || n.length() > 60) return "Debe tener entre 3 y 60 caracteres";
        if (!n.matches(NOMBRE)) return "Usa solo letras";
        return null;
    }

    /** Devuelve el mensaje de error de alergias (campo opcional) o null si está bien. */
    public static String errorAlergias(String alergias) {
        if (alergias == null) return null;
        String a = alergias.trim();
        if (tieneNumeros(a)) return "Las alergias no pueden tener números";
        if (a.length() > 100) return "Máximo 100 caracteres";
        if (!a.matches(ALERGIAS)) return "Usa solo letras, comas y puntos";
        return null;
    }

    /** Nacionalidad (opcional): solo letras y espacios, máximo 30 caracteres. */
    public static String errorNacionalidad(String nacionalidad) {
        if (nacionalidad == null) return null;
        String n = nacionalidad.trim();
        if (tieneNumeros(n)) return "La nacionalidad no puede tener números";
        if (n.length() > 30) return "Máximo 30 caracteres";
        if (!n.isEmpty() && !n.matches("[\\p{L} ]+")) return "Usa solo letras";
        return null;
    }

    /** Quita los dígitos de un texto (lo usa el filtro y sirve para probarlo sin Android). */
    public static String sinNumeros(CharSequence texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (!Character.isDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    /** Agrega al campo un filtro que bloquea los números, conservando los filtros del XML (maxLength). */
    public static void bloquearNumeros(EditText campo) {
        InputFilter sinNumeros = (fuente, inicio, fin, destino, dInicio, dFin) -> {
            CharSequence trozo = fuente.subSequence(inicio, fin);
            String limpio = sinNumeros(trozo);
            return limpio.length() == trozo.length() ? null : limpio; // null = aceptar tal cual
        };
        InputFilter[] actuales = campo.getFilters();
        InputFilter[] nuevos = new InputFilter[actuales.length + 1];
        System.arraycopy(actuales, 0, nuevos, 0, actuales.length);
        nuevos[actuales.length] = sinNumeros;
        campo.setFilters(nuevos);
    }
}
