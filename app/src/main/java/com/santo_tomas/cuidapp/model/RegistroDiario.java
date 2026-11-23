package com.santo_tomas.cuidapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * NUEVO - MODELO: un REGISTRO CLÍNICO DIARIO de un paciente (idea de la demo web de CuidApp).
 *
 * Se guarda en una SUBCOLECCIÓN dentro del paciente:
 *   pacientes/{pacienteId}/registros/{fecha}      ej: pacientes/aB3x.../registros/2026-11-18
 *
 * El ID del documento ES la fecha (yyyy-MM-dd): así hay como máximo UN registro por día.
 *
 * Los campos clínicos son Integer/Double (objetos, no int/double) porque pueden ir VACÍOS (null):
 * solo se llenan las secciones de las condiciones que tiene el paciente.
 */
public class RegistroDiario {

    // Bloqueo clínico: después de 24 horas el registro es de SOLO LECTURA
    public static final long HORAS_EDITABLE = 24;

    @DocumentId
    private String id;            // = fecha

    private String uid;           // cuidador dueño (para las reglas de seguridad)
    private String pacienteId;
    private String fecha;         // "2026-11-18"

    @ServerTimestamp
    private Date creado;          // lo pone el servidor; desde aquí corren las 24 horas

    // --- Hipertensión / Enfermedad renal: presión y pulso ---
    private Integer sistolica;
    private Integer diastolica;
    private Integer frecuenciaCardiaca;

    // --- Diabetes ---
    private Integer glucemia;
    private String momentoGlucemia;
    private Double insulina;

    // --- EPOC / Asma ---
    private Integer saturacion;
    private Integer frecuenciaRespiratoria;

    // --- Enfermedad renal crónica ---
    private Double pesoDiario;
    private Integer diuresis;

    // --- Bienestar general (todos los pacientes) ---
    private boolean medicacionTomada;
    private int dolor;            // escala EVA 0 a 10
    private String notas;

    // Alertas calculadas con RangosClinicos al guardar
    private List<String> alertas = new ArrayList<>();

    public RegistroDiario() {} // requerido por Firestore

    // ¿Todavía se puede editar o eliminar? (la regla de Firestore verifica lo mismo en el servidor)
    @Exclude
    public boolean esEditable() {
        if (creado == null) return true; // recién creado: el servidor aún no confirma la hora
        long horas = (System.currentTimeMillis() - creado.getTime()) / (1000L * 60 * 60);
        return horas < HORAS_EDITABLE;
    }

    @Exclude
    public boolean tieneAlertas() {
        return alertas != null && !alertas.isEmpty();
    }

    // --- Getters y Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public Date getCreado() { return creado; }
    public void setCreado(Date creado) { this.creado = creado; }
    public Integer getSistolica() { return sistolica; }
    public void setSistolica(Integer sistolica) { this.sistolica = sistolica; }
    public Integer getDiastolica() { return diastolica; }
    public void setDiastolica(Integer diastolica) { this.diastolica = diastolica; }
    public Integer getFrecuenciaCardiaca() { return frecuenciaCardiaca; }
    public void setFrecuenciaCardiaca(Integer frecuenciaCardiaca) { this.frecuenciaCardiaca = frecuenciaCardiaca; }
    public Integer getGlucemia() { return glucemia; }
    public void setGlucemia(Integer glucemia) { this.glucemia = glucemia; }
    public String getMomentoGlucemia() { return momentoGlucemia; }
    public void setMomentoGlucemia(String momentoGlucemia) { this.momentoGlucemia = momentoGlucemia; }
    public Double getInsulina() { return insulina; }
    public void setInsulina(Double insulina) { this.insulina = insulina; }
    public Integer getSaturacion() { return saturacion; }
    public void setSaturacion(Integer saturacion) { this.saturacion = saturacion; }
    public Integer getFrecuenciaRespiratoria() { return frecuenciaRespiratoria; }
    public void setFrecuenciaRespiratoria(Integer frecuenciaRespiratoria) { this.frecuenciaRespiratoria = frecuenciaRespiratoria; }
    public Double getPesoDiario() { return pesoDiario; }
    public void setPesoDiario(Double pesoDiario) { this.pesoDiario = pesoDiario; }
    public Integer getDiuresis() { return diuresis; }
    public void setDiuresis(Integer diuresis) { this.diuresis = diuresis; }
    public boolean isMedicacionTomada() { return medicacionTomada; }
    public void setMedicacionTomada(boolean medicacionTomada) { this.medicacionTomada = medicacionTomada; }
    public int getDolor() { return dolor; }
    public void setDolor(int dolor) { this.dolor = dolor; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public List<String> getAlertas() { return alertas; }
    public void setAlertas(List<String> alertas) { this.alertas = alertas; }
}
