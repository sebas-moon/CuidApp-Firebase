package com.santo_tomas.cuidapp.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * NUEVO - LECTOR DE CÉDULA CHILENA: recibe el TEXTO que ML Kit reconoció en la foto
 * y saca los datos. Es Java puro (sin Android), por eso tiene pruebas unitarias.
 *
 * Funciona con las dos caras:
 *   FRENTE  : etiquetas APELLIDOS, NOMBRES, NACIONALIDAD, SEXO, fechas "15 ENE 1950",
 *             NÚMERO DOCUMENTO "123.456.789" y RUN "12.345.678-5".
 *   REVERSO : zona de lectura mecánica (MRZ), 3 líneas de 30 caracteres con "<":
 *             INCHL123456789?12345678<5<<<<     (documento y RUN)
 *             5001155M3001159CHL<<<<<<<<<<<?     (nacimiento, sexo, vencimiento, país)
 *             PEREZ<GONZALEZ<<JUAN<ANDRES<<<     (apellidos << nombres)
 *   La MRZ es más confiable (está hecha para máquinas), así que si aparece, manda.
 *
 * Lo que NO se puede sacar: la FIRMA y la foto, porque son imágenes, no texto.
 * El OCR puede equivocarse: la app siempre deja revisar y corregir antes de guardar.
 */
public final class CedulaParser {

    private CedulaParser() { }

    /** Datos leídos. Lo que no se encontró queda como texto vacío. */
    public static class Resultado {
        public String apellidos = "";
        public String nombres = "";
        public String nacionalidad = "";
        public String sexo = "";              // "Femenino" / "Masculino"
        public String fechaNacimiento = "";   // dd/MM/yyyy
        public String numeroDocumento = "";   // 123.456.789
        public String fechaEmision = "";
        public String fechaVencimiento = "";
        public String run = "";               // 12345678-5 (solo si el dígito verificador es correcto)
        public boolean runConError = false;   // se vio un RUN pero su dígito verificador no calza

        /** "Juan Andrés Pérez González" */
        public String nombreCompleto() {
            return (nombres + " " + apellidos).trim();
        }

        /** Cuántos datos se encontraron (para el mensaje "Se leyeron N datos"). */
        public int cantidad() {
            int n = 0;
            for (String s : new String[]{apellidos, nombres, nacionalidad, sexo, fechaNacimiento,
                    numeroDocumento, fechaEmision, fechaVencimiento, run}) {
                if (!s.isEmpty()) n++;
            }
            return n;
        }
    }

    private static final String MESES = "ENE|FEB|MAR|ABR|MAY|JUN|JUL|AGO|SEP|SET|OCT|NOV|DIC";
    private static final Pattern FECHA_TEXTO = Pattern.compile(
            "\\b(\\d{1,2})\\s*[.\\-/ ]?\\s*(" + MESES + ")[A-Z]*\\.?\\s*[.\\-/ ]?\\s*((?:19|20)\\d{2})\\b");
    private static final Pattern FECHA_NUMERICA = Pattern.compile(
            "\\b(\\d{2})[/\\-.](\\d{2})[/\\-.]((?:19|20)\\d{2})\\b");
    private static final Pattern RUN = Pattern.compile(
            "\\b(\\d{1,2}\\.?\\d{3}\\.?\\d{3})\\s*-\\s*([0-9K])\\b");
    private static final Pattern NUMERO_DOCUMENTO = Pattern.compile(
            "(?<![\\d.])(\\d{3}\\.\\d{3}\\.\\d{3})(?![\\d.]|\\s*-)");
    private static final Pattern NACIONALIDADES = Pattern.compile(
            "\\b(CHILEN[AO]|ARGENTIN[AO]|PERUAN[AO]|BOLIVIAN[AO]|VENEZOLAN[AO]|COLOMBIAN[AO]"
                    + "|HAITIAN[AO]|ECUATORIAN[AO]|CUBAN[AO]|DOMINICAN[AO]|ESPANOL[A]?|BRASILEN[AO])\\b");
    private static final Pattern MRZ_LINEA2 = Pattern.compile(
            "(\\d{6})\\d([MF<])(\\d{6})\\d([A-Z]{3})");
    private static final Pattern MRZ_RUN = Pattern.compile("(\\d{7,8})<{0,2}([0-9K])");
    private static final String[] ETIQUETAS = {"APELLIDO", "NOMBRE", "NACIONALIDAD", "SEXO", "FECHA",
            "NUMERO", "DOCUMENTO", "RUN", "FIRMA", "EMISION", "VENCIMIENTO", "REPUBLICA",
            "SERVICIO", "REGISTRO", "CEDULA", "IDENTIDAD", "IDENTIFICACION", "CHILE"};

    public static Resultado leer(String textoOcr) {
        Resultado r = new Resultado();
        if (textoOcr == null || textoOcr.trim().isEmpty()) return r;

        String original = textoOcr.toUpperCase(Locale.ROOT);
        String sinTildes = quitarTildes(original);
        String[] lineasOrig = original.split("\\r?\\n");
        String[] lineas = sinTildes.split("\\r?\\n");

        FechasMrz mrzFechas = leerMrz(lineas, r);   // primero el reverso (más confiable)
        leerFrente(lineasOrig, lineas, sinTildes, r, mrzFechas);
        return r;
    }

    // =====================================================================
    // REVERSO: zona MRZ
    // =====================================================================
    /** Fechas que vienen en la MRZ (nacimiento y vencimiento), para ubicar la de emisión. */
    private static class FechasMrz {
        String nacimiento = "";
        String vencimiento = "";
    }

    private static FechasMrz leerMrz(String[] lineas, Resultado r) {
        FechasMrz f = new FechasMrz();
        for (String bruta : lineas) {
            String l = bruta.replace(" ", "").replace("«", "<<").replace("‹", "<");
            if (l.length() < 20) continue;

            // Línea 1: I + (N o <) + CHL + documento(9) + dígito control + datos opcionales (RUN)
            if (l.matches("^I.?CHL.*")) {
                int inicio = l.indexOf("CHL") + 3;
                if (l.length() >= inicio + 9) {
                    String doc = l.substring(inicio, inicio + 9).replace("<", "");
                    if (doc.matches("\\d{9}")) {
                        r.numeroDocumento = doc.substring(0, 3) + "." + doc.substring(3, 6) + "." + doc.substring(6);
                    }
                    Matcher m = MRZ_RUN.matcher(l.substring(Math.min(l.length(), inicio + 10)));
                    if (m.find()) {
                        String run = m.group(1) + m.group(2);
                        if (RutUtil.esValido(run)) r.run = RutUtil.formatear(run);
                        else r.runConError = true;
                    }
                }
                continue;
            }

            // Línea 2: nacimiento AAMMDD, sexo, vencimiento AAMMDD, país
            Matcher m2 = MRZ_LINEA2.matcher(l);
            if (m2.find()) {
                f.nacimiento = fechaMrz(m2.group(1), true);
                f.vencimiento = fechaMrz(m2.group(3), false);
                if (!f.nacimiento.isEmpty()) r.fechaNacimiento = f.nacimiento;
                if (!f.vencimiento.isEmpty()) r.fechaVencimiento = f.vencimiento;
                if ("M".equals(m2.group(2))) r.sexo = "Masculino";
                if ("F".equals(m2.group(2))) r.sexo = "Femenino";
                if ("CHL".equals(m2.group(4))) r.nacionalidad = "Chilena";
                continue;
            }

            // Línea 3: APELLIDO1<APELLIDO2<<NOMBRE1<NOMBRE2<<<
            if (l.matches("^[A-Z<]+$") && l.contains("<<") && l.replace("<", "").length() >= 4) {
                String[] partes = l.split("<<+", 2);
                if (partes.length == 2) {
                    String ap = partes[0].replace("<", " ").trim();
                    String no = partes[1].replace("<", " ").trim();
                    if (!ap.isEmpty() && !no.isEmpty()) {
                        r.apellidos = tipoTitulo(ap);
                        r.nombres = tipoTitulo(no);
                    }
                }
            }
        }
        return f;
    }

    /** "500115" -> "15/01/1950". El siglo se deduce: nacimientos futuros no existen, vencimientos sí. */
    private static String fechaMrz(String aammdd, boolean esNacimiento) {
        int aa = Integer.parseInt(aammdd.substring(0, 2));
        String mm = aammdd.substring(2, 4);
        String dd = aammdd.substring(4, 6);
        int anioActual = Calendar.getInstance().get(Calendar.YEAR) % 100;
        int anio = esNacimiento
                ? (aa > anioActual ? 1900 + aa : 2000 + aa)
                : 2000 + aa;
        String fecha = dd + "/" + mm + "/" + anio;
        return Fechas.esValida(fecha) ? fecha : "";
    }

    // =====================================================================
    // FRENTE: etiquetas y patrones
    // =====================================================================
    private static void leerFrente(String[] lineasOrig, String[] lineas, String texto,
                                   Resultado r, FechasMrz mrz) {
        // --- RUN: el primero con dígito verificador correcto ---
        if (r.run.isEmpty()) {
            Matcher m = RUN.matcher(texto);
            while (m.find()) {
                String run = m.group(1) + m.group(2);
                if (RutUtil.esValido(run)) {
                    r.run = RutUtil.formatear(run);
                    r.runConError = false;
                    break;
                }
                r.runConError = true;
            }
        }

        // --- Número de documento: 123.456.789 ---
        if (r.numeroDocumento.isEmpty()) {
            Matcher m = NUMERO_DOCUMENTO.matcher(texto);
            if (m.find()) r.numeroDocumento = m.group(1);
        }

        // --- Nacionalidad ---
        if (r.nacionalidad.isEmpty()) {
            Matcher m = NACIONALIDADES.matcher(texto);
            if (m.find()) r.nacionalidad = tipoTitulo(m.group(1).replaceAll("O$", "A"));
        }

        // --- Sexo: una línea que sea solo "M" o "F", o "SEXO M", o "CHILENA F" ---
        if (r.sexo.isEmpty()) {
            Matcher m = Pattern.compile("(?m)(?:^|SEXO\\s*|CHILEN[AO]\\s+)([MF])\\s*$").matcher(texto);
            if (m.find()) r.sexo = "M".equals(m.group(1)) ? "Masculino" : "Femenino";
        }

        // --- Fechas ---
        List<Date> fechas = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        juntarFechas(FECHA_TEXTO.matcher(texto), true, fechas, textos);
        juntarFechas(FECHA_NUMERICA.matcher(texto), false, fechas, textos);
        asignarFechas(fechas, textos, r, mrz);

        // --- Apellidos y nombres: las líneas que siguen a cada etiqueta ---
        if (r.apellidos.isEmpty() || r.nombres.isEmpty()) {
            String ap = valorBajoEtiqueta(lineasOrig, lineas, "APELLIDO");
            String no = valorBajoEtiqueta(lineasOrig, lineas, "NOMBRE");
            if (!ap.isEmpty() && !no.isEmpty()) {
                r.apellidos = tipoTitulo(ap);
                r.nombres = tipoTitulo(no);
            }
        } else {
            // Si la MRZ dio los nombres (sin tildes), se prefieren los del frente que sí las tienen
            String ap = valorBajoEtiqueta(lineasOrig, lineas, "APELLIDO");
            String no = valorBajoEtiqueta(lineasOrig, lineas, "NOMBRE");
            if (!ap.isEmpty() && quitarTildes(ap).equalsIgnoreCase(quitarTildes(r.apellidos))) r.apellidos = tipoTitulo(ap);
            if (!no.isEmpty() && quitarTildes(no).equalsIgnoreCase(quitarTildes(r.nombres))) r.nombres = tipoTitulo(no);
        }
    }

    private static void juntarFechas(Matcher m, boolean conMes, List<Date> fechas, List<String> textos) {
        while (m.find()) {
            String dd = String.format(Locale.US, "%02d", Integer.parseInt(m.group(1)));
            String mm = conMes ? numeroMes(m.group(2)) : m.group(2);
            String fecha = dd + "/" + mm + "/" + m.group(3);
            Date d = Fechas.leer(fecha);
            if (d != null && !textos.contains(fecha)) {
                fechas.add(d);
                textos.add(fecha);
            }
        }
    }

    /**
     * El frente trae 3 fechas y no siempre en orden. Se ordenan de la más antigua a la más nueva:
     * nacimiento < emisión < vencimiento. Si la MRZ ya dio nacimiento y vencimiento, la que sobra es la emisión.
     */
    private static void asignarFechas(List<Date> fechas, List<String> textos, Resultado r, FechasMrz mrz) {
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < fechas.size(); i++) orden.add(i);
        Collections.sort(orden, (a, b) -> fechas.get(a).compareTo(fechas.get(b)));
        List<String> ordenadas = new ArrayList<>();
        for (int i : orden) ordenadas.add(textos.get(i));

        if (!mrz.nacimiento.isEmpty() || !mrz.vencimiento.isEmpty()) {
            for (String f : ordenadas) {
                if (!f.equals(mrz.nacimiento) && !f.equals(mrz.vencimiento) && r.fechaEmision.isEmpty()) {
                    r.fechaEmision = f;
                }
            }
            return;
        }
        if (ordenadas.isEmpty()) return;
        r.fechaNacimiento = ordenadas.get(0);
        if (ordenadas.size() >= 3) {
            r.fechaEmision = ordenadas.get(1);
            r.fechaVencimiento = ordenadas.get(ordenadas.size() - 1);
        } else if (ordenadas.size() == 2) {
            String segunda = ordenadas.get(1);
            if (Fechas.esFutura(segunda)) r.fechaVencimiento = segunda;
            else r.fechaEmision = segunda;
        }
    }

    /**
     * Busca la línea con la etiqueta (ej. "NOMBRES") y devuelve las líneas siguientes que son
     * solo letras, hasta la próxima etiqueta (máximo 2 líneas). Si el valor viene en la misma
     * línea ("NOMBRES JUAN ANDRÉS"), también lo toma.
     */
    private static String valorBajoEtiqueta(String[] orig, String[] lineas, String etiqueta) {
        for (int i = 0; i < lineas.length; i++) {
            String l = lineas[i].trim();
            if (!l.startsWith(etiqueta)) continue;

            StringBuilder valor = new StringBuilder();
            String resto = orig[i].trim().replaceFirst("^[A-ZÁÉÍÓÚÑ]*" + "\\s*", "");
            if (esSoloLetras(resto) && !esEtiqueta(quitarTildes(resto))) valor.append(resto);

            for (int j = i + 1; j < lineas.length && j <= i + 2; j++) {
                String sig = lineas[j].trim();
                if (sig.isEmpty()) continue;
                if (esEtiqueta(sig) || !esSoloLetras(orig[j].trim())) break;
                if (valor.length() > 0) valor.append(' ');
                valor.append(orig[j].trim());
            }
            if (valor.length() > 0) return valor.toString();
        }
        return "";
    }

    private static boolean esEtiqueta(String linea) {
        for (String e : ETIQUETAS) if (linea.contains(e)) return true;
        return false;
    }

    private static boolean esSoloLetras(String s) {
        return s.length() >= 2 && s.matches("[A-ZÁÉÍÓÚÑÜ' ]+");
    }

    private static String numeroMes(String mes) {
        switch (mes) {
            case "ENE": return "01";
            case "FEB": return "02";
            case "MAR": return "03";
            case "ABR": return "04";
            case "MAY": return "05";
            case "JUN": return "06";
            case "JUL": return "07";
            case "AGO": return "08";
            case "SEP": case "SET": return "09";
            case "OCT": return "10";
            case "NOV": return "11";
            default: return "12";
        }
    }

    private static String quitarTildes(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** "JUAN ANDRÉS" -> "Juan Andrés" */
    static String tipoTitulo(String s) {
        StringBuilder sb = new StringBuilder();
        for (String p : s.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
