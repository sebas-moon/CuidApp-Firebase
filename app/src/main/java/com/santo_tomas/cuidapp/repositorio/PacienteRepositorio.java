package com.santo_tomas.cuidapp.repositorio;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.santo_tomas.cuidapp.model.Paciente;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * REPOSITORIO DE PACIENTES: la ÚNICA clase que habla con Cloud Firestore.
 * Aquí están las operaciones CRUD (Crear, Leer, Actualizar, Eliminar).
 */
public class PacienteRepositorio {

    private static final String TAG = "PacienteRepositorio";

    // Nombre de la colección. Si no existe, Firestore la crea con la PRIMERA escritura exitosa.
    private static final String COLECCION = "pacientes";

    // Conexión con Cloud Firestore (usa la configuración de google-services.json)
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ---------------------------------------------------------------
    // C - CREATE: guardar un paciente nuevo (antes se revisa que el RUT no esté repetido)
    // ---------------------------------------------------------------
    public void crear(Paciente paciente,
                      MutableLiveData<Boolean> guardado,
                      MutableLiveData<String> mensaje,
                      MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        verificarRutDisponible(paciente, null, mensaje, cargando, () -> {
            // document() SIN parámetro genera un ID único automático
            DocumentReference ref = db.collection(COLECCION).document();
            ref.set(paciente)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Paciente creado con ID: " + ref.getId());
                        cargando.setValue(false);
                        mensaje.setValue("Paciente registrado correctamente");
                        guardado.setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error al crear", e);
                        cargando.setValue(false);
                        mensaje.setValue("Error al guardar: " + e.getMessage());
                    });
        });
    }

    // ---------------------------------------------------------------
    // R - READ (lista): escuchar EN TIEMPO REAL los pacientes del cuidador
    // ---------------------------------------------------------------
    // whereEqualTo("uid", uid) -> solo los documentos de ESTE usuario (las reglas lo exigen).
    // addSnapshotListener      -> se ejecuta al abrir y CADA VEZ que algo cambia en la nube.
    public ListenerRegistration escucharPacientes(String uid,
                                                  MutableLiveData<List<Paciente>> lista,
                                                  MutableLiveData<String> mensaje,
                                                  MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        return db.collection(COLECCION)
                .whereEqualTo("uid", uid)
                .addSnapshotListener((snapshot, error) -> {
                    cargando.setValue(false);
                    if (error != null) {
                        Log.w(TAG, "Error al leer", error);
                        mensaje.setValue("Error al cargar: " + error.getMessage());
                        return;
                    }
                    if (snapshot != null) {
                        // toObjects convierte TODOS los documentos en objetos Paciente
                        List<Paciente> pacientes = snapshot.toObjects(Paciente.class);
                        pacientes.sort(Comparator.comparing(p -> p.getNombre().toLowerCase()));
                        lista.setValue(pacientes);
                    }
                });
    }

    // ---------------------------------------------------------------
    // R - READ (uno): escuchar UN paciente por su ID (ficha y formulario de edición)
    // ---------------------------------------------------------------
    // Se usa listener (y no get) para que la ficha se actualice sola después de editar.
    public ListenerRegistration escucharPaciente(String id,
                                                 MutableLiveData<Paciente> resultado,
                                                 MutableLiveData<String> mensaje) {
        return db.collection(COLECCION).document(id)
                .addSnapshotListener((documento, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error al buscar", error);
                        mensaje.setValue("Error al buscar: " + error.getMessage());
                        return;
                    }
                    if (documento != null && documento.exists()) {
                        resultado.setValue(documento.toObject(Paciente.class));
                    } else {
                        resultado.setValue(null); // fue eliminado
                    }
                });
    }

    // ---------------------------------------------------------------
    // U - UPDATE: modificar un paciente existente
    // ---------------------------------------------------------------
    // update() cambia SOLO los campos indicados; uid y fechaRegistro quedan intactos.
    // (set() reemplazaría el documento completo).
    public void actualizar(Paciente paciente,
                           MutableLiveData<Boolean> guardado,
                           MutableLiveData<String> mensaje,
                           MutableLiveData<Boolean> cargando) {
        cargando.setValue(true);
        verificarRutDisponible(paciente, paciente.getId(), mensaje, cargando, () -> {
            Map<String, Object> cambios = new HashMap<>();
            cambios.put("nombre", paciente.getNombre());
            cambios.put("rut", paciente.getRut());
            cambios.put("edad", paciente.getEdad());
            cambios.put("sexo", paciente.getSexo());
            cambios.put("peso", paciente.getPeso());
            cambios.put("estatura", paciente.getEstatura());
            cambios.put("tipoSangre", paciente.getTipoSangre());
            cambios.put("condiciones", paciente.getCondiciones());
            cambios.put("alergias", paciente.getAlergias());
            cambios.put("contactoNombre", paciente.getContactoNombre());
            cambios.put("contactoRelacion", paciente.getContactoRelacion());
            cambios.put("contactoTelefono", paciente.getContactoTelefono());
            cambios.put("nacionalidad", paciente.getNacionalidad());
            cambios.put("fechaNacimiento", paciente.getFechaNacimiento());
            cambios.put("numeroDocumento", paciente.getNumeroDocumento());
            cambios.put("fechaEmision", paciente.getFechaEmision());
            cambios.put("fechaVencimiento", paciente.getFechaVencimiento());

            db.collection(COLECCION).document(paciente.getId())
                    .update(cambios)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Paciente actualizado: " + paciente.getId());
                        cargando.setValue(false);
                        mensaje.setValue("Paciente actualizado correctamente");
                        guardado.setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error al actualizar", e);
                        cargando.setValue(false);
                        mensaje.setValue("Error al actualizar: " + e.getMessage());
                    });
        });
    }

    // ---------------------------------------------------------------
    // D - DELETE: borrar un paciente Y sus registros diarios
    // ---------------------------------------------------------------
    // Firestore NO borra las subcolecciones automáticamente. Si solo borráramos el paciente,
    // sus registros quedarían "huérfanos". Por eso usamos un WriteBatch: todos los borrados
    // se aplican JUNTOS (o ninguno, si algo falla).
    public void eliminar(Paciente paciente,
                         MutableLiveData<Boolean> eliminado,
                         MutableLiveData<String> mensaje) {
        DocumentReference refPaciente = db.collection(COLECCION).document(paciente.getId());

        refPaciente.collection("registros")
                .whereEqualTo("uid", paciente.getUid()) // las reglas exigen filtrar por dueño
                .get()
                .addOnSuccessListener(registros -> {
                    WriteBatch lote = db.batch();
                    for (DocumentSnapshot doc : registros.getDocuments()) {
                        lote.delete(doc.getReference());
                    }
                    lote.delete(refPaciente);
                    lote.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Paciente eliminado: " + paciente.getId()
                                        + " (" + registros.size() + " registros)");
                                mensaje.setValue(paciente.getNombre() + " fue eliminado");
                                eliminado.setValue(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error al eliminar", e);
                                mensaje.setValue("Error al eliminar: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al leer registros", e);
                    mensaje.setValue("Error al eliminar: " + e.getMessage());
                });
    }

    // ---------------------------------------------------------------
    // Validación con la base de datos: el mismo cuidador no puede repetir un RUT.
    // Busca pacientes del cuidador con ese RUT; si hay uno (distinto al que se edita), avisa.
    // ---------------------------------------------------------------
    private void verificarRutDisponible(Paciente paciente, String idExcluir,
                                        MutableLiveData<String> mensaje,
                                        MutableLiveData<Boolean> cargando,
                                        Runnable siDisponible) {
        db.collection(COLECCION)
                .whereEqualTo("uid", paciente.getUid())
                .whereEqualTo("rut", paciente.getRut())
                .get()
                .addOnSuccessListener(resultado -> {
                    for (DocumentSnapshot doc : resultado.getDocuments()) {
                        if (!doc.getId().equals(idExcluir)) {
                            cargando.setValue(false);
                            mensaje.setValue("Ya tienes un paciente registrado con el RUT " + paciente.getRut());
                            return;
                        }
                    }
                    siDisponible.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al verificar RUT", e);
                    cargando.setValue(false);
                    mensaje.setValue("Error al verificar el RUT: " + e.getMessage());
                });
    }
}
