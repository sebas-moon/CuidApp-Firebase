package com.santo_tomas.cuidapp.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* Pruebas del resumen del Monitoreo Diario (sin Firebase: datos armados a mano). */
public class ResumenDashboardTest {

    private static RegistroDiario registro(String fecha, Integer pulso, boolean medicacion, int alertas) {
        RegistroDiario r = new RegistroDiario();
        r.setFecha(fecha);
        r.setFrecuenciaCardiaca(pulso);
        r.setMedicacionTomada(medicacion);
        r.setAlertas(Collections.nCopies(alertas, "alerta"));
        return r;
    }

    private static Paciente paciente(String nombre) {
        Paciente p = new Paciente();
        p.setNombre(nombre);
        return p;
    }

    @Test
    public void calculaElResumenDelDia() {
        String hoy = "2026-11-23";
        List<Paciente> pacientes = Arrays.asList(paciente("Juan"), paciente("Ana"), paciente("Luis"));
        List<List<RegistroDiario>> registros = Arrays.asList(
                Arrays.asList(registro(hoy, 72, true, 0), registro("2026-11-22", 110, false, 2)),
                Arrays.asList(registro(hoy, null, false, 1)),
                Collections.emptyList());

        ResumenDashboard r = ResumenDashboard.calcular(pacientes, registros, hoy);
        assertEquals(3, r.pacientes);
        assertEquals(2, r.registrosHoy);
        assertEquals(1, r.medicacionHoy);
        assertEquals(1, r.alertasHoy);
        assertEquals(Integer.valueOf(72), r.ultimoPulso);   // el más reciente, no el más alto
        assertEquals("Juan", r.ultimoPulsoPaciente);
        assertEquals(66, ResumenDashboard.porcentaje(2, 3));
    }

    @Test
    public void sinPacientesNoFalla() {
        ResumenDashboard r = ResumenDashboard.calcular(
                Collections.emptyList(), Collections.emptyList(), "2026-11-23");
        assertEquals(0, r.pacientes);
        assertNull(r.ultimoPulso);
        assertEquals(0, ResumenDashboard.porcentaje(0, 0));
    }
}
