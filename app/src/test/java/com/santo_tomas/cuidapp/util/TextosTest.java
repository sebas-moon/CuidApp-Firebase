package com.santo_tomas.cuidapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TextosTest {

    @Test
    public void nombreSinNumeros() {
        assertNull(Textos.errorNombre("María José Pérez", "vacío"));
        assertNull(Textos.errorNombre("Juan O'Higgins-Riquelme", "vacío"));
        assertEquals("El nombre no puede tener números", Textos.errorNombre("Juan 123", "vacío"));
        assertEquals("El nombre no puede tener números", Textos.errorNombre("1234", "vacío"));
        assertEquals("vacío", Textos.errorNombre("  ", "vacío"));
        assertEquals("Usa solo letras", Textos.errorNombre("Juan@Pérez", "vacío"));
    }

    @Test
    public void alergiasYNacionalidad() {
        assertNull(Textos.errorAlergias(""));
        assertNull(Textos.errorAlergias("Penicilina, mariscos (leve)"));
        assertEquals("Las alergias no pueden tener números", Textos.errorAlergias("Penicilina 500 mg"));
        assertNull(Textos.errorNacionalidad(""));
        assertNull(Textos.errorNacionalidad("Chilena"));
        assertEquals("La nacionalidad no puede tener números", Textos.errorNacionalidad("Chil3na"));
    }

    @Test
    public void filtroQuitaDigitos() {
        assertEquals("Juan Pérez", Textos.sinNumeros("Ju4an P3érez"));
        assertEquals("", Textos.sinNumeros("2024"));
    }
}
