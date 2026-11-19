# cd2026mnd1g1 (guía rápida)

Este proyecto es una base completa con **backend Java (Spring Boot)**, **frontend Angular** y **base de datos**.  
La idea es que puedas arrancarlo, probarlo y entender la estructura sin necesitar experiencia avanzada.


- Iniciar sesión desde frontend.
- Consumir una API REST de usuarios (`/api/users`).
- Probar endpoints desde Swagger.
- Trabajar con 3 modos de ejecución del backend: `v`, `p`, `sp`.
- Usar una base en memoria, una base local persistente o PostgreSQL.


| Módulo/carpeta | Qué contiene |
|---|---|
| `cd2026mnd1g1-api` | Contratos: DTOs e interfaces de servicio (qué se expone). |
| `cd2026mnd1g1-model` | Lógica de negocio, entidades JPA y repositorios (cómo se guarda y valida). |
| `cd2026mnd1g1-ws` | Controladores REST y seguridad (endpoints HTTP y control de acceso). |
| `cd2026mnd1g1-openapi` | Especificación OpenAPI (`openapi.yaml` y `openapi-copilot.yaml`). |
| `cd2026mnd1g1-boot` | Arranque Spring Boot, configuración por perfiles y migraciones de base de datos. |
| `cd2026mnd1g1-frontend` | Aplicación Angular (login + pantalla privada + llamadas al backend). |
| `.run` | Lanzadores preparados para IntelliJ (`V`, `P`, `SP` y `DB`). |
| `.github/copilot-instructions.md` | Reglas del proyecto para asistente/copilot (fuente de verdad). |


Estas son las versiones que se utilizaron en este proyecto:

| Herramienta | Versión |
|---|---|
| Node.js | `23.6.0` |
| npm | `10.9.2` |
| Maven | `3.9.10` |
| Java | `25.0.4.1` (Eclipse Adoptium) |
| Angular CLI | `19.2.27` |

> Importante sobre Node.js: Angular CLI marca `23.6.0` como **Unsupported**, por no ser una versión LTS.


- Usa base **en memoria** (no guarda datos al cerrar).
- Es el más simple para practicar.

**En IntelliJ**
1. Ejecuta `V - cd2026mnd1g1Application`.

**Alternativa Maven**
```bash
mvn -pl cd2026mnd1g1-boot spring-boot:run -Dspring-boot.run.profiles=v
```

---

- Usa HSQLDB en archivo local (`.local/hsqldb/appdb`), así los datos **se conservan**.
- Necesita **arrancar la base de datos antes** que el backend.
- Puedes consultar la BD desde la consola H2 del navegador, pero para trabajo más cómodo normalmente también se usan clientes externos como **DBeaver**.

**En IntelliJ (orden obligatorio)**
1. Ejecuta `DB - HSQLDB Server`.
2. Ejecuta `P - cd2026mnd1g1Application`.

**Alternativa Maven (dos terminales)**
Terminal 1 (BD):
```bash
mvn -pl cd2026mnd1g1-boot -Prun_database validate exec:java
```
Terminal 2 (backend):
```bash
mvn -pl cd2026mnd1g1-boot -Prun_app_p spring-boot:run
```

---

- Usa PostgreSQL en `localhost:5432`, base `appdb`.
**En IntelliJ**
1. Asegúrate de tener PostgreSQL levantado con esa base.
2. Ejecuta `SP - cd2026mnd1g1Application`.

**Alternativa Maven**
```bash
mvn -pl cd2026mnd1g1-boot spring-boot:run -Dspring-boot.run.profiles=sp
```


| Uso | URL |
|---|---|
| Backend (base) | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI automática | `http://localhost:8080/v3/api-docs` |
| OpenAPI Copilot (manual) | `http://localhost:8080/openapi-copilot.yaml` |
| Consola H2 (navegador) | `http://localhost:8080/h2-console` |

> La consola web de BD incluida en el proyecto es la de H2 (`/h2-console`) y funciona bien para pruebas rápidas.  
> Aun así, para consultas más largas, filtros, exportaciones y manejo avanzado, lo habitual es usar un cliente externo.
> - En modo `p` (HSQLDB persistente): Suele resultar más práctico usar herramientas externas como **DBeaver**.
> - En modo `sp` (PostgreSQL): Normalmente se utiliza **pgAdmin** o **DBeaver**.


Abre una terminal en `cd2026mnd1g1-frontend` y ejecuta:

```bash
npm install
npm start
```

Luego abre:

```text
http://localhost:4200
```

Notas útiles:
- El frontend tiene proxy configurado: las llamadas a `/api` se redirigen a `http://localhost:8080`.
- Si backend no está levantado, el login mostrará error de conexión.


En el arranque se crea un usuario admin:
- usuario: `admin`
- contraseña: `admin1234`

Usuarios de demostración disponibles tras ejecutar las migraciones:
- `admin` o `admin@guarderiademo.test` / `admin1234`
- `recepcionista` o `recepcionista@guarderiademo.test` / `admin1234`
- `tutor` o `tutor@guarderiademo.test` / `admin1234`

La autenticación usa las tablas canónicas `USUARIOS` y `ROL`. Los permisos se asocian a los roles mediante `rol_permiso`; las migraciones Flyway consolidan las tablas históricas de usuarios y roles.

## Historial del cambio de autenticación
- Plan aplicado (aprobado el 17/11/2026): consolidar usuarios y roles, conservar el login por username/email, sembrar cuentas demo con rol asignado y mantener permisos asociados a `ROL`.

## Consulta de reservas para recepción
- Plan aplicado (aprobado el 17/11/2026): consulta de solo lectura por fecha, detalle protegido por rol y centro, presencia derivada de `check_registro`, sin cambios de esquema.
- La zona horaria para “hoy” es `Europe/Madrid`. La API permite consultar cualquier fecha con paginación (50 resultados por defecto, máximo 100).
- Recepcionista, Admin y Gerente pueden consultar las reservas de su propio centro. Tutor y usuario del centro cliente B2B no pueden acceder. No se acepta un centro enviado por el navegador.
- Se muestran también las reservas canceladas. Se distinguen del estado de presencia y no se presentan como llegadas pendientes. Una salida sin entrada previa se marca como incoherente.
- En el tablero, las acciones de llegada y salida quedan disponibles según las reglas de sus secciones. No permiten crear/editar reservas ni cancelarlas. Al recargar una ruta protegida habrá que iniciar sesión de nuevo; después se recupera la ruta y la fecha solicitadas.
- La base de datos no registra si `MEDIA_JORNADA` es de mañana o tarde. Los datos de Kira usan ese valor sin asignar una franja. Una reserva cancelada libera capacidad de grupo, pero la restricción única de perro y fecha impide una segunda reserva para el mismo perro y día.

### Cuentas y carga de datos manuales
Las cuentas `admin`, `recepcionista` y `tutor` se crean por migraciones existentes. Para los casos `gerente` y `cliente-b2b`, el script reutiliza hashes BCrypt ya guardados; no contiene contraseñas en texto plano.

1. Arranca la base local persistente y el backend con el perfil `p` (ver “Perfil p” arriba).
2. Desde DBeaver, conéctate al HSQLDB local y ejecuta `scripts/dev/seed-reception-demo.sql`. Está preparado para repetirse en la misma fecha sin duplicar tutores, perros, reservas ni registros de entrada/salida; guarda claves de fixtures en la tabla auxiliar local `dev_fixture_registry`.
3. El script es solo para desarrollo local: no lo ejecutes contra PostgreSQL compartido ni producción. No se carga automáticamente y no borra datos.
4. Comprueba que `SELECT CURRENT_DATE` en HSQLDB coincide con la fecha de `Europe/Madrid`; esa fecha es D. El script crea reservas D−1, D y D+1.

| Cuenta/rol | Centro o ámbito | Perros/tutores y fecha | Estado y registros | Resultado esperado |
|---|---|---|---|---|
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Luna / Ana Núñez, D | Confirmada; sin entrada | Aparece como “Sin entrada registrada”. |
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Bruno / Ana Núñez, D | Confirmada; entrada registrada | Aparece “En el centro”. |
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Kira / Belén López, D | Confirmada, media jornada; sin entrada | Aparece sin franja horaria inventada. |
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Coco / César Vázquez, D | Completada; entrada y salida | Aparece “Salida registrada”. |
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Nala / Inés Peña, D | Cancelada; sin entrada ni salida | Aparece claramente como cancelada, no como llegada pendiente. |
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Otra Luna / Inés Peña, D | Confirmada; sin entrada | Se distingue de la otra Luna por tutor y referencia. |
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Kira / Belén López, D−1 | Confirmada; sin entrada | Solo aparece al consultar D−1. |
| `recepcionista` / Recepcionista | Guardería Demo (propia) | Luna y Bruno / Ana Núñez, D+1 | Confirmadas; sin entrada | Solo aparecen al consultar D+1. |
| `tutor` / Tutor | Guardería Demo (propia) | Cualquier fecha | Sin permisos de recepción | Respuesta de acceso denegado. |
| `cliente-b2b` / Tutor en centro CLIENTE | Guardería Cliente Demo | Sin reservas de guardería | Sin permisos de recepción | No puede consultar ni siquiera cambiando el ID. |
| `admin` / Admin y `gerente` / Gerente | Guardería Demo (propia) | Cualquier fecha | Solo consulta | Acceden al listado y detalle de su centro. |

El script deja vacío el campo opcional de raza de Coco. Para localizar un día sin reservas sin asumir una fecha fija, ejecuta en HSQLDB:

```sql
SELECT CURRENT_DATE + days.X AS fecha_sin_reservas
FROM SYSTEM_RANGE(2, 365) days
WHERE NOT EXISTS (
    SELECT 1
    FROM reserva r
    JOIN perro p ON p.id = r.perro_id
    JOIN centro c ON c.id = p.centro_id
    WHERE c.cif = 'B00000000'
      AND r.fecha = CURRENT_DATE + days.X
)
ORDER BY days.X
FETCH FIRST 1 ROW ONLY;
```

Los tests automatizados usan su propia base HSQLDB de pruebas y no dependen de este script ni de los datos manuales. En los tests, la presencia se comprueba desde `check_registro`; una salida sin entrada se muestra como registro incoherente.

## Registro de llegada en recepción
- Plan aplicado (aprobado por el usuario): solo registrar llegada para reservas confirmadas del día en `Europe/Madrid`; preservar la transición `CONFIRMADA → EN_CURSO` y derivar presencia de `check_registro`. No se crean reservas, no se cambian bonos, grupo, modalidad ni aforo.
- `POST /api/recepcion/reservas/{id}/entrada` está protegido para Recepcionista, Admin y Gerente y limita la reserva al centro del usuario autenticado. La hora y el responsable proceden del servidor/sesión. No admite reservas canceladas, futuras, pasadas, entradas repetidas ni registros incoherentes.
- La reserva se bloquea dentro de la transacción y V7 añade unicidad por `(reserva_id, tipo)`: admite una entrada y una salida, pero no reentradas. Observaciones opcionales (máximo 2000 caracteres) incluyen las pertenencias descritas por recepción; no hay tabla de artículos. El detalle muestra hora, responsable y texto guardado.
- Como las pertenencias son texto y no filas estructuradas, no se puede cotejar ni auditar su devolución individual. El flujo de salida permite declarar en texto lo devuelto y lo pendiente, sin inventariar objetos.
- Antes de aplicar V7 a una base existente, comprobar que no haya duplicados históricos con `SELECT reserva_id, tipo, COUNT(*) FROM check_registro GROUP BY reserva_id, tipo HAVING COUNT(*) > 1;`. La migración no borra ni corrige duplicados: si existen, deben revisarse antes de actualizar.
- La vista muestra alergias y medicación ya registradas en modo de consulta. Una lista vacía significa “sin datos registrados”, no ausencia confirmada de necesidades. La ficha de cuidados es parcial; no se administran ni editan pautas desde la llegada.
- En el tablero, **Registrar llegada** solo aparece para una reserva confirmada de hoy sin entrada. El detalle y el tablero derivan presencia de los registros persistidos. Si se pierde la respuesta de red, consulta el detalle antes de reintentar; el servidor rechaza duplicados.

Para la prueba manual local, carga `scripts/dev/seed-reception-demo.sql` únicamente en la base HSQLDB local y entra con `recepcionista` / `admin1234`. Usa una reserva confirmada de hoy todavía sin entrada, por ejemplo Luna / Ana Núñez o Kira / Belén López si siguen disponibles. Prueba primero sin observaciones y, en otra reserva elegible, con texto como `Mochila azul; correa roja; recipiente de comida identificado`. Cada entrada se persiste y no debe borrarse para repetir pruebas; la repetición debe usar otra reserva de demostración válida y comprobar antes el aforo. El script existente aporta casos de entrada previa, entrada/salida, cancelación, fecha pasada/futura y nombres repetidos. Los escenarios de concurrencia y reintento deben ejecutarse en base de pruebas aislada; no se afirma que se hayan probado manualmente. No se preparan incoherencias en la base local.

| Comprobación | Preparación | Resultado esperado |
|---|---|---|
| Llegada sin texto | Reserva de hoy, confirmada y sin registros; recepción autorizada | `201`, una entrada con hora y usuario del servidor, estado `EN_CURSO`; aforo sin cambios |
| Llegada con pertenencias | Otra reserva elegible; observaciones con varias descripciones | Un solo texto guardado en `check_registro.observaciones`; sin filas de artículos |
| Reintento/duplicado | Repetir POST o concurrir con dos recepcionistas | Una entrada como máximo; segunda petición `409`; sin pertenencias duplicadas |
| Fecha/estado no elegibles | Reserva pasada, futura, cancelada o ya finalizada | Rechazo controlado; sin cambios persistidos |
| Autorización/ámbito | Sin sesión, tutor, cuidador o reserva de otro centro | `401`/`403`/`404`; no se registra la llegada |
| Persistencia en pantalla | Consultar tablero y detalle después del `201` | Presencia “En el centro”, hora, responsable y observaciones visibles |

Las verificaciones de construcción y automatización realizadas se informan por separado al cierre; los escenarios manuales de la tabla son pasos de aceptación, no pruebas manuales ejecutadas.

## Entrega 8: alta de perros y autorizaciones desde el portal tutor

- **Plan aplicado (aprobado por el usuario):** se reutilizan `perro`, `alergia`, `medicacion` y `persona_autorizada`. El tutor y el centro salen de la cuenta autenticada; no se aceptan sus IDs desde el navegador. V11 añade una marca de información declarada por el tutor, auditoría nullable para las autorizaciones existentes y almacenamiento de claves de idempotencia. No edita migraciones anteriores ni crea una tabla paralela de perros.
- En **Mis perros**, **Añadir perro** está disponible también cuando la lista está vacía. Nombre y tamaño son obligatorios; raza, fecha de nacimiento, alergias, medicación y foto son opcionales. La alimentación la gestiona el centro y el temperamento es interno; el portal no los recibe. Se rechazan fechas futuras, valores de catálogo no válidos y filas incompletas. No se registra peso ni se declara que un apartado vacío signifique “sin alergias”.
- El alta marca el perro activo, igual que el alta de recepción. No existe un estado separado de admisión; según la regla existente, ese perro podrá utilizarse para reservar si cumple los demás requisitos. El alta no crea ni modifica reservas, aforo, llegadas o salidas.
- Las alergias y pautas de medicación aportadas por el tutor quedan marcadas para recepción como información declarada, sin implicar revisión, aprobación ni administración. No se crean tareas ni avisos de medicación. La alimentación y el temperamento no se editan desde el portal.
- Las personas nuevas se vinculan al tutor autenticado y autorizan la recogida de cualquiera de sus perros actuales o futuros. Solo se guardan nombre y teléfono; no se crean cuentas. Las autorizaciones existentes se muestran y no se copian ni modifican. La etapa 6 conserva la referencia y el nombre histórico de las salidas existentes.
- `POST /api/tutor/mis-perros` recibe `multipart/form-data`: parte JSON `request`, foto opcional y cabecera `Idempotency-Key` UUID. Devuelve `201` en una alta nueva; un reintento con la misma clave y datos devuelve `200` y el perro original. Reutilizar la clave con datos diferentes produce `409`. La clave se conserva para el reintento mientras se mantiene el formulario abierto; no hay expiración automática del registro de idempotencia, y una interrupción abrupta entre el guardado del archivo y la transacción puede dejar un archivo huérfano.
- Las fotos aceptan JPEG/PNG de hasta 5 MiB, se decodifican y normalizan en almacenamiento privado. Por defecto se usa `${user.home}/.cd2026mnd1g1/dog-photos`; `DOG_PHOTOS_DIR` permite sobrescribir esa ruta, por ejemplo para desarrollo o despliegue. No se aceptan URLs ni rutas del cliente. El sistema de archivos no comparte transacción con la base de datos. Una imagen ausente no bloquea el alta.
- La especificación manual `cd2026mnd1g1-openapi/src/main/resources/openapi-copilot.yaml` documenta el alta, idempotencia, consulta de autorizaciones, límites y errores.

### Datos de desarrollo y pasos manuales

1. En HSQLDB local con perfil `p`, carga primero `scripts/dev/seed-reception-demo.sql` si falta el centro/cuenta base, después `scripts/dev/seed-tutor-portal-demo.sql` y finalmente `scripts/dev/seed-tutor-dog-create-demo.sql`. Los scripts son manuales y repetibles, no borran datos y no deben ejecutarse en una base compartida ni en producción. El último solo añade autorizaciones generales ficticias a `portal-a` y `portal-b`; no precarga el perro que se debe crear.
2. Entra como `portal-c / admin1234` (tutor vinculado sin perros), añade un perro con nombre y tamaño, guarda y verifica que se abre su ficha. Vuelve a **Mis perros**: el perro debe aparecer una sola vez. Cancela otro formulario para verificar que no crea nada.
3. Entra como `portal-a / admin1234` para probar un alta adicional. Puedes introducir alergias, pauta de medicación y una persona autorizada; si hay una coincidencia exacta de nombre y teléfono, la interfaz pide confirmación y no fusiona ni impide registrar otra persona. El formulario no carga la foto de prueba por defecto: selecciona un JPEG/PNG local menor de 5 MiB si quieres comprobar la subida.
4. En la ficha, comprueba los datos aportados, la indicación de datos ausentes y las autorizaciones generales con nombre y teléfono. Las autorizaciones existentes de `portal-a` permanecen sin cambios; `portal-b` solo ve sus propios datos. La cuenta `portal-d` sigue mostrando el error de cuenta sin vínculo.
5. Entra como `recepcionista / admin1234`, busca el perro en **Perros** y abre la ficha: debe usar el mismo registro y mostrar las autorizaciones generales de su tutor. Para verificar la recogida, utiliza una reserva de prueba válida y el flujo normal de llegada/salida; registrar una autorización por sí solo no crea una salida. No reutilices ni modifiques salidas históricas.
6. La ruta de foto es `/api/tutor/mis-perros/{id}/foto` y requiere autenticación del tutor propietario. El almacenamiento usa por defecto `${user.home}/.cd2026mnd1g1/dog-photos`; configura `DOG_PHOTOS_DIR` antes de iniciar el backend solo si necesitas elegir otra ubicación. El fixture de imagen previo `scripts/dev/private-dog-photos/portal-dog-a1.png` es solo para lectura y no se asocia a los perros que se creen manualmente.

### Pruebas y estado de verificación

- Se añadieron pruebas de integración para permisos, aislamiento, cuenta sin vínculo, alta, transacción ante una autorización incompleta, repetición idempotente y consulta de autorizaciones desde recepción. Se añadieron pruebas unitarias de foto válida, rechazo de contenido falso, límite de tamaño y limpieza. No se ejecutó `mvn test` durante esta implementación.
- Verificaciones ejecutadas en esta implementación: `mvn -q -pl cd2026mnd1g1-boot -am test-compile` y `npm run build` desde `cd2026mnd1g1-frontend`, ambos finalizaron correctamente. Angular informa advertencias de presupuesto SCSS, incluida la hoja de estilos del portal; no son errores de compilación.
- No se ejecutaron pruebas manuales ni una prueba real de subida HTTP. El paso manual anterior describe cómo comprobarlo; los tests añadidos usan datos aislados y no dependen de la base local.

## Edición de perros y autorizaciones desde el portal tutor

- **Plan aplicado (decisiones aprobadas):** se reutilizan las fichas y relaciones existentes. La identidad, el tutor y el centro se resuelven desde la sesión; el tutor no puede cambiar titularidad ni editar perros ajenos. El alcance de autorización sigue siendo general para perros presentes y futuros del mismo tutor.
- Desde la ficha se puede editar nombre, raza, tamaño, nacimiento, foto, alergias y medicación declaradas por el tutor, y añadir, modificar o revocar personas autorizadas. No se alteran alergias ni medicación registradas por el centro. Las autorizaciones omitidas del conjunto se revocan lógicamente y conservan auditoría; las salidas históricas no se modifican.
- Alimentación: el tutor la consulta, pero no la edita. Temperamento: es información interna para la gestión de grupos y no se devuelve en las respuestas del portal tutor. Ambos campos se conservan en la ficha y recepción mantiene su acceso. Los datos de cuidado quedan separados por procedencia; la edición no los convierte en pautas aprobadas ni administradas.
- `GET /api/tutor/mis-perros/{id}` devuelve `ETag`; `PUT /api/tutor/mis-perros/{id}` requiere `If-Match` y recibe un formulario multipart con los campos permitidos, foto opcional y el conjunto de autorizaciones. Un ETag obsoleto devuelve `409`. La transacción cubre datos relacionales; el sistema de archivos se compensa por separado al fallar la persistencia.
- La foto puede conservarse, reemplazarse o retirarse. Sigue siendo JPEG/PNG de hasta 5 MiB en almacenamiento privado; el cambio de archivo no comparte transacción con la base de datos. No se aceptan rutas ni URLs del cliente.
- Migración V12 añade procedencia y auditoría de edición/revocación; no modifica migraciones aplicadas. `openapi-copilot.yaml` documenta el contrato actualizado.

### Comprobaciones

- Para verificar la edición, abre un perro propio en **Mis perros**, usa **Editar ficha**, cambia datos básicos y filas propias de alergia/medicación y guarda. Confirma que la ficha actualizada se abre, que recepción ve los mismos cambios y que los registros de cuidados del centro permanecen intactos.
- Añade una persona autorizada desde la edición; modifica su nombre o teléfono y luego retírala tras confirmar. La autorización se aplica a todos los perros actuales y futuros del tutor. En recepción, las nuevas recogidas solo deben ofrecer autorizaciones activas; las salidas pasadas conservan el destinatario registrado.
- Alimentación aparece solo como lectura en la ficha tutor. Temperamento no aparece en el portal tutor y continúa disponible en recepción. Cancelar no persiste cambios; una edición concurrente exige recargar la ficha y revisar el formulario.
- La prueba de integración genera cuentas, perros, alergias, medicación y autorizaciones aleatorios en HSQLDB en memoria; no requiere ni modifica los fixtures locales. Cubre aislamiento, modificación versionada, conservación de datos del centro, actualización y revocación de autorizaciones e idempotencia del alta.
- Verificado: `mvn -q -pl cd2026mnd1g1-boot -am -Dtest=TutorPortalControllerITTest -Dsurefire.failIfNoSpecifiedTests=false test`, `npm run build` en `cd2026mnd1g1-frontend`, análisis sintáctico de `openapi-copilot.yaml` y `git diff --check`. No se ejecutó una prueba manual de interfaz ni se levantó el servidor.

## Registro de salida y entrega en recepción
- Plan aplicado (aprobado por el usuario el 19/11/2026): registrar una salida solo para una estancia EN_CURSO con entrada válida y sin salida; preservar la entrada y cerrar con `COMPLETADA`, sin cambiar fecha, modalidad, grupo ni bono. El centro, responsable y hora actual se obtienen del servidor. Una estancia abierta de una fecha anterior puede cerrarse ahora, con aviso en pantalla; no se retrocede ni inventa la hora.
- `POST /api/recepcion/reservas/{id}/salida` y los datos auxiliares están protegidos para Recepcionista, Admin y Gerente, y limitados al centro autenticado. La reserva se bloquea dentro de la transacción y V7 permite como máximo una entrada y una salida. Una repetición o carrera devuelve un conflicto; no sobrescribe al destinatario ganador. La ruta genérica de check-registros ya no acepta `SALIDA`.
- La persona que recoge se representa mediante una referencia inequívoca al tutor del perro o a una persona autorizada de ese tutor, junto con una copia histórica del nombre. Al guardar se vuelve a validar la autorización dentro de su tutor; no se admiten personas escritas libremente, IDs ajenos, excepciones administrativas ni alta rápida. La relación existente es por tutor, así que su autorización puede aplicarse a varios perros de ese tutor; no hay autorización por perro, estados inactivos ni caducidad en el modelo.
- V8 conserva el esquema ya aplicado (`recogido_nombre VARCHAR(120)`); V9 amplía esa columna a `VARCHAR(281)` mediante una migración nueva. No se debe modificar una migración aplicada ni ejecutar `repair` para ocultar diferencias de checksum.
- **Dependencia operativa pendiente:** aunque la tabla `persona_autorizada` existe, el producto aún no ofrece una pantalla/procedimiento aprobado para mantenerla. Las altas de demostración solo se cargan localmente con el fixture. Si no existe un registro autorizado utilizable (y el tutor no está vinculado), la salida se bloquea; no se debe presentar la gestión operativa como terminada.
- La entrada y la salida se conservan. La presencia cambia de “En el centro” a “Salida registrada”; el estado de reserva pasa de `EN_CURSO` a `COMPLETADA`, transición ya existente. El aforo cuenta reservas no canceladas, por lo que la salida no libera plaza ni cambia modalidad o fecha. Tampoco consume ni devuelve bonos. Peluquería no se finaliza ni bloquea automáticamente.
- La salida solicita únicamente observaciones opcionales (máximo 2000 caracteres), en texto libre e independientes de las pertenencias. Se guardan sin prefijos en `check_registro.observaciones`. No se registra ni se declara devolución o pendiente de pertenencias. Las observaciones guardadas por salidas anteriores no se modifican.
- Las observaciones de salida no sustituyen incidencias. No hay reentradas, anulación/corrección de salidas, alta rápida de autorizados ni cambios de reserva. Información de cuidados pendiente del MVP y cancelación de reservas siguen fuera de alcance.

### Comprobación manual de salida
1. Carga `scripts/dev/seed-reception-demo.sql` solo en la base HSQLDB local y entra como `recepcionista / admin1234`. El fixture deja a Bruno / Ana Núñez en el centro con la entrada y las pertenencias declaradas `Arnés rojo; bolsa azul; recipiente de comida`, y añade a `María Autorizada Demo` como persona autorizada de Ana. No se reinician datos ni se borran salidas para repetir el ejercicio.
2. Desde el detalle de Bruno, registra una salida como tutor o selecciona a María como persona autorizada; confirma la salida y las observaciones opcionales. El tablero y detalle deben mostrar salida, responsable y destinatario. Para probar una nueva entrega, utiliza otra reserva abierta válida; no reutilices una reserva ya completada.
3. La reserva Kira / Belén o Luna / Ana puede servir para probar primero el flujo normal de llegada, y después la salida como tutor, siempre que siga disponible hoy. Una reserva futura, una reserva sin entrada o una entrada ya cerrada debe rechazarse. Una estancia antigua solo puede probarse si existe una entrada válida de fecha anterior; se cierra con la hora actual.
4. Verifica los datos sin modificar la base: `SELECT * FROM check_registro WHERE reserva_id = <id> ORDER BY fecha_hora;` y `SELECT estado, fecha, modalidad, grupo_id, bono_id FROM reserva WHERE id = <id>;`. No ejecutes `DELETE` ni `TRUNCATE`. El script no crea incidencias ni salidas de prueba para poder revertir.

| Comprobación | Resultado esperado |
|---|---|
| Tutor recoge; entrada válida | Una sola salida con hora y usuario del servidor, referencia al tutor y nombre histórico. |
| Persona autorizada recoge | Solo se acepta si se mantiene vinculada al tutor de la reserva; persona de otro tutor rechazada. |
| Observaciones | Campo opcional de texto libre; se guarda literalmente y no representa el estado de pertenencias. |
| Sin entrada, salida previa o reserva ajena | Rechazo controlado; sin mutaciones parciales. |
| Dos solicitudes concurrentes o reintento | Como máximo una salida; el segundo intento recibe conflicto y no sustituye al destinatario. |
| Presencia y aforo | Presencia “Salida registrada”; fecha, modalidad, grupo y bono no cambian; no se libera plaza por salida anticipada. |
| Roles y ámbito | Recepción autorizada del centro puede operar; sin sesión `401`, rol no autorizado `403`, reserva ajena `404`. |

Las pruebas automatizadas independientes no dependen de estos fixtures manuales. La concurrencia se cubre en pruebas con peticiones concurrentes y base HSQLDB de pruebas; no se debe probar simultaneidad en la base local persistente.

## Personas autorizadas en la ficha del perro
- La ficha de recepción (`GET /api/recepcion/perros/{id}`) muestra los nombres de las personas autorizadas asociadas al tutor del perro. No añade datos de contacto ni permite editarlas.
- Las autorizaciones dependen del tutor y pueden aplicar a varios perros de su titularidad. El listado se obtiene solo en la consulta individual de la ficha; los resultados paginados no cargan autorizaciones.
- Si el perro no tiene tutor o no hay personas autorizadas registradas, la ficha indica que no hay personas disponibles. El endpoint conserva la autorización de Recepcionista, Admin y Gerente y el aislamiento por centro.

## Gestión de tutores para recepción
- Plan aplicado (aprobado el 18/11/2026): conservar la tabla `tutor`, separar las fichas de tutor de las cuentas de `USUARIOS`, permitir consulta/alta/edición solo dentro del centro del usuario autenticado y no modificar el esquema.
- Recepcionista, Admin y Gerente pueden buscar, crear, consultar y editar tutores de su centro. No se crean cuentas, no se vinculan usuarios y no se cambian perros, bonos ni reservas.
- Nombre, apellidos, DNI, teléfono y email son obligatorios. Se recortan espacios exteriores; el DNI se guarda en mayúsculas y el email en minúsculas. El teléfono se trata como texto y no se limita a un país ni a un formato numérico concreto.
- El DNI no se puede repetir dentro del mismo centro. No hay unicidad para teléfono o email: si se encuentra otro tutor con el mismo teléfono (ignorando espacios exteriores) o email (sin distinguir mayúsculas), la aplicación pregunta si se desea continuar. La confirmación no fusiona ni altera otras fichas.
- La actualización usa `If-Match`/`ETag` calculado a partir de los datos de la ficha. Si desde que se abrió otra persona guardó cambios, se avisa y no se sobreescribe esa versión. Es una detección sencilla, no un bloqueo transaccional: dos guardados exactamente simultáneos podrían superar la comprobación; no se añadió una columna de versión.
- El API se encuentra bajo `/api/recepcion/tutores`: listado/búsqueda paginada, alta, ficha y edición. El centro no se recibe desde el navegador. Los errores para fichas ajenas e inexistentes no revelan información del otro centro.

### Datos de demostración y comprobación manual
1. En la base local `p`, ejecuta manualmente `scripts/dev/seed-tutors-demo.sql`. No se carga al arrancar, no borra datos y puede repetirse sin duplicar tutores ni el perro de prueba. No lo ejecutes en producción ni en una base compartida.
2. Inicia sesión con `recepcionista` / `admin1234` y abre **Tutores** desde el tablero. Los datos se localizan por DNI, no por identificadores numéricos.
3. Prueba buscar `Alex Martín`: deben aparecer dos fichas diferentes. `DEMO-TUTOR-COMPARTIDO` comparte contacto con `DEMO-TUTOR-ANA` y debe mostrar una confirmación antes de guardar coincidencias nuevas.
4. `DEMO-TUTOR-PERRO` ya tiene un perro asociado; editar sus datos debe conservar esa relación. `DEMO-TUTOR-EDITAR` es la ficha manual de prueba: valores iniciales `Elena Prueba de edición`, `+1 202 555 0107`, `elena.inicial@example.com`; se puede cambiar, por ejemplo, el teléfono a `+1 202 555 0198` y el email a `elena.actualizada@example.com`.
5. Para comprobar un alta que no forma parte del fixture, puedes usar `Noa`, `Prueba Interfaz`, `DEMO-ALTA-NOA-2026-11-18`, `+1 202 555 0199`, `noa.prueba.20261118@example.com`. Si repites la prueba, comprueba primero que ese DNI siga libre en el centro.
6. La cuenta `tutor` no puede entrar a estos endpoints. Las fichas de centros ajenos responden como no encontradas. La tabla de HSQLDB y los tests automatizados son independientes de las modificaciones manuales.

| Ficha de prueba | Datos principales | Comprobación esperada |
|---|---|---|
| `DEMO-TUTOR-ANA` | Ana García López; email y teléfono | Búsqueda, ficha y coincidencia de contacto. |
| `DEMO-TUTOR-INAKI` | Iñaki Muñoz; contacto ficticio | Búsqueda de caracteres con tilde; email obligatorio. |
| `DEMO-TUTOR-MARIA` | María del Mar O'Neill | Nombre compuesto y apóstrofo. |
| `DEMO-TUTOR-ALEX-1` y `DEMO-TUTOR-ALEX-2` | Alex Martín; teléfonos distintos | Dos personas con el mismo nombre no se confunden. |
| `DEMO-TUTOR-COMPARTIDO` | Comparte email y teléfono con Ana | Confirmación explícita de coincidencia. |
| `DEMO-TUTOR-PERRO` | Tutor con perro ya relacionado | La edición no altera la relación existente. |
| `DEMO-TUTOR-EDITAR` | Elena Prueba de edición | Cambiar contacto, guardar y recargar conserva los cambios. |

El email es obligatorio según el esquema actual y la decisión aprobada; por eso Iñaki también tiene un email ficticio `example.com`. El fixture no crea cuentas en `USUARIOS`. La edición puede comprobar cambios simultáneos abriendo la misma ficha en dos sesiones: guardar desde la primera y luego intentar guardar desde la segunda debe mostrar un aviso de versión desactualizada.

1. Arranca backend en modo `v`.
2. Arranca frontend (`npm start`).
3. Entra en `http://localhost:4200` con `admin / admin1234`.
4. Revisa Swagger en `http://localhost:8080/swagger-ui/index.html`.


En la raíz del proyecto:

```bash
mvn compile
mvn test-compile
```


El proyecto incluye reglas específicas en:

```text
.github/copilot-instructions.md
```

Ahí se define, entre otras cosas:
- estructura esperada de módulos
- convenciones de código/documentación
- cómo mantener los ficheros de openapi
- reglas de ejecución en terminal para el equipo

## Gestión básica de perros para recepción
- Plan aplicado (decisiones aprobadas por el usuario): reutilizar el modelo existente, asociar el perro al centro del usuario autenticado y exigir un tutor del mismo centro solo en el flujo interno. No se modifican migraciones ni tablas de la aplicación; los scripts locales crean únicamente la tabla auxiliar `dev_fixture_registry`.
- Recepcionista, Admin y Gerente pueden buscar, registrar, consultar y editar perros de su centro. Tutor y usuarios B2B no acceden a esta API.
- El nombre es obligatorio; raza y fecha de nacimiento son opcionales; tamaño es obligatorio según el esquema y admite `PEQUENO`, `MEDIANO` o `GRANDE`. No existe una columna de peso.
- El formulario no modifica centro, tutor, estado, fecha de creación, temperamento, pautas de alimentación ni foto. En edición, tutor se muestra como solo lectura y las reservas y demás relaciones se conservan.
- La búsqueda admite nombre del perro y nombre/apellidos del tutor; está paginada y ordenada por nombre e identificador. El selector de tutor reutiliza la búsqueda ya existente de tutores.
- Si otro perro del mismo tutor y centro tiene el mismo nombre, se solicita confirmación. No hay unicidad ni fusión automática. La confirmación no evita dobles envíos simultáneos.
- La edición utiliza `If-Match`/`ETag` para avisar si la ficha cambió desde que se abrió. Es detección sencilla, no bloqueo transaccional; dos escrituras exactamente simultáneas aún podrían competir. La raza y fecha omitidas en un PUT se conservan; para vaciarlas se envía `clearBreed` o `clearBirthDate`.
- El API está bajo `/api/recepcion/perros`: listado, alta, detalle y edición. El centro no se recibe desde el navegador; el tutor elegido se valida en backend contra el mismo centro. No se acepta modificar tutor o centro en la edición.
- El cambio de tutor queda pendiente para una entrega futura. La siguiente entrega de cuidados (alergias, pautas de medicación, observaciones relevantes y consulta de contactos/autorizaciones) también queda pendiente; registrar una pauta no significa administrar una dosis.
- La entrega 2B de peluquería, incluidos los flujos B2B, queda fuera del MVP y no se implementa en esta entrega.

### Datos y recorrido manual
1. En desarrollo local, ejecuta `scripts/dev/seed-reception-demo.sql` y `scripts/dev/seed-tutors-demo.sql` si sus datos aún no están cargados. No se ejecutan al arrancar, no deben usarse en producción y no borran datos. Usa el perfil local `p` descrito al principio de esta guía.
2. Inicia sesión como `recepcionista` / `admin1234` y abre **Perros** desde el tablero. Los fixtures existentes incluyen dos perros llamados Luna de `DEMO-REC-01` (Ana Núñez), otra Luna de `DEMO-REC-04` (Inés Peña), Bruno y un perro de `DEMO-TUTOR-PERRO`.
3. Para comprobar alta con nombre acentuado, busca primero `Bóreas Prueba 20261118`; si no existe, registra ese nombre con el tutor `DEMO-TUTOR-INAKI`, sin raza ni fecha de nacimiento, y tamaño `MEDIANO`. Al repetir, abre la ficha existente en lugar de crear otro registro.
4. Para probar la advertencia de coincidencia, intenta registrar un tercer perro llamado `Luna` para `DEMO-REC-01`, revisa el aviso y confirma solo si se trata de un perro distinto. En el listado, distingue los dos perros existentes por raza y tamaño. No se fusionan fichas.
5. Para comprobar edición y persistencia, abre `Perro de prueba de tutor` de `DEMO-TUTOR-PERRO`, cambia raza o fecha, guarda y recarga. Para comprobar conservación de una reserva, edita `Bruno` de `DEMO-REC-01` en la base local de desarrollo y verifica que su reserva sigue asociada; no uses datos reales.
6. La edición simultánea se puede comprobar en dos pestañas: abre la misma ficha en ambas, guarda un cambio en la primera e intenta guardar desde la segunda. Debe avisar que la ficha está desactualizada y ofrecer recargarla.
7. El enlace **Abrir alta de tutor** se abre en otra pestaña para no perder el formulario de perro; al volver, busca el nuevo tutor en el selector.

Los scripts usan `dev_fixture_registry`, una tabla auxiliar exclusiva de las bases locales de desarrollo; no forma parte del modelo de aplicación ni de las migraciones. Sus claves relacionan fixtures con sus IDs generados, por lo que cambiar el nombre de un perro después de registrarlo no provoca que se vuelva a crear ni que se restablezcan ediciones. En una base que ya tenía estos datos, la primera ejecución asocia los perros con su nombre inicial; si ya se habían renombrado antes de esa primera ejecución, revisa y resuelve esas filas manualmente antes de volver a sembrar. Los tests de integración crean sus propios tutores, perros y reserva dentro de la base de pruebas; no dependen de estos fixtures. Las pruebas manuales anteriores son pasos reproducibles, no ejecuciones realizadas durante la implementación.

## Alta de reservas y aforo para recepción
- Plan aplicado (decisiones aprobadas por el usuario): reserva individual por perro, fecha y grupo elegido expresamente; capacidad `grupo.capacidad_max` independiente para mañana y tarde; fechas de hoy o futuras de lunes a sábado en `Europe/Madrid`; alta directamente confirmada. No se asigna grupo automáticamente ni se consume bono.
- Las modalidades nuevas son `DIA_COMPLETO`, `MANANA` y `TARDE`. La modalidad histórica `MEDIA_JORNADA`, al no guardar franja, consume mañana y tarde. Consumen aforo todos los estados salvo `CANCELADA`; no hay caducidad automática de pendientes. La capacidad no se agrega entre grupos.
- Solo Recepcionista, Admin y Gerente pueden buscar perros elegibles, consultar grupos/disponibilidad y crear reservas dentro de su centro. Se exige perro activo con tutor del mismo centro. Centro, tutor, estado, bono y grupo no se aceptan como datos libres del cliente (el grupo sí se elige entre los grupos autorizados). El API es `GET /api/recepcion/grupos`, `GET /api/recepcion/disponibilidad?date=YYYY-MM-DD&groupId=...` y `POST /api/recepcion/reservas`; la alta devuelve `201` y la ruta del detalle en `Location`.
- Un alta consume una plaza por franja elegida. Día completo exige capacidad simultánea en ambas. La interfaz muestra disponibilidad informativa; al crear, el backend bloquea primero la fila del perro y después la del grupo, comprueba duplicado/aforo y persiste dentro de la misma transacción. El bloqueo del grupo serializa altas que compiten por capacidad, incluidas fechas sin reservas previas. En las limitaciones de HSQLDB/PostgreSQL descritas por la aplicación, la garantía depende del bloqueo pesimista de esa fila de grupo y no coordina operaciones que escriban reservas fuera de la aplicación.
- Se permite como máximo una reserva no cancelada por perro y fecha. No se permiten dos medias jornadas separadas para el mismo perro/día. Tras cancelar se puede reservar de nuevo; el histórico cancelado se conserva. No hay clave idempotente: un reintento de un alta ya persistida se informa como duplicado (`409 DUPLICATE_RESERVATION`), no repite la operación. El aforo completo devuelve `409 CAPACITY_FULL`, la ausencia de grupos `409 CAPACITY_NOT_CONFIGURED` y una fecha pasada o domingo `422 DATE_NOT_ALLOWED`.
- Cancelar reservas queda expresamente para la siguiente entrega. Modificar reservas sigue pendiente de decisión de alcance. Esta entrega no añade interfaz de cancelación/modificación.

### Fixture local de capacidad y escenarios manuales
1. En HSQLDB local con perfil `p`, arranca la base y el backend como se indica arriba. Ejecuta primero `scripts/dev/seed-reception-demo.sql` si aún no existen el centro y tutor de demostración; a continuación ejecuta manualmente `scripts/dev/seed-reservation-capacity-demo.sql`. No se carga automáticamente, no borra filas y no se debe ejecutar en producción ni en una base compartida.
2. El segundo script crea un grupo propio de fixture con capacidad 3, perros de prueba asociados por claves de `dev_fixture_registry` y fechas operativas F1–F9. Al final imprime la fecha real de cada escenario y la capacidad del grupo; comprueba que la capacidad es 3 antes de interpretar los resultados. Las fechas se fijan en la primera ejecución y el script no las actualiza ni restablece cambios manuales. Usa el nombre del grupo **Reserva Demo Aforo** y los perros indicados en la tabla. El tutor de fixture es `DEMO-REC-01`.
3. Entra como `recepcionista` / `admin1234`. Abre **Nueva reserva** desde el tablero, elige expresamente perro y grupo, y consulta disponibilidad antes de confirmar. Las observaciones de plazas son previas; el alta vuelve a validar capacidad en backend.
4. Comprueba primero las filas de forma solo lectura y los rechazos; después ejecuta una sola vez las altas permitidas y la competencia F9. Las altas modifican deliberadamente la ocupación. El script es repetible sin duplicar filas ni borrar datos, pero no reinicia escenarios alterados; no borres reservas ni cambies las fechas para repetir. Para otra ejecución destructiva o un reinicio hace falta una decisión explícita.
5. F10 (día cerrado) no aplica: no existe calendario de cierres ni de días no operativos en el esquema; solo se aprobó que el centro no opera en domingo.

| Escenario | Fecha real | Capacidad | Reservas precargadas | Plazas esperadas (mañana/tarde) | Perro / acción | Resultado esperado |
|---|---|---:|---|---:|---|---|
| F1 — Centro vacío | Consultar salida del script | 3 | Ninguna | 3 / 3 | `F1-Perro`; alta de día completo (una vez) | Debe crearse una reserva confirmada; después quedan 2 / 2. |
| F2 — Mañana completa | Consultar salida del script | 3 | 3 mañana | 0 / 3 | Rechazo de mañana y día completo; luego otro perro para tarde | `CAPACITY_FULL` para mañana/día completo; tarde consume solo tarde. |
| F3 — Tarde completa | Consultar salida del script | 3 | 3 tarde | 3 / 0 | Rechazo de tarde y día completo; otro perro para mañana | `CAPACITY_FULL` para tarde/día completo; mañana consume solo mañana. |
| F4 — Día completo | Consultar salida del script | 3 | 3 día completo | 0 / 0 | Intentar cualquier modalidad con un perro sin reserva ese día | Se rechazan todas por aforo. |
| F5 — Ocupación mixta | Consultar salida del script | 3 | 1 día completo, 1 mañana, 1 tarde | 1 / 1 | Intentar día completo con un perro sin reserva | Debe caber en ambas y dejarlas a 0 / 0. |
| F6 — Canceladas | Consultar salida del script | 3 | 1 activa de día completo, 2 canceladas | 2 / 2 | Consultar disponibilidad; no reservar con perros precargados | Canceladas no cuentan; la activa sí ocupa ambas franjas. |
| F7 — Duplicado | Consultar salida del script | 3 | 1 mañana activa | 2 / 3 | `F7-Duplicado`, reservar con otra modalidad | `409 DUPLICATE_RESERVATION`, aunque quede aforo. |
| F8 — Re-reserva | Consultar salida del script | 3 | 1 tarde cancelada | 3 / 3 | `F8-ReReserva`, reservar una vez | Alta nueva confirmada; la cancelada permanece en histórico. |
| F9 — Última plaza/concurrencia | Consultar salida del script | 3 | 2 mañana activas | 1 / 3 | Competir con `F9-Competidor-1` y `F9-Competidor-2` | Exactamente una alta; la otra recibe conflicto y ocupación final 3 / 3 para mañana. |

## Entrega 7: consulta de Mis perros para tutor

- **Plan aplicado (aprobado por el usuario):** se reutilizan las tablas existentes `USUARIOS`, `tutor`, `perro`, `alergia` y `medicacion`; la relación es explícita mediante `USUARIOS.tutor_id`, nullable y única. La migración V10 no asocia usuarios existentes ni busca coincidencias por nombre, email o DNI. La asociación de cuentas reales debe verificarse y establecerse explícitamente por el personal autorizado.
- El login consulta `GET /api/users/me` para obtener identidad y rol; ya no consulta el listado de usuarios. `GET /api/users` y `/api/users/{id}` quedan restringidos a Admin. El rol Tutor entra directamente en **Mis perros**; las rutas de recepción conservan su pantalla y están limitadas a Recepcionista, Admin y Gerente.
- El API de lectura es `GET /api/tutor/mis-perros?page=0&size=50` y `GET /api/tutor/mis-perros/{id}`. El tutor y el centro se resuelven desde la cuenta autenticada; listado, ficha y foto filtran por tutor y centro. Un identificador ajeno responde como no encontrado. El tamaño de página máximo es 100.
- El listado devuelve solo identificador, nombre, raza, tamaño y disponibilidad de foto. La ficha devuelve nombre, raza, tamaño, nacimiento y alimentación de solo lectura, además de alergias, medicación y autorizaciones aplicables. El temperamento no se expone al tutor porque es información interna de organización de grupos. No hay peso ni campos estructurados de frecuencia o vigencia; no se indican tratamientos activos/finalizados ni dosis administradas. Las listas vacías se presentan como ausencia de registros, no como afirmaciones clínicas.
- **Política de campos compartidos:** `perro.temperamento` y `perro.pautas_alimentacion` se consideran información destinada al tutor. No se deben guardar ahí notas privadas, incompatibilidades ni evaluaciones internas. El DTO del portal no incluye incompatibilidades, notas, incidencias, centro, tutor ni datos de usuarios.
- `GET /api/tutor/mis-perros/{id}/foto` autoriza de nuevo el acceso al perro y solo sirve nombres de archivo de imagen desde almacenamiento privado fuera de recursos públicos. La ubicación predeterminada es `${user.home}/.cd2026mnd1g1/dog-photos` y se puede sobrescribir mediante `DOG_PHOTOS_DIR`. No acepta ni descarga URLs remotas; no devuelve rutas internas. Si falta el archivo o falla su lectura, la interfaz muestra un avatar neutro.
- Si una cuenta Tutor no tiene vínculo, se responde con `409 TUTOR_ACCOUNT_NOT_LINKED` y la interfaz muestra una incidencia de configuración diferente de la lista vacía. Una incoherencia cuenta/tutor/centro se rechaza y registra técnicamente sin seleccionar otra ficha.

### Fixture local y recorrido manual

1. En HSQLDB local (perfil `p`), ejecuta primero `scripts/dev/seed-reception-demo.sql` si aún no existen centro y cuentas base; después ejecuta manualmente `scripts/dev/seed-tutor-portal-demo.sql`. El script es idempotente por claves estables de fixture, no borra filas y no se ejecuta al arrancar. No usar en producción ni en una base compartida.
2. El script crea `portal-a`, `portal-b`, `portal-c` y `portal-d` con rol Tutor. Para no tocar contraseñas existentes, cada cuenta nueva reutiliza el hash BCrypt ya sembrado del usuario `tutor`; para la prueba local, la contraseña es `admin1234`. A y B tienen ficha asociada; C está asociado sin perros; D no tiene ficha asociada. Las fichas se enlazan mediante DNI estable del fixture, nunca por email/nombre.
3. Entra con `portal-a / admin1234`: deben aparecer **Luna** y **Milo**; Luna tiene foto, datos compartidos, alergia y una pauta de medicación. Milo no tiene registros opcionales. Entra con `portal-b / admin1234`: solo aparece su propia Luna. `portal-c / admin1234` muestra exactamente “Todavía no tienes perros registrados”. `portal-d / admin1234` muestra el mensaje de configuración, no la lista vacía.
4. La incompatibilidad ficticia entre los perros de A es interna: no debe aparecer en listado, ficha ni JSON. Abre Luna, comprueba los datos permitidos y vuelve al listado. Recarga directamente `/mis-perros/{id}` y prueba que el perro B no se puede consultar desde A.
5. Para servir la foto de fixture, configura `DOG_PHOTOS_DIR` apuntando a `scripts/dev/private-dog-photos` antes de iniciar el backend. El registro apunta a `portal-dog-a1.png`; Milo no tiene foto. Sin esa variable, la subida y consulta usan el almacenamiento privado predeterminado en el directorio de usuario.
6. Para vincular una cuenta real existente, primero verifica la identidad de la cuenta y la ficha de tutor con el procedimiento administrativo del centro; después asigna explícitamente su `tutor_id` en `USUARIOS`. La migración y el portal no realizan ese backfill. No vincules por coincidencia de email, nombre ni DNI.

### Cuentas de tutor para datos locales existentes

- Ejecuta manualmente `scripts/dev/seed-tutor-user-accounts.sql` en la BD HSQLDB local (perfil `p`). Crea una cuenta propia por ficha de tutor que aún no tenga vínculo: el usuario y el email de acceso son el email de la ficha; el centro, nombre, apellidos y DNI se copian de esa ficha. Las cuentas nuevas tienen rol Tutor y se vinculan por `tutor_id`, no por una búsqueda de coincidencias.
- Para esta BD local, las cuentas nuevas reutilizan el hash BCrypt de `tutor`; la contraseña inicial es `admin1234`. No se cambian ni se eliminan las cuentas demo `admin`, `recepcionista` y `tutor`. El usuario demo `tutor` permanece separado de las cuentas individuales.
- Si una ficha no tiene perros, el script no reasigna perros existentes. Para los datos actuales crea únicamente `Perro demo de Vanessa` (tamaño mediano, sin datos clínicos), ya que Vanessa es la única ficha sin perro. El registro auxiliar mantiene la carga repetible.
- El script solo está destinado a desarrollo local; no es una migración ni debe ejecutarse en producción. Su consulta final permite comprobar que cada tutor tiene cuenta y al menos un perro, y que las cuentas demo conservan sus roles.

### Comprobaciones de la entrega

- Prueba de integración añadida en `cd2026mnd1g1-boot/src/test/java/com/campusdual/cd2026mnd1g1/TutorPortalControllerITTest.java`: autenticación/rol, identidad, aislamiento por tutor, DTO sin campos internos/peso/URL, lista vacía frente a cuenta sin vínculo, acceso a perro ajeno y consulta de foto ajena. Sus datos se crean dentro de la base de pruebas y no dependen de fixtures manuales.
- Comprobaciones ejecutadas: `mvn test-compile` y `mvn -pl cd2026mnd1g1-boot -am test` desde la raíz finalizaron correctamente; la suite backend ejecutó 43 pruebas, con 0 fallos y 0 errores. También pasó `mvn -pl cd2026mnd1g1-boot -am '-Dtest=ReceptionReservationControllerITTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` (18 pruebas) y `npm run build` desde `cd2026mnd1g1-frontend`. Una ejecución previa de la suite completa falló de forma aislada en la prueba concurrente de salidas; tanto la clase focalizada como una repetición completa posterior pasaron. El build Angular terminó correctamente con advertencias existentes de presupuesto SCSS.
- No existe automatización E2E de navegador para login, navegación, error/fallback de imagen ni cierre de sesión entre cuentas; esos recorridos quedan como pasos manuales reproducibles.
