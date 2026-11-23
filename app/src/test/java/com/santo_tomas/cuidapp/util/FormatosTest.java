package com.santo_tomas.cuidapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/* Pruebas del RUT, las máscaras de entrada y las fechas. */
public class FormatosTest {

    @Test
    public void rutSinPuntosYConGuion() {
        assertEquals("12345678-5", RutUtil.formatear("12.345.678-5"));
        assertEquals("12345678-5", RutUtil.formatear("123456785"));
        assertTrue(RutUtil.esValido("12345678-5"));
        assertFalse(RutUtil.esValido("12345678-9"));
    }

    @Test
    public void mascaraRut() {
        assertEquals("1", Mascaras.formatoRut("1"));
        assertEquals("1-2", Mascaras.formatoRut("12"));
        assertEquals("12345678-5", Mascaras.formatoRut("123456785"));
        assertEquals("12345678-5", Mascaras.formatoRut("12.345.678-5"));
        assertEquals("12345678-5", Mascaras.formatoRut("1234567851111")); // tope de 9 caracteres
        assertEquals("1000005-K", Mascaras.formatoRut("1000005k"));
    }

    @Test
    public void mascaraEstatura() {
        assertEquals("1", Mascaras.formatoEstatura("1"));
        assertEquals("1,8", Mascaras.formatoEstatura("18"));
        assertEquals("1,80", Mascaras.formatoEstatura("180"));
        assertEquals("1,80", Mascaras.formatoEstatura("1,805")); // tope de 3 dígitos
    }

    @Test
    public void mascaraPeso() {
        assertEquals("111", Mascaras.formatoPeso("11111"));      // tope de 3 enteros
        assertEquals("72,5", Mascaras.formatoPeso("72.5"));      // punto -> coma
        assertEquals("72,5", Mascaras.formatoPeso("72,55"));     // un solo decimal
        assertEquals("72,", Mascaras.formatoPeso("72,,"));       // un solo separador
        assertEquals("5", Mascaras.formatoPeso(",5"));           // la coma inicial se ignora
    }

    @Test
    public void mascaraFecha() {
        assertEquals("15/01/1950", Mascaras.formatoFecha("15011950"));
        assertEquals("15/0", Mascaras.formatoFecha("150"));
    }

    @Test
    public void fechas() {
        assertTrue(Fechas.esValida("29/02/2024"));
        assertFalse(Fechas.esValida("31/02/2024"));
        Calendar hoy = Calendar.getInstance();
        hoy.set(2026, Calendar.NOVEMBER, 23);
        assertEquals(76, Fechas.edad("15/01/1950", hoy.getTime()));
        assertEquals(75, Fechas.edad("24/11/1950", hoy.getTime()));
    }

    @Test
    public void estaturaYTelefono() {
        assertEquals("1,80", Formatos.estaturaMetros(180));
        assertEquals("0,55", Formatos.estaturaMetros(55));
        assertEquals(180, Formatos.estaturaCm("1,80"));
        assertEquals(180, Formatos.estaturaCm("1,8"));
        assertEquals(-1, Formatos.estaturaCm("180"));
        assertEquals("+56912345678", Formatos.telefonoParaGuardar("12345678"));
        assertEquals("12345678", Formatos.telefonoLocal("+56912345678"));
        assertEquals("+56 9 1234 5678", Formatos.telefonoLegible("+56912345678"));
    }
}
