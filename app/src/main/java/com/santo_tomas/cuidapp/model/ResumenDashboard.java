package com.santo_tomas.cuidapp.model;

import java.util.List;

/*
 * NUEVO - Resumen del día para el "Monitoreo Diario" (Dashboard), calculado con datos REALES
 * de Firestore (antes el pulso se inventaba al azar cada 3 segundos).
 *
 *   - Último pulso registrado (frecuencia cardíaca más reciente de los registros diarios)
 *   - Pacientes con registro hoy / total de pacientes
 *   - Registros de hoy con la medicación tomada
 *   - Alertas de hoy
 */
public class ResumenDashboard {

    public int pacientes;
    public int registrosHoy;
    public int medicacionHoy;
    public int alertasHoy;

    public Integer ultimoPulso;          // null = nunca se ha registrado
    public String ultimoPulsoPaciente = "";
    public String ultimoPulsoFecha = "";  // yyyy-MM-dd

    /**
     * @param pacientes  pacientes del cuidador
     * @param registros  registros de cada paciente, en el MISMO orden que la lista de pacientes
     * @param hoy        fecha de hoy "yyyy-MM-dd"
     */
    public static ResumenDashboard calcular(List<Paciente> pacientes,
                                            List<List<RegistroDiario>> registros, String hoy) {
        ResumenDashboard r = new ResumenDashboard();
        r.pacientes = pacientes.size();

        for (int i = 0; i < pacientes.size() && i < registros.size(); i++) {
            Paciente p = pacientes.get(i);
            for (RegistroDiario reg : registros.get(i)) {
                if (hoy.equals(reg.getFecha())) {
                    r.registrosHoy++;
                    if (reg.isMedicacionTomada()) r.medicacionHoy++;
                    if (reg.getAlertas() != null) r.alertasHoy += reg.getAlertas().size();
                }
                // El pulso más reciente (las fechas yyyy-MM-dd se comparan bien como texto)
                if (reg.getFrecuenciaCardiaca() != null && reg.getFecha() != null
                        && reg.getFecha().compareTo(r.ultimoPulsoFecha) > 0) {
                    r.ultimoPulso = reg.getFrecuenciaCardiaca();
                    r.ultimoPulsoPaciente = p.getNombre();
                    r.ultimoPulsoFecha = reg.getFecha();
                }
            }
        }
        return r;
    }

    /** Porcentaje 0..100 sin dividir por cero. */
    public static int porcentaje(int parte, int total) {
        return total <= 0 ? 0 : Math.min(100, parte * 100 / total);
    }
}
