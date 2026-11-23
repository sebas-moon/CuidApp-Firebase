package com.santo_tomas.cuidapp.util;

import java.util.ArrayList;
import java.util.List;

/*
 * Catálogo de CONDICIONES CLÍNICAS (tomado de la demo web de CuidApp).
 * En Firestore se guarda solo la CLAVE (ej. "DIABETES"); el nombre es para mostrar.
 * Según las condiciones del paciente, el registro diario muestra unas secciones u otras.
 */
public final class Condiciones {

    private Condiciones() { }

    public static final String HIPERTENSION = "HIPERTENSION";
    public static final String DIABETES = "DIABETES";
    public static final String EPOC = "EPOC";
    public static final String ERC = "ERC";

    /** Nombre legible de una condición. */
    public static String nombre(String clave) {
        if (clave == null) return "";
        switch (clave) {
            case HIPERTENSION: return "Hipertensión";
            case DIABETES: return "Diabetes";
            case EPOC: return "EPOC / Asma";
            case ERC: return "Enf. Renal Crónica";
            default: return clave;
        }
    }

    /** Une las condiciones para mostrarlas: "Hipertensión, Diabetes" o "Monitoreo general". */
    public static String texto(List<String> claves) {
        if (claves == null || claves.isEmpty()) return "Monitoreo general";
        List<String> nombres = new ArrayList<>();
        for (String c : claves) nombres.add(nombre(c));
        return String.join(", ", nombres);
    }
}
