package com.santo_tomas.cuidapp.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * MODELO: representa UN documento de la colección "pacientes" en Cloud Firestore.
 *
 * MEJORA respecto a CuidApp original: antes había UN solo paciente guardado en
 * SharedPreferences (solo en el teléfono). Ahora cada cuidador puede tener VARIOS
 * pacientes guardados en la nube, y solo él puede verlos.
 *
 * Así se ve en la consola:
 *   pacientes/{idAutomatico}
 *      uid:              "Xy12...abc"     <- UID del cuidador (Firebase Authentication)
 *      nombre:           "Juan Pérez"
 *      rut:              "12.345.678-5"
 *      edad:             78
 *      sexo:             "Masculino"
 *      peso:             74.5            (kg)
 *      estatura:         168             (cm)
 *      tipoSangre:       "O+"
 *      condiciones:      ["HIPERTENSION", "DIABETES"]   <- idea de la demo web
 *      alergias:         "Penicilina"
 *      contactoNombre:   "María Pérez"
 *      contactoRelacion: "Hija"
 *      contactoTelefono: "+56912345678"
 *
 *   y dentro, la subcolección pacientes/{id}/registros (ver RegistroDiario).
 *      fechaRegistro:    18 nov 2026 20:15
 *
 * Firestore usa el CONSTRUCTOR VACÍO y los GETTERS/SETTERS para convertir objeto <-> documento.
 */
public class Paciente {

    // @DocumentId: al LEER, Firestore pone aquí el ID del documento. Al ESCRIBIR se ignora.
    @DocumentId
    private String id;

    private String uid;               // cuidador dueño del registro
    private String nombre;
    private String rut;
    private int edad;
    private String sexo;
    private double peso;              // kg
    private int estatura;             // cm
    private String tipoSangre;
    private List<String> condiciones = new ArrayList<>(); // claves de util/Condiciones
    private String alergias;
    private String contactoNombre;    // contacto de emergencia
    private String contactoRelacion;
    private String contactoTelefono;

    // NUEVO: datos de la cédula (opcionales, "" si no se ingresaron; fechas dd/MM/yyyy)
    private String nacionalidad = "";
    private String fechaNacimiento = "";
    private String numeroDocumento = "";
    private String fechaEmision = "";
    private String fechaVencimiento = "";

    // @ServerTimestamp: si va null, la fecha la pone el SERVIDOR de Firebase
    @ServerTimestamp
    private Date fechaRegistro;

    // IMPORTANTE: constructor vacío requerido por Firestore
    public Paciente() {}

    public Paciente(String uid, String nombre, String rut, int edad, String sexo, double peso,
                    int estatura, String tipoSangre, List<String> condiciones, String alergias,
                    String contactoNombre, String contactoRelacion, String contactoTelefono) {
        this.uid = uid;
        this.nombre = nombre;
        this.rut = rut;
        this.edad = edad;
        this.sexo = sexo;
        this.peso = peso;
        this.estatura = estatura;
        this.tipoSangre = tipoSangre;
        this.condiciones = condiciones;
        this.alergias = alergias;
        this.contactoNombre = contactoNombre;
        this.contactoRelacion = contactoRelacion;
        this.contactoTelefono = contactoTelefono;
    }

    // --- Getters y Setters (requeridos por Firestore) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public int getEstatura() { return estatura; }
    public void setEstatura(int estatura) { this.estatura = estatura; }

    public List<String> getCondiciones() { return condiciones; }
    public void setCondiciones(List<String> condiciones) { this.condiciones = condiciones; }

    public String getContactoRelacion() { return contactoRelacion; }
    public void setContactoRelacion(String contactoRelacion) { this.contactoRelacion = contactoRelacion; }

    public String getTipoSangre() { return tipoSangre; }
    public void setTipoSangre(String tipoSangre) { this.tipoSangre = tipoSangre; }

    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }

    public String getContactoNombre() { return contactoNombre; }
    public void setContactoNombre(String contactoNombre) { this.contactoNombre = contactoNombre; }

    public String getContactoTelefono() { return contactoTelefono; }
    public void setContactoTelefono(String contactoTelefono) { this.contactoTelefono = contactoTelefono; }
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public Date getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Date fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    // ¿Tiene esta condición? (decide qué secciones muestra el registro diario)
    @Exclude
    public boolean tieneCondicion(String clave) {
        return condiciones != null && condiciones.contains(clave);
    }

    // @Exclude: ayuda SOLO para la pantalla; Firestore NO lo guarda como campo
    @Exclude
    public boolean tieneAlergias() {
        return alergias != null && !alergias.trim().isEmpty()
                && !alergias.trim().equalsIgnoreCase("ninguna");
    }
}
