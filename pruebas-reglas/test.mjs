import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, deleteDoc, collection, query, where, getDocs, serverTimestamp, writeBatch, Timestamp } from 'firebase/firestore';
import fs from 'fs';
const env = await initializeTestEnvironment({ projectId: 'cuidapp-test', firestore: { rules: fs.readFileSync('../firestore.rules','utf8'), host: '127.0.0.1', port: 8080 } });
const alice = env.authenticatedContext('alice').firestore();
const bob = env.authenticatedContext('bob').firestore();
const anon = env.unauthenticatedContext().firestore();
const pac = (uid, extra={}) => ({ uid, nombre:'Juan Pérez', rut:'12345678-5', edad:78, sexo:'Masculino', peso:72.5, estatura:170,
  tipoSangre:'O+', condiciones:['HIPERTENSION','DIABETES'], alergias:'Ninguna', contactoNombre:'Ana Pérez', contactoRelacion:'Hija/o',
  contactoTelefono:'+56912345678', fechaRegistro: serverTimestamp(),
  nacionalidad:'Chilena', fechaNacimiento:'15/01/1950', numeroDocumento:'123.456.789', fechaEmision:'10/03/2020', fechaVencimiento:'15/01/2030', ...extra });
const reg = (uid, pid, fecha, extra={}) => ({ uid, pacienteId:pid, fecha, creado: serverTimestamp(), sistolica:150, diastolica:95, frecuenciaCardiaca:80,
  glucemia:120, momentoGlucemia:'Ayunas', insulina:null, saturacion:null, frecuenciaRespiratoria:null, pesoDiario:null, diuresis:null,
  medicacionTomada:true, dolor:3, notas:null, alertas:['Presión alta 150/95'], ...extra });
let ok=0, bad=0;
async function t(name, p, expectOk) { try { await (expectOk?assertSucceeds(p):assertFails(p)); ok++; console.log('✓', name);} catch(e){ bad++; console.log('✗', name, e.message);} }

await t('alice crea paciente válido', setDoc(doc(alice,'pacientes/p1'), pac('alice')), true);
await t('alice NO crea paciente con uid de bob', setDoc(doc(alice,'pacientes/p2'), pac('bob')), false);
await t('NO acepta campo extra (clave)', setDoc(doc(alice,'pacientes/p3'), pac('alice',{clave:'123'})), false);
await t('NO acepta condición inventada', setDoc(doc(alice,'pacientes/p4'), pac('alice',{condiciones:['CANCER']})), false);
await t('NO acepta peso 500', setDoc(doc(alice,'pacientes/p5'), pac('alice',{peso:500})), false);
await t('NO acepta nombre con números', setDoc(doc(alice,'pacientes/p5f'), pac('alice',{nombre:'Juan 123'})), false);
await t('NO acepta contacto con números', setDoc(doc(alice,'pacientes/p5g'), pac('alice',{contactoNombre:'Ana 2'})), false);
await t('NO acepta alergias con números', setDoc(doc(alice,'pacientes/p5h'), pac('alice',{alergias:'Penicilina 500'})), false);
await t('NO acepta nacionalidad con números', setDoc(doc(alice,'pacientes/p5i'), pac('alice',{nacionalidad:'Chil3na'})), false);
await t('NO acepta peso 11111', setDoc(doc(alice,'pacientes/p5b'), pac('alice',{peso:11111})), false);
await t('NO acepta peso 1 kg', setDoc(doc(alice,'pacientes/p5c'), pac('alice',{peso:1})), false);
await t('NO acepta edad 150', setDoc(doc(alice,'pacientes/p5d'), pac('alice',{edad:150})), false);
await t('NO acepta estatura 300 cm', setDoc(doc(alice,'pacientes/p5e'), pac('alice',{estatura:300})), false);
await t('NO acepta RUT con puntos', setDoc(doc(alice,'pacientes/p7'), pac('alice',{rut:'12.345.678-5'})), false);
await t('NO acepta celular sin +569', setDoc(doc(alice,'pacientes/p8'), pac('alice',{contactoTelefono:'111111111111'})), false);
await t('NO acepta fecha de nacimiento mal escrita', setDoc(doc(alice,'pacientes/p9'), pac('alice',{fechaNacimiento:'1950-01-15'})), false);
await t('acepta cédula vacía (datos opcionales)', setDoc(doc(alice,'pacientes/p10'), pac('alice',{nacionalidad:'', fechaNacimiento:'', numeroDocumento:'', fechaEmision:'', fechaVencimiento:''})), true);
await t('acepta paciente sin condiciones', setDoc(doc(alice,'pacientes/p6'), pac('alice',{condiciones:[]})), true);
await t('sin sesión NO lee', getDoc(doc(anon,'pacientes/p1')), false);
await t('bob NO lee paciente de alice', getDoc(doc(bob,'pacientes/p1')), false);
await t('alice lee su paciente', getDoc(doc(alice,'pacientes/p1')), true);
await t('alice lee doc inexistente (resource==null)', getDoc(doc(alice,'pacientes/nada')), true);
await t('alice lista con where uid', getDocs(query(collection(alice,'pacientes'), where('uid','==','alice'))), true);
await t('lista sin filtro rechazada', getDocs(collection(alice,'pacientes')), false);
await t('alice actualiza su paciente', updateDoc(doc(alice,'pacientes/p1'), {peso:70}), true);
await t('alice NO traspasa paciente a bob', updateDoc(doc(alice,'pacientes/p1'), {uid:'bob'}), false);
await t('bob NO borra paciente de alice', deleteDoc(doc(bob,'pacientes/p1')), false);

const hoy='2026-11-18';
await t('alice crea registro de hoy', setDoc(doc(alice,`pacientes/p1/registros/${hoy}`), reg('alice','p1',hoy)), true);
await t('bob NO crea registro en paciente de alice', setDoc(doc(bob,`pacientes/p1/registros/2026-11-17`), reg('bob','p1','2026-11-17')), false);
await t('NO crea si fecha != ID', setDoc(doc(alice,`pacientes/p1/registros/2026-11-16`), reg('alice','p1','2026-11-15')), false);
await t('NO crea con creado falso', setDoc(doc(alice,`pacientes/p1/registros/2026-11-14`), reg('alice','p1','2026-11-14',{creado: Timestamp.fromDate(new Date(2020,0,1))})), false);
await t('NO acepta sistólica 400', setDoc(doc(alice,`pacientes/p1/registros/2026-11-13`), reg('alice','p1','2026-11-13',{sistolica:400})), false);
await t('NO acepta momento inventado', setDoc(doc(alice,`pacientes/p1/registros/2026-11-12`), reg('alice','p1','2026-11-12',{momentoGlucemia:'Siempre'})), false);
await t('alice lista registros con where uid', getDocs(query(collection(alice,'pacientes/p1/registros'), where('uid','==','alice'))), true);
await t('bob NO lista registros de alice', getDocs(query(collection(bob,'pacientes/p1/registros'), where('uid','==','alice'))), false);
await t('alice edita registro < 24h', updateDoc(doc(alice,`pacientes/p1/registros/${hoy}`), {dolor:5, notas:'Durmió bien'}), true);
await t('NO cambia la fecha del registro', updateDoc(doc(alice,`pacientes/p1/registros/${hoy}`), {fecha:'2026-01-01'}), false);

// Registro antiguo (creado hace 25 h) sembrado sin reglas
await env.withSecurityRulesDisabled(async c => {
  await setDoc(doc(c.firestore(),'pacientes/p1/registros/2026-11-10'), reg('alice','p1','2026-11-10',{creado: Timestamp.fromMillis(Date.now()-25*3600*1000)}));
});
await t('registro > 24h NO se edita', updateDoc(doc(alice,'pacientes/p1/registros/2026-11-10'), {dolor:1}), false);
await t('registro > 24h NO se borra solo', deleteDoc(doc(alice,'pacientes/p1/registros/2026-11-10')), false);
await t('alice borra registro < 24h', deleteDoc(doc(alice,`pacientes/p1/registros/${hoy}`)), true);
const b = writeBatch(alice);
b.delete(doc(alice,'pacientes/p1/registros/2026-11-10')); b.delete(doc(alice,'pacientes/p1'));
await t('borrado en cascada (paciente + registro antiguo) en lote', b.commit(), true);

await t('usuarios: perfil propio sin contraseña', setDoc(doc(alice,'usuarios/alice'), {nombre:'Alice', correo:'a@a.cl', fechaCreacion: serverTimestamp()}), true);
await t('usuarios: NO guarda contraseña', setDoc(doc(alice,'usuarios/alice'), {nombre:'Alice', correo:'a@a.cl', clave:'123456'}), false);
await t('usuarios: NO acepta nombre con números', setDoc(doc(alice,'usuarios/alice'), {nombre:'Alice99', correo:'a@a.cl', fechaCreacion: serverTimestamp()}), false);
await t('usuarios: NO escribe perfil ajeno', setDoc(doc(alice,'usuarios/bob'), {nombre:'x', correo:'x@x.cl'}), false);
await t('otra colección cerrada', setDoc(doc(alice,'otra/x'), {a:1}), false);
console.log(`\n${ok} OK, ${bad} fallidas`);
await env.cleanup();
process.exit(bad?1:0);
