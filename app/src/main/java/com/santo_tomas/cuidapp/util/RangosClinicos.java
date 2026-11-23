package com.santo_tomas.cuidapp.util;

import com.santo_tomas.cuidapp.model.RegistroDiario;

import java.util.ArrayList;
import java.util.List;

/*
 * RANGOS CLÍNICOS DE CONTROL (referencia de la demo web de CuidApp).
 * Revisa un registro diario y devuelve la lista de ALERTAS que se guardan junto al registro.
 * Así la lista de registros puede marcar en rojo los días con valores fuera de rango.
 *
 * Valores de referencia usados (orientativos, no reemplazan una indicación médica):
 *   Presión arterial  : alta si >= 140/90, baja si sistólica < 90
 *   Frecuencia cardíaca: > 100 taquicardia, < 50 bradicardia
 *   Glucemia          : < 70 hipoglucemia; en ayunas > 130, en otro momento > 180 hiperglucemia
 *   Saturación SpO2   : < 92 %
 *   Frec. respiratoria: > 24 rpm
 *   Dolor (EVA)       : >= 7 intenso
 */
public final class RangosClinicos {

    private RangosClinicos() { }

    public static List<String> calcularAlertas(RegistroDiario r) {
        List<String> alertas = new ArrayList<>();

        if (r.getSistolica() != null && r.getDiastolica() != null) {
            if (r.getSistolica() >= 140 || r.getDiastolica() >= 90) {
                alertas.add("Presión alta " + r.getSistolica() + "/" + r.getDiastolica());
            } else if (r.getSistolica() < 90) {
                alertas.add("Presión baja " + r.getSistolica() + "/" + r.getDiastolica());
            }
        }
        if (r.getFrecuenciaCardiaca() != null) {
            if (r.getFrecuenciaCardiaca() > 100) alertas.add("Taquicardia " + r.getFrecuenciaCardiaca() + " lpm");
            else if (r.getFrecuenciaCardiaca() < 50) alertas.add("Bradicardia " + r.getFrecuenciaCardiaca() + " lpm");
        }
        if (r.getGlucemia() != null) {
            int g = r.getGlucemia();
            boolean ayunas = "Ayunas".equals(r.getMomentoGlucemia());
            if (g < 70) alertas.add("Hipoglucemia " + g + " mg/dL");
            else if ((ayunas && g > 130) || (!ayunas && g > 180)) alertas.add("Hiperglucemia " + g + " mg/dL");
        }
        if (r.getSaturacion() != null && r.getSaturacion() < 92) {
            alertas.add("Saturación baja " + r.getSaturacion() + "%");
        }
        if (r.getFrecuenciaRespiratoria() != null && r.getFrecuenciaRespiratoria() > 24) {
            alertas.add("Respiración rápida " + r.getFrecuenciaRespiratoria() + " rpm");
        }
        if (r.getDolor() >= 7) {
            alertas.add("Dolor intenso " + r.getDolor() + "/10");
        }
        if (!r.isMedicacionTomada()) {
            alertas.add("No tomó la medicación");
        }
        return alertas;
    }
}
