Presentación de 12 minutos dividida en partes iguales. Cada integrante habla 4 minutos y hace su propia demo en vivo.

| Integrante | Tema | Diapositivas | Tiempo |
|---|---|---|---|
| Sebastián | Firebase, Authentication y UID | 1 a 5 | 4:00 |
| Fabián | Datos, CRUD, validaciones y cédula | 6 a 9 | 4:00 |
| Sofía | Registros diarios, reglas y cierre | 10 a 14 | 4:00 |

## Guión por diapositiva

### 1. Portada · Sebastián (0:30)

Buenos días. Somos Fabián, Sofía y Sebastián, y presentamos CuidApp 2.0: una app Android para cuidadores de adultos mayores, hecha en Java y conectada a Firebase. Yo explico la conexión con Firebase y el inicio de sesión, Fabián el CRUD de pacientes y sus validaciones, y Sofía los registros diarios y las reglas de seguridad.

### 2. El problema · Sebastián (0:40)

Un cuidador suele anotar en papel los remedios, la presión y el contacto de emergencia de cada paciente. Ese papel se pierde y nadie más puede consultarlo. CuidApp guarda esa ficha en la nube, ordenada por paciente y protegida por cuenta. Elegimos salud porque nos obliga a tomarnos en serio la seguridad de los datos.

### 3. Cómo está construida · Sebastián (0:50)

La app sigue el patrón MVVM. La Activity solo muestra la pantalla, el ViewModel valida y guarda el estado, y el Repositorio es la única clase que habla con Firebase. El proyecto se conecta con el archivo google-services.json y el BoM de Firebase 35.2.0, que fija versiones compatibles de firebase-auth y firebase-firestore.

### 4. Firebase Authentication · Sebastián (1:10)

Firebase Authentication responde a la pregunta ¿quién eres? Usamos cuatro métodos: createUserWithEmailAndPassword para registrarse, signInWithEmailAndPassword para entrar, getCurrentUser para saber quién está conectado y signOut para salir. Si la contraseña tiene menos de 6 caracteres o el correo ya existe, la app muestra el error.

**Demo:** Registrar una cuenta nueva, cerrar sesión y volver a entrar. Mostrar que el menú dice «Hola, Sebastián».

### 5. El UID une los dos servicios · Sebastián (0:50)

Al registrarse, Firebase entrega un UID único que no cambia. Con ese UID creamos el documento usuarios/{uid} con nombre y correo, nunca la contraseña: esa la guarda Authentication con hash. Cada paciente lleva el uid de su cuidador, y así unimos Authentication con Firestore.

**Demo:** En la consola, mostrar que el UID de Authentication es el mismo ID del documento en «usuarios».

### 6. Datos en Cloud Firestore · Fabián (0:50)

En Firestore tenemos tres niveles. La colección usuarios; la colección pacientes, donde cada documento guarda RUT, edad, peso, estatura, tipo de sangre, condiciones y contacto de emergencia; y, dentro de cada paciente, la subcolección registros, con un documento por día. La lista se consulta con whereEqualTo("uid", uid), así solo llegan los pacientes del cuidador conectado.

### 7. CRUD de pacientes · Fabián (1:30)

Las cuatro operaciones del CRUD: crear usa add, leer usa get con el filtro por uid, actualizar usa update y eliminar usa delete. Al borrar un paciente también se borran sus registros diarios en un mismo WriteBatch, para no dejar datos huérfanos. Los datos quedan en la nube, por eso siguen ahí al volver a abrir la app.

**Demo:** Crear un paciente (mensaje «Paciente guardado»), editar su peso, eliminarlo confirmando el diálogo. Cerrar la app, abrirla y mostrar los 5 pacientes.

### 8. Validaciones · Fabián (1:00)

Antes de guardar validamos cada campo. El RUT se escribe con el guion automático y se revisa el dígito verificador con módulo 11. La estatura se escribe 1,80 y se guarda en centímetros. El peso acepta tres enteros y un decimal, entre 2 y 300 kilos. El celular tiene 8 dígitos y el +56 9 ya viene puesto. Cada error aparece en rojo bajo su campo y se borra al corregirlo.

**Demo:** Intentar guardar con RUT 12345678-9 y peso 0 para mostrar los mensajes rojos.

### 9. Escanear cédula · Fabián (0:40)

Para no escribir todo a mano, el botón Escanear cédula toma una foto y ML Kit lee el texto en el mismo teléfono, sin subir la imagen a ningún servidor. Leemos la zona MRZ del reverso y las etiquetas del frente para llenar RUN, nombres, fecha de nacimiento y vencimiento. El cuidador revisa los datos antes de guardar.

### 10. Registros diarios y dashboard · Sofía (1:00)

Cada día el cuidador anota presión, pulso, glucemia, saturación, dolor y si el paciente tomó sus remedios. Los campos cambian según las condiciones del paciente: a un diabético se le pide glucemia, a uno con EPOC la saturación. Si un valor sale de rango, la app genera una alerta y el dashboard la resume.

**Demo:** Crear el registro de hoy con pulso 110 y mostrar el dashboard con el último pulso en rojo.

### 11. Reglas de seguridad · Sofía (1:20)

Las reglas de seguridad se ejecutan en el servidor de Firebase, así que nadie se las puede saltar desde la app. Exigen sesión iniciada, que el uid del documento sea el del usuario conectado, que solo existan los campos permitidos con tipos y rangos válidos, y que nadie cambie el dueño. Un registro diario solo se edita durante 24 horas. Todo lo que no está declarado queda bloqueado. Las probamos con 41 pruebas automáticas en el emulador.

### 12. Cada cuidador ve lo suyo · Sofía (0:50)

Ahora entro con la cuenta de Fabián. Fabián ve solo su paciente. Los de Sebastián siguen en Firestore, pero ni la consulta ni las reglas se los entregan. Por eso nunca dejamos reglas abiertas: con allow read, write: if true, cualquiera con la app podría leer datos de salud ajenos.

**Demo:** Cerrar sesión, entrar con la cuenta de Fabián y mostrar que la lista cambia.

### 13. Módulos del dashboard · Sofía (0:30)

El dashboard suma tres módulos más: rutinas de ejercicio con temporizador, la versión web de CuidApp dentro de la app y un navegador con sitios oficiales como MINSAL, FONASA y Salud Responde.

### 14. Cierre · Sofía (0:20)

En resumen: Firebase Authentication identifica al cuidador, Firestore guarda sus pacientes y registros, y las reglas aseguran que cada uno vea solo lo suyo. Muchas gracias. ¿Preguntas?

## Preguntas probables

| Responde | Pregunta | Respuesta corta |
|---|---|---|
| Sebastián | ¿Cuál es la diferencia entre Authentication y Firestore? | Authentication maneja cuentas, contraseñas y sesiones. Firestore guarda los datos de la app. Se unen por el UID y por request.auth en las reglas. |
| Sebastián | ¿Por qué no guardan la contraseña en Firestore? | Firestore guarda el dato tal cual. Authentication ya la guarda con hash y nunca la devuelve, así que copiarla solo agrega riesgo. |
| Fabián | ¿Cómo validan el RUT? | Formato 12345678-5 y dígito verificador con módulo 11 (RutUtil.esValido). Las reglas además revisan el formato en el servidor. |
| Fabián | ¿Cómo persisten los datos al cerrar la app? | Se guardan en Cloud Firestore, en la nube. Al abrir, la app vuelve a consultar con el uid del usuario. |
| Sofía | Si ya tienen reglas, ¿para qué filtran con whereEqualTo? | Las reglas no filtran: si la consulta pidiera todos los pacientes, Firestore la rechazaría completa. El filtro y la regla tienen que coincidir. |
| Sofía | ¿Qué pasa si alguien sin sesión intenta leer? | request.auth es null, la regla lo rechaza y la app recibe PERMISSION_DENIED. |

## Antes de presentar

- [ ] 5 pacientes en la cuenta de Sebastián y 1 en la de Fabián.
- [ ] Reglas nuevas publicadas en Firebase.
- [ ] Celular con batería e internet, con la app abierta en el menú.
- [ ] Consola de Firebase abierta en Authentication y en Firestore.
- [ ] Ensayar una vez completo con cronómetro.