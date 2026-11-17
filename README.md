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
2. Desde DBeaver, conéctate al HSQLDB local y ejecuta `scripts/dev/seed-reception-demo.sql` una vez. Está preparado para repetirse en la misma fecha sin duplicar tutores, perros, reservas ni registros de entrada/salida.
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
