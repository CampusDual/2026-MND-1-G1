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
- El tablero no permite crear, editar ni cancelar reservas, ni registrar entradas o salidas. Al recargar una ruta protegida habrá que iniciar sesión de nuevo; después se recupera la ruta y la fecha solicitadas.
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
