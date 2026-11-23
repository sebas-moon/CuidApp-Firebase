package com.santo_tomas.cuidapp.model;

import java.util.Arrays;
import java.util.List;

/*
 * NUEVO - Una rutina de ejercicio suave, pensada para adultos mayores y pacientes crónicos.
 * Los pasos se reparten en partes iguales durante los minutos de la rutina.
 */
public class Rutina {

    public final String emoji;
    public final String nombre;
    public final String descripcion;
    public final int minutos;
    public final String[] pasos;

    public Rutina(String emoji, String nombre, String descripcion, int minutos, String... pasos) {
        this.emoji = emoji;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.minutos = minutos;
        this.pasos = pasos;
    }

    /** Catálogo fijo de rutinas (no necesita internet). */
    public static List<Rutina> catalogo() {
        return Arrays.asList(
                new Rutina("🚶", "Caminata suave",
                        "Paso cómodo: debe poder conversar sin ahogarse. Ideal para hipertensión y diabetes.",
                        10,
                        "Calienta caminando lento y moviendo los brazos",
                        "Camina a tu ritmo cómodo, respirando por la nariz",
                        "Sigue a ritmo cómodo; si te cansas, baja la velocidad",
                        "Vuelve a la calma caminando lento"),
                new Rutina("🧘", "Estiramientos en silla",
                        "Sentado con la espalda recta. Movimientos lentos, sin dolor.",
                        5,
                        "Gira el cuello lentamente a cada lado",
                        "Sube y baja los hombros, luego haz círculos",
                        "Estira los brazos hacia arriba y hacia adelante",
                        "Haz círculos con los tobillos, primero uno y luego el otro",
                        "Respira hondo y relaja todo el cuerpo"),
                new Rutina("🫁", "Respiración con labios fruncidos",
                        "Ayuda a controlar la falta de aire. Recomendada para EPOC y asma.",
                        5,
                        "Siéntate cómodo y relaja los hombros",
                        "Inspira por la nariz contando hasta 2",
                        "Bota el aire por la boca con los labios fruncidos contando hasta 4",
                        "Repite a tu ritmo, sin forzar"),
                new Rutina("💪", "Fuerza con silla",
                        "Fortalece piernas y equilibrio. Ten una silla firme al lado.",
                        8,
                        "Siéntate y párate de la silla, despacio (repite)",
                        "De pie, apoyado en la silla, sube y baja los talones",
                        "Sentado, estira una rodilla y luego la otra",
                        "De pie, apoyado, levanta una pierna hacia el lado y luego la otra",
                        "Siéntate y respira tranquilo")
        );
    }

    public List<String> listaPasos() {
        return Arrays.asList(pasos);
    }
}
