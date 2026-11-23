package com.santo_tomas.cuidapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/*
 * Pruebas del lector de cédula con textos como los que entrega ML Kit.
 * Se ejecutan en el computador (sin emulador): clic derecho > Run 'CedulaParserTest'.
 */
public class CedulaParserTest {

    // Frente de una cédula (formato 2013 en adelante). ML Kit suele devolver un bloque por línea.
    private static final String FRENTE =
            "REPÚBLICA DE CHILE\n"
            + "SERVICIO DE REGISTRO CIVIL E IDENTIFICACIÓN\n"
            + "CÉDULA DE IDENTIDAD\n"
            + "APELLIDOS\n"
            + "PÉREZ\n"
            + "GONZÁLEZ\n"
            + "NOMBRES\n"
            + "JUAN ANDRÉS\n"
            + "NACIONALIDAD\n"
            + "SEXO\n"
            + "CHILENA\n"
            + "M\n"
            + "FECHA DE NACIMIENTO\n"
            + "NÚMERO DOCUMENTO\n"
            + "15 ENE 1950\n"
            + "123.456.789\n"
            + "FECHA DE EMISIÓN\n"
            + "FECHA DE VENCIMIENTO\n"
            + "10 MAR 2020\n"
            + "15 ENE 2030\n"
            + "FIRMA DEL TITULAR\n"
            + "RUN 12.345.678-5\n";

    // Reverso: zona de lectura mecánica (MRZ)
    private static final String REVERSO =
            "INCHL1234567893S12345678<5<<<<\n"
            + "5001155M3001159CHL<<<<<<<<<<<4\n"
            + "PEREZ<GONZALEZ<<JUAN<ANDRES<<<\n";

    @Test
    public void leeElFrente() {
        CedulaParser.Resultado r = CedulaParser.leer(FRENTE);
        assertEquals("Pérez González", r.apellidos);
        assertEquals("Juan Andrés", r.nombres);
        assertEquals("Juan Andrés Pérez González", r.nombreCompleto());
        assertEquals("Chilena", r.nacionalidad);
        assertEquals("Masculino", r.sexo);
        assertEquals("15/01/1950", r.fechaNacimiento);
        assertEquals("123.456.789", r.numeroDocumento);
        assertEquals("10/03/2020", r.fechaEmision);
        assertEquals("15/01/2030", r.fechaVencimiento);
        assertEquals("12345678-5", r.run);
        assertEquals(9, r.cantidad());
    }

    @Test
    public void leeElReverso() {
        CedulaParser.Resultado r = CedulaParser.leer(REVERSO);
        assertEquals("Perez Gonzalez", r.apellidos);
        assertEquals("Juan Andres", r.nombres);
        assertEquals("Masculino", r.sexo);
        assertEquals("15/01/1950", r.fechaNacimiento);
        assertEquals("15/01/2030", r.fechaVencimiento);
        assertEquals("123.456.789", r.numeroDocumento);
        assertEquals("12345678-5", r.run);
        assertEquals("Chilena", r.nacionalidad);
    }

    @Test
    public void ambasCarasPrefiereTildesDelFrenteYDatosDeLaMrz() {
        CedulaParser.Resultado r = CedulaParser.leer(REVERSO + FRENTE);
        assertEquals("Pérez González", r.apellidos);
        assertEquals("10/03/2020", r.fechaEmision);
        assertEquals("15/01/2030", r.fechaVencimiento);
    }

    @Test
    public void fechasDesordenadasSeOrdenanPorAntiguedad() {
        CedulaParser.Resultado r = CedulaParser.leer("15 ENE 2030\n10 MAR 2020\n03 JUL 1948");
        assertEquals("03/07/1948", r.fechaNacimiento);
        assertEquals("10/03/2020", r.fechaEmision);
        assertEquals("15/01/2030", r.fechaVencimiento);
    }

    @Test
    public void runConDigitoMaloNoSeAcepta() {
        CedulaParser.Resultado r = CedulaParser.leer("RUN 12.345.678-9");
        assertEquals("", r.run);
        assertTrue(r.runConError);
    }

    @Test
    public void numeroDocumentoNoSeConfundeConElRun() {
        CedulaParser.Resultado r = CedulaParser.leer("RUN 7.654.321-6");
        assertEquals("", r.numeroDocumento);
        assertEquals("7654321-6", r.run);
    }

    @Test
    public void textoVacioNoFalla() {
        assertEquals(0, CedulaParser.leer("").cantidad());
        assertEquals(0, CedulaParser.leer(null).cantidad());
        assertFalse(CedulaParser.leer("HOLA MUNDO").runConError);
    }
}
