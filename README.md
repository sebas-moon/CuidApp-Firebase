<div align="center">

# 🩺 CuidApp: Tu Asistente de Salud Integral 💙

[![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84.svg?style=for-the-badge&logo=android-studio&logoColor=white)](#)
[![Java](https://img.shields.io/badge/Java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)](#)
[![Status](https://img.shields.io/badge/Status-Completado-success?style=for-the-badge)](#)

*App Android (Java, MVVM) para cuidadores: CRUD de pacientes y registros diarios de salud con Firebase Auth y Cloud Firestore, reglas de seguridad por UID, validación de RUT, lectura de cédula con ML Kit y dashboard de monitoreo. Proyecto IGR00164, Santo Tomás.*

</div>

---

## 📁 Documentos de la entrega

| Archivo | Contenido |
| :---

## 🔥 Versión 2.0: CuidApp + Firebase (Actividad con nota)

Esta versión conecta CuidApp a **Firebase** y cambia la ficha de un solo paciente (guardada en `SharedPreferences`, solo en el teléfono) por un **CRUD de pacientes en Cloud Firestore**, protegido con **Firebase Authentication**.

| Mejora | Antes (v1) | Ahora (v2) |
| :--- | :--- | :--- |
| Acceso | Cualquiera abría la app | Registro e inicio de sesión con correo y contraseña |
| Pacientes | 1 paciente fijo en el teléfono | Varios pacientes por cuidador, en la nube |
| Persistencia | `SharedPreferences` | Cloud Firestore (colección `pacientes`) |
| Validaciones | Ninguna al editar | Error bajo cada campo, RUT con dígito verificador, teléfono, edad, RUT no repetido |
| Seguridad | — | Reglas de Firestore: cada cuidador solo ve sus pacientes |
| Arquitectura | Plana | Plana en las pantallas originales + MVVM (model / repositorio / viewmodel) para Firebase |
| Ficha | Datos fijos | Datos en tiempo real, llamar al contacto de emergencia, enviar ficha por correo, editar y eliminar |
| Perfil clínico | Nombre, RUT, sangre y alergias | + sexo, peso, estatura, condiciones (Hipertensión, Diabetes, EPOC/Asma, Enf. renal) y relación del contacto |
| Seguimiento | — | **Registros diarios** por paciente (subcolección `registros`): formulario según condición, alertas por rangos clínicos, bloqueo a las 24 h |

### Antes de ejecutar (obligatorio)

1. Crear un proyecto en https://console.firebase.google.com
2. Agregar una app Android con el paquete **com.santo_tomas.cuidapp**.
3. Descargar `google-services.json` y copiarlo en la carpeta `app/`.
4. **Authentication > Método de acceso > Correo electrónico/contraseña > Habilitar**.
5. **Firestore Database > Crear base de datos** (modo producción).
6. **Firestore > Reglas**: pegar `firestore.rules` y **Publicar**.

### Archivos nuevos

| Archivo | Función |
| :--- | :--- |
| `LoginActivity` (Launcher) / `RegistroActivity` | Inicio de sesión y registro del cuidador |
| `PacientesActivity` + `PacienteAdapter` | Lista en tiempo real de los pacientes (READ) |
| `FormularioPacienteActivity` | Crear (CREATE) y editar (UPDATE) con validaciones |
| `FichaPacienteActivity` (reescrita) | Detalle desde Firestore, eliminar (DELETE), llamar, correo, foto |
| `model/Paciente` | Documento de la colección `pacientes` |
| `repositorio/AuthRepositorio` | Firebase Authentication |
| `repositorio/PacienteRepositorio` | CRUD en Cloud Firestore |
| `viewmodel/AuthViewModel`, `viewmodel/PacienteViewModel` | Lógica y LiveData |
| `util/RutUtil` | Validación del RUT chileno (módulo 11) |
| `RegistrosActivity` + `RegistroAdapter` | Historial de registros diarios del paciente, con alertas en rojo |
| `FormularioRegistroActivity` | CRUD del registro del día: secciones según condición, solo lectura después de 24 h |
| `model/RegistroDiario`, `repositorio/RegistroRepositorio`, `viewmodel/RegistroViewModel` | MVVM de los registros |
| `util/Condiciones`, `util/RangosClinicos` | Catálogo de condiciones y cálculo de alertas |
| `firestore.rules` | Reglas de seguridad |
| `pruebas-reglas/` | 46 pruebas automáticas de las reglas en el emulador (`npm install` y `npm test`) |
| `util/CedulaParser`, `util/Mascaras`, `util/Formatos`, `util/Fechas` | Escaneo de cédula (ML Kit), RUT `12345678-5`, estatura `1,80`, celular `+56 9` y fechas |
| `DashboardViewModel`, `model/ResumenDashboard` | Monitoreo diario con datos reales (sin BPM al azar) |
| `EjercicioActivity`, `WebCuidAppActivity`, `NavegadorActivity` | Pestañas Ejercicio, Web y Navegador del monitoreo |
| `app/src/test/` | 19 pruebas unitarias (cédula, formatos, máscaras, textos sin números y resumen del día) |

### Estructura en Firestore

```
usuarios/{uid}                         nombre, correo, fechaCreacion (sin contraseña)
pacientes/{pacienteId}                 uid del cuidador + datos del paciente
pacientes/{pacienteId}/registros/{yyyy-MM-dd}   un registro por día, editable 24 h
```

Ideas de la demo web que quedan como trabajo futuro: roles (familiar / cuidador / profesional), invitar a otros usuarios a un paciente y acceso sin contraseña.

---

## 📌 Resumen del Proyecto

**CuidApp** es una solución móvil nativa para Android construida con arquitectura plana (Java/XML, sin MVC/MVVM). Consta de **4 Activities** y **1 Service**. Permite navegar por distintas herramientas médicas, contactar a un cuidador o servicio de emergencia, ubicar hospitales en el mapa y mantener un monitoreo activo del paciente.

### ⚙️ Entorno de Desarrollo
- **Android SDK (Min / Target / Compile):** API 31 (Android 12) / API 36 (Android 16) / API 36
- **Lenguaje:** Java
- **Interfaz:** XML con Material Components (tema oscuro Material 3)
- **Paquete:** `com.santo_tomas.cuidapp`

### 🗂️ Estructura de pantallas

| Componente | Archivo | Función |
| :--- | :--- | :--- |
| Activity (Launcher) | `MenuPrincipalActivity` | Menú principal, mapas y navegador |
| Activity | `FichaPacienteActivity` | Datos del paciente, llamada, correo y cámara |
| Activity | `MisExpedientesActivity` | Expedientes médicos (selector de PDF) |
| Activity | `DashboardActivity` | Monitoreo diario con datos reales: último pulso, registros de hoy, ejercicio, medicación y alertas |
| Activity | `EjercicioActivity` / `WebCuidAppActivity` / `NavegadorActivity` | Pestañas del monitoreo: rutinas con temporizador, CuidApp Web en un WebView y sitios útiles en el navegador |
| Activity | `BaseActivity` | Clase base de las 4 pantallas |
| Utilidad | `InsetsUtil` | Evita que el contenido quede bajo las barras del sistema (edge-to-edge) |

---

## 🚀 Navegación y Lógica (Los 8 Intents Implementados)

El núcleo de la aplicación utiliza **8 Intents** (3 explícitos y 5 implícitos). Cada uno está señalado con un comentario `INTENT #n` en el código fuente.

### 🎯 3 Intents Explícitos (Navegación Interna y Servicios)

| # | Origen | Destino | Método en el código | Propósito |
| :---: | :--- | :--- | :--- | :--- |
| **1** | `MenuPrincipalActivity` | `FichaPacienteActivity`, `MisExpedientesActivity`, `DashboardActivity` | `navegarA()` | Navegación desde el menú hacia el resto de las vistas. |
| **2** | `FichaPacienteActivity`, `MisExpedientesActivity`, `DashboardActivity` | `MenuPrincipalActivity` | `volverAlMenu()` | Botón de retorno global. Usa `CLEAR_TOP` + `SINGLE_TOP` para no duplicar pantallas en la pila. |
| **3** | `DashboardActivity` | `EjercicioActivity`, `WebCuidAppActivity`, `NavegadorActivity` | barra inferior | Abre las pestañas Ejercicio, Web y Navegador. |

### 🌐 5 Intents Implícitos (Acciones Nativas del SO)

| # | Ubicación | Acción | Método | Funcionalidad |
| :---: | :--- | :--- | :--- | :--- |
| **4** | `FichaPacienteActivity` | `ACTION_DIAL` | `llamarEmergencia()` | Abre el marcador con el número de emergencia (131). |
| **5** | `FichaPacienteActivity` | `ACTION_SENDTO` (`mailto:`) | `enviarCorreo()` | Abre el cliente de correo para escribir al cuidador. |
| **6** | `FichaPacienteActivity` | `ACTION_IMAGE_CAPTURE` | `abrirCamara()` | Abre la cámara para tomar la foto de perfil. |
| **7** | `MenuPrincipalActivity` | `ACTION_VIEW` (`geo:`) | `abrirMapaHospitales()` | Abre la app de mapas buscando hospitales cercanos. |
| **8** | `MenuPrincipalActivity` | `ACTION_VIEW` (`https:`) | `abrirPortalMinsal()` | Abre el navegador en el portal del Ministerio de Salud. |

### ➕ Funcionalidades extra (fuera de los 8 intents de la rúbrica)

Estas dos funciones usan intents implícitos adicionales y están marcadas como `EXTRA` en el código:

| Ubicación | Acción | Funcionalidad |
| :--- | :--- | :--- |
| `DashboardActivity` | `ACTION_SEND` | Compartir el reporte de salud como texto. |
| `MisExpedientesActivity` | `ACTION_GET_CONTENT` | Seleccionar un PDF desde el explorador de archivos y mostrar su nombre. |

---

## 🛡️ Validaciones y Manejo de Errores (Criterio 2)

| Riesgo | Cómo se previene |
| :--- | :--- |
| No existe una app que atienda el intent implícito (ej. sin cliente de correo) | Cada intent implícito se lanza dentro de `try/catch (ActivityNotFoundException)` y se muestra un aviso al usuario. En el Manifest se declara `<queries>` para que Android 11+ permita resolver esas apps. |
| Vistas nulas (ID inexistente en el layout) | Se valida `findViewById() != null` antes de asignar listeners. |
| Intents o extras nulos en el `BroadcastReceiver` | Se comprueba `intent`, `getAction()` y el valor del extra antes de usarlos. |
| Dispositivo sin cámara | Se verifica `FEATURE_CAMERA_ANY` antes de abrir la cámara. |
| Cámara sin permiso concedido | Se solicita en tiempo de ejecución (Activity Result API). Si se deniega, se informa y la app no se cierra. |
| Ubicación sin permiso concedido | Se solicita en tiempo de ejecución (precisa y aproximada). Si se deniega, el mapa se abre igualmente, sin usar la posición. |
| Archivo de expediente ilegible | Se valida el resultado del selector y se muestra un aviso si no se puede leer el nombre. |

### 🔐 Permisos declarados en el Manifest

| Permiso | Uso | ¿En tiempo de ejecución? |
| :--- | :--- | :---: |
| `CAMERA` | Foto de perfil del paciente | ✅ Sí |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Centrar la búsqueda de hospitales | ✅ Sí |
| `INTERNET` | Abrir el Portal Minsal | No (permiso normal) |

> 🔒 `allowBackup` está en `false` porque la aplicación maneja datos de salud.

---

## 📸 Capturas de Pantalla (UI/UX)

La interfaz usa un tema oscuro con recursos centralizados en `strings.xml` (textos con emojis 🩺 y 📈) y `colors.xml` (paleta de colores).

<div align="center">

| 🏠 Menú Inicial | 👤 Ficha del Paciente |
| :---: | :---: |
| <img src="URL_IMAGEN_1" width="250" alt="Captura Menu Principal"> | <img src="URL_IMAGEN_2" width="250" alt="Captura Ficha Paciente"> |

| 🩺 Mis Expedientes | 📈 Resumen de Salud |
| :---: | :---: |
| <img src="URL_IMAGEN_3" width="250" alt="Captura Mis Expedientes"> | <img src="URL_IMAGEN_4" width="250" alt="Captura Dashboard"> |

</div>

---

## 🧠 Características Técnicas Destacadas

*   **Monitoreo con datos reales:** la versión anterior inventaba un BPM al azar con un Service y un Thread. Ahora el Dashboard calcula el resumen del día con los registros guardados en Firestore (`Tasks.whenAllSuccess` junta las consultas de todos los pacientes). Cuando se integre el módulo ECG, el pulso vendrá del sensor.
*   **Temporizador que sobrevive a la rotación:** el `CountDownTimer` de Ejercicio vive en un `ViewModel`, no en la Activity.
*   **Escaneo de cédula sin internet:** ML Kit reconoce el texto en el teléfono y `CedulaParser` (Java puro, con pruebas unitarias) saca nombre, RUN, sexo, nacionalidad y fechas, desde el frente o desde la zona MRZ del reverso.
*   **Compatibilidad con Android 15/16:** como `targetSdk` es 36, las pantallas se dibujan de borde a borde. `InsetsUtil` aplica el espacio de las barras del sistema para que nada quede tapado.
*   **Gestión de Recursos Centralizada:** todos los textos (incluidos mensajes de error, títulos de alerta y las URI de los intents) están en `res/values/strings.xml`. Los colores están en `colors.xml` y los íconos en `res/drawable/`. No hay textos escritos directamente en el código Java.

---

## 📦 Ubicación del Ejecutable

El archivo binario pre-compilado para pruebas se encuentra en:

📂 **APK Debug:** `release/app-debug.apk`

---

## 🌳 Flujo de Trabajo (GitFlow Simplificado)

El desarrollo se gestionó respetando los lineamientos de la Carta Gantt:
1. Rama principal: `main` (versiones estables).
2. Rama de desarrollo: `feature/intents` (implementación de Intents, permisos e interfaces).

---
<div align="center">
  <i>Desarrollado para el Proyecto de Arquitectura Android - CuidApp</i> 🏥
</div>
