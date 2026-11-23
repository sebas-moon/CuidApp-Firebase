package com.santo_tomas.cuidapp.repositorio;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.santo_tomas.cuidapp.model.Paciente;
import com.santo_tomas.cuidapp.model.RegistroDiario;
import com.santo_tomas.cuidapp.model.ResumenDashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * NUEVO - REPOSITORIO DE REGISTROS DIARIOS: CRUD sobre la SUBCOLECCIÓN
 *   pacientes/{pacienteId}/registros/{fecha}
 */
public class RegistroRepositorio {

    private static final String TAG = "RegistroRepositorio";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Referencia a la subcolección de UN paciente
    private CollectionReference registrosDe(String pacienteId) {
        return db.collection("pacientes").document(pacienteId).collection("registros");
    }

    // ---------------------------------------------------------------
    // R - READ (lista) en tiempo real, del más nuevo al más antiguo
    // ---------------------------------------------------------------
    // Se filtra por uid (las reglas lo exigen) y se ordena en el teléfono por fecha.
    // (Ordenar en Firestore con where + orderBy en campos distintos pediría crear un índice.)
    public ListenerRegistration escucharRegistros(String pacienteId, String uid,
                                                  MutableLiveData<List<RegistroDiario>> lista,
                                                  MutableLiveData<String> mensaje) {
        return registrosDe(pacienteId)
                .whereEqualTo("uid", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error al leer registros", error);
                        mensaje.setValue("Error al cargar registros: " + error.getMessage());
                        return;
                    }
                    if (snapshot != null) {
                        List<RegistroDiario> registros = snapshot.toObjects(RegistroDiario.class);
                        // "2026-11-18" se ordena bien como texto: más reciente primero
                        registros.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
                        lista.setValue(registros);
                    }
                });
    }

    // ---------------------------------------------------------------
    // R - READ (uno): el registro de una fecha (null si ese día no hay)
    // ---------------------------------------------------------------
    public void obtener(String pacienteId, String fecha,
                        MutableLiveData<RegistroDiario> resultado,
                        MutableLiveData<Boolean> existe,
                        MutableLiveData<String> mensaje) {
        registrosDe(pacienteId).document(fecha)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        resultado.setValue(doc.toObject(RegistroDiario.class));
                        existe.setValue(true);
                    } else {
                        existe.setValue(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al buscar registro", e);
                    mensaje.setValue("Error al buscar el registro: " + e.getMessage());
                });
    }

    // ---------------------------------------------------------------
    // C - CREATE: el ID del documento es la fecha -> máximo un registro por día
    // ---------------------------------------------------------------
    public void crear(RegistroDiario registro,
                      MutableLiveData<Boolean> guardado,
                      MutableLiveData<String> mensaje,
                      MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        registrosDe(registro.getPacienteId()).document(registro.getFecha())
                .set(registro)
                .addOnSuccessListener(aVoid -> {
                    cargando.setValue(false);
                    mensaje.setValue(textoResultado("Registro del día guardado", registro));
                    guardado.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al crear registro", e);
                    cargando.setValue(false);
                    mensaje.setValue("Error al guardar: " + e.getMessage());
                });
    }

    // ---------------------------------------------------------------
    // U - UPDATE: solo dentro de las 24 horas (la regla de Firestore lo verifica)
    // ---------------------------------------------------------------
    // No se envían uid, pacienteId, fecha ni creado: quedan como estaban.
    public void actualizar(RegistroDiario r,
                           MutableLiveData<Boolean> guardado,
                           MutableLiveData<String> mensaje,
                           MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        Map<String, Object> cambios = new HashMap<>();
        cambios.put("sistolica", r.getSistolica());
        cambios.put("diastolica", r.getDiastolica());
        cambios.put("frecuenciaCardiaca", r.getFrecuenciaCardiaca());
        cambios.put("glucemia", r.getGlucemia());
        cambios.put("momentoGlucemia", r.getMomentoGlucemia());
        cambios.put("insulina", r.getInsulina());
        cambios.put("saturacion", r.getSaturacion());
        cambios.put("frecuenciaRespiratoria", r.getFrecuenciaRespiratoria());
        cambios.put("pesoDiario", r.getPesoDiario());
        cambios.put("diuresis", r.getDiuresis());
        cambios.put("medicacionTomada", r.isMedicacionTomada());
        cambios.put("dolor", r.getDolor());
        cambios.put("notas", r.getNotas());
        cambios.put("alertas", r.getAlertas());

        registrosDe(r.getPacienteId()).document(r.getFecha())
                .update(cambios)
                .addOnSuccessListener(aVoid -> {
                    cargando.setValue(false);
                    mensaje.setValue(textoResultado("Registro actualizado", r));
                    guardado.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar registro", e);
                    cargando.setValue(false);
                    mensaje.setValue("No se pudo actualizar (¿pasaron más de 24 horas?): " + e.getMessage());
                });
    }

    // ---------------------------------------------------------------
    // D - DELETE: solo dentro de las 24 horas
    // ---------------------------------------------------------------
    public void eliminar(RegistroDiario r,
                         MutableLiveData<Boolean> eliminado,
                         MutableLiveData<String> mensaje) {
        registrosDe(r.getPacienteId()).document(r.getFecha())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    mensaje.setValue("🗑️ Registro del " + r.getFecha() + " eliminado");
                    eliminado.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al eliminar registro", e);
                    mensaje.setValue("No se pudo eliminar (¿pasaron más de 24 horas?): " + e.getMessage());
                });
    }

    // ---------------------------------------------------------------
    // NUEVO - RESUMEN DEL DÍA para el Dashboard: pacientes + registros de cada uno
    // ---------------------------------------------------------------
    // Tasks.whenAllSuccess espera a que TODAS las consultas terminen y entrega los resultados juntos.
    public void cargarResumen(String uid, MutableLiveData<ResumenDashboard> resumen,
                              MutableLiveData<String> mensaje, MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        db.collection("pacientes").whereEqualTo("uid", uid).get()
                .addOnSuccessListener(snapPacientes -> {
                    List<Paciente> pacientes = snapPacientes.toObjects(Paciente.class);
                    List<Task<QuerySnapshot>> consultas = new ArrayList<>();
                    for (Paciente p : pacientes) {
                        consultas.add(registrosDe(p.getId()).whereEqualTo("uid", uid).get());
                    }
                    Tasks.whenAllSuccess(consultas)
                            .addOnSuccessListener(resultados -> {
                                List<List<RegistroDiario>> registros = new ArrayList<>();
                                for (Object r : resultados) {
                                    registros.add(((QuerySnapshot) r).toObjects(RegistroDiario.class));
                                }
                                cargando.setValue(false);
                                resumen.setValue(ResumenDashboard.calcular(pacientes, registros,
                                        com.santo_tomas.cuidapp.viewmodel.RegistroViewModel.hoy()));
                            })
                            .addOnFailureListener(e -> error(e, mensaje, cargando));
                })
                .addOnFailureListener(e -> error(e, mensaje, cargando));
    }

    private void error(Exception e, MutableLiveData<String> mensaje, MutableLiveData<Boolean> cargando) {
        Log.w(TAG, "Error al cargar el resumen", e);
        cargando.setValue(false);
        mensaje.setValue("No se pudo cargar el resumen: " + e.getMessage());
    }

    private String textoResultado(String base, RegistroDiario r) {
        if (r.tieneAlertas()) {
            return base + ". ⚠️ " + r.getAlertas().size() + " alerta(s): " + String.join(", ", r.getAlertas());
        }
        return base + ". ✅ Valores dentro de rango";
    }
}
