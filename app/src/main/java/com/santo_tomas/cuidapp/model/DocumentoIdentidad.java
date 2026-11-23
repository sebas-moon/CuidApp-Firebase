package com.santo_tomas.cuidapp.model;

/*
 * NUEVO - Datos de la cédula de identidad que se guardan junto al paciente.
 * Todos son opcionales (texto vacío si no se ingresaron). Las fechas van como "dd/MM/yyyy".
 * La FIRMA no se guarda: en la cédula es una imagen, no texto que se pueda leer.
 */
public class DocumentoIdentidad {

    public String nacionalidad = "";
    public String fechaNacimiento = "";
    public String numeroDocumento = "";
    public String fechaEmision = "";
    public String fechaVencimiento = "";

    public DocumentoIdentidad() { }

    public DocumentoIdentidad(String nacionalidad, String fechaNacimiento, String numeroDocumento,
                              String fechaEmision, String fechaVencimiento) {
        this.nacionalidad = nacionalidad;
        this.fechaNacimiento = fechaNacimiento;
        this.numeroDocumento = numeroDocumento;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
    }
}
