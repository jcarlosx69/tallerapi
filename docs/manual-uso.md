# Manual de uso — TallerAPI

> Guía de referencia rápida para usar la API de gestión de inventario y proyectos del taller de carpintería. No contiene detalles de código ni de arquitectura interna: solo qué hace cada función y cómo usarla.

---

## 1. Qué es TallerAPI

TallerAPI permite gestionar, vía peticiones HTTP, tres cosas:

- **Clientes** del taller.
- **Materiales** disponibles en el inventario (madera, herrajes, barnices...) y su stock.
- **Proyectos** (encargos de carpintería), incluyendo qué materiales se les asigna y en qué cantidad, con control automático de stock.

Toda la API cuelga del prefijo `/api/v1`. Puedes usarla de tres formas:

- **Swagger UI** (recomendado si no quieres usar la terminal): `http://localhost:8080/swagger-ui.html` en local.
- **Postman** o cualquier cliente REST gráfico.
- **curl** desde terminal (los ejemplos de este manual usan curl, pero son igualmente válidos pegando los mismos datos en Swagger).

---

## 2. Antes de empezar: autenticarse

Casi todo en la API requiere estar autenticado. Hay dos roles:

- **ADMIN**: puede crear, modificar y borrar (Clientes, Materiales, Proyectos, asignaciones).
- **USER**: solo puede consultar (listar y ver detalle). Los datos de ejemplo solo incluyen el usuario ADMIN.

### Paso 1 — Iniciar sesión

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}'
```

La respuesta incluye un **token**:

```json
{
  "token": "eyJhbGciOiJI...",
  "expiresAt": "2026-06-18T17:16:45Z"
}
```

### Paso 2 — Usar el token en cada petición siguiente

Cualquier petición a partir de ahora necesita el token en la cabecera `Authorization`:

```
Authorization: Bearer eyJhbGciOiJI...
```

El token caduca en el plazo indicado en `expiresAt` (por defecto, 1 hora). Pasado ese tiempo, hay que volver a hacer login para obtener uno nuevo.

> **En Swagger UI**: pulsa el botón "Authorize" (arriba a la derecha) y pega el token (sin escribir "Bearer", Swagger lo añade solo).

---

## 3. Gestión de Clientes

Un Cliente representa a la persona o empresa para la que se hace un encargo.

| Acción | Método | Endpoint | Quién puede |
|---|---|---|---|
| Listar clientes (paginado) | GET | `/api/v1/clientes` | ADMIN o USER |
| Ver un cliente concreto | GET | `/api/v1/clientes/{id}` | ADMIN o USER |
| Crear un cliente | POST | `/api/v1/clientes` | ADMIN |
| Modificar un cliente | PUT | `/api/v1/clientes/{id}` | ADMIN |
| Eliminar un cliente | DELETE | `/api/v1/clientes/{id}` | ADMIN |

> **Ordenación:** en los listados puedes añadir `?sort=campo,asc|desc`, p. ej. `?sort=nombre,desc`. Si el campo no existe, la API responde 400.

**Datos de un cliente:** nombre (obligatorio), email (opcional, debe tener formato válido), teléfono (opcional).

**Restricción importante:** no se puede eliminar un cliente que tenga proyectos asociados. Si lo intentas, la API responde con un error indicando que el cliente está en uso.

### Ejemplo: crear un cliente

```bash
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Authorization: Bearer <TU_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Carpintería Ribera","email":"contacto@ribera.es","telefono":"600111222"}'
```

La respuesta incluye el `id` que se le ha asignado — lo necesitarás para crear proyectos a su nombre.

---

## 4. Gestión de Materiales (inventario)

Un Material es cualquier elemento del inventario del taller: madera, herrajes, barniz, etc.

| Acción | Método | Endpoint | Quién puede |
|---|---|---|---|
| Listar materiales (paginado, con filtro opcional por tipo) | GET | `/api/v1/materiales` | ADMIN o USER |
| Ver un material concreto | GET | `/api/v1/materiales/{id}` | ADMIN o USER |
| Crear un material | POST | `/api/v1/materiales` | ADMIN |
| Modificar un material (incluido el stock) | PUT | `/api/v1/materiales/{id}` | ADMIN |
| Eliminar un material | DELETE | `/api/v1/materiales/{id}` | ADMIN |

**Datos de un material:**
- `nombre`: obligatorio.
- `tipo`: uno de `MADERA`, `HERRAJE`, `BARNIZ`, `OTRO`.
- `unidad`: uno de `UNIDAD`, `METRO`, `KG`, `LITRO`.
- `stockDisponible`: cantidad actual en inventario (no puede ser negativa).
- `costeUnitario`: precio por unidad (no puede ser negativo).

**Restricción importante:** no se puede eliminar un material que esté asignado a algún proyecto activo.

### Ejemplo: crear un material

```bash
curl -X POST http://localhost:8080/api/v1/materiales \
  -H "Authorization: Bearer <TU_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Tablero de roble","tipo":"MADERA","unidad":"METRO","stockDisponible":50,"costeUnitario":18.90}'
```

### Ejemplo: filtrar materiales por tipo

```bash
curl "http://localhost:8080/api/v1/materiales?tipo=MADERA&page=0&size=10" \
  -H "Authorization: Bearer <TU_TOKEN>"
```

---

## 5. Gestión de Proyectos

Un Proyecto es un encargo de carpintería: tiene un cliente, fechas, un estado, y opcionalmente un presupuesto y materiales asignados.

| Acción | Método | Endpoint | Quién puede |
|---|---|---|---|
| Listar proyectos (paginado, filtros por estado y/o fechas) | GET | `/api/v1/proyectos` | ADMIN o USER |
| Ver el detalle de un proyecto (incluye materiales asignados) | GET | `/api/v1/proyectos/{id}` | ADMIN o USER |
| Crear un proyecto | POST | `/api/v1/proyectos` | ADMIN |
| Modificar un proyecto (incluido el cambio de estado) | PUT | `/api/v1/proyectos/{id}` | ADMIN |
| Eliminar un proyecto | DELETE | `/api/v1/proyectos/{id}` | ADMIN |

**Datos de un proyecto:**
- `nombre`: obligatorio.
- `clienteId`: id de un cliente ya existente.
- `fechaInicio`: obligatoria.
- `fechaEntregaPrevista`: opcional; si se indica, debe ser igual o posterior a `fechaInicio`.
- `estado`: obligatorio en la petición, pero al crear se ignora: todo proyecto nace en `EN_CURSO`. Se cambia después con un `PUT` (ver transiciones abajo).
- `presupuesto`: opcional.

### Estados de un proyecto y sus transiciones permitidas

```
EN_CURSO  ──▶  TERMINADO  ──▶  ENTREGADO
   │               │
   └──────▶  CANCELADO  ◀──────┘
```

- Un proyecto **EN_CURSO** puede pasar a `TERMINADO` o a `CANCELADO`.
- Un proyecto **TERMINADO** puede pasar a `ENTREGADO` o a `CANCELADO`.
- Un proyecto **ENTREGADO** o **CANCELADO** ya no puede cambiar de estado (son estados finales).
- No se puede "retroceder" en el flujo (por ejemplo, de `TERMINADO` a `EN_CURSO`). Si lo intentas, la API lo rechaza.

### Filtrar el listado de proyectos

Puedes combinar estos filtros en la URL:
- `?estado=EN_CURSO` (o `TERMINADO`, `ENTREGADO`, `CANCELADO`)
- `?fechaDesde=2026-01-01&fechaHasta=2026-12-31` (ambas fechas a la vez)

```bash
curl "http://localhost:8080/api/v1/proyectos?estado=EN_CURSO" \
  -H "Authorization: Bearer <TU_TOKEN>"
```

---

## 6. Asignar materiales a un proyecto (con control de stock)

Esta es la funcionalidad central de TallerAPI: cuando un proyecto necesita madera, herrajes, etc., se "asignan" desde el inventario, y el stock se descuenta automáticamente.

| Acción | Método | Endpoint | Quién puede |
|---|---|---|---|
| Asignar un material a un proyecto | POST | `/api/v1/proyectos/{id}/materiales` | ADMIN |
| Ver los materiales asignados a un proyecto | GET | `/api/v1/proyectos/{id}/materiales` | ADMIN o USER |
| Retirar una asignación (repone el stock) | DELETE | `/api/v1/proyectos/{id}/materiales/{materialId}` | ADMIN |

### Cómo funciona la validación de stock

Al asignar un material, indicas `materialId` y `cantidadAsignada`:

```bash
curl -X POST http://localhost:8080/api/v1/proyectos/1/materiales \
  -H "Authorization: Bearer <TU_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"materialId":2,"cantidadAsignada":5}'
```

- Si hay stock suficiente: la asignación se crea, y el stock del material se reduce en esa cantidad.
- Si **no** hay stock suficiente: la API rechaza la petición y no se modifica nada (ni se crea la asignación ni se toca el stock).

### Retirar una asignación

```bash
curl -X DELETE http://localhost:8080/api/v1/proyectos/1/materiales/2 \
  -H "Authorization: Bearer <TU_TOKEN>"
```

Al retirarla, la cantidad asignada se **devuelve** automáticamente al stock disponible del material.

> Nota: un mismo material puede asignarse varias veces a un mismo proyecto (por ejemplo, si necesitas añadir más cantidad más adelante); cada asignación queda registrada por separado, con su propia fecha. Si hay varias asignaciones del mismo material, cada `DELETE` retira una; repítelo para retirar las demás.

---

## 7. Tutorial completo: de cero a un proyecto con materiales

Sigue estos pasos en orden para ver el ciclo de vida completo de un encargo.

### Paso 1 — Iniciar sesión

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
```

(O simplemente copia el token de la respuesta a mano y pégalo donde se indique `<TU_TOKEN>` en los siguientes pasos.)

### Paso 2 — Crear un cliente

```bash
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Carpintería Ribera","email":"contacto@ribera.es","telefono":"600111222"}'
```

Anota el `id` devuelto (supongamos que es `1`).

### Paso 3 — Crear dos materiales

```bash
curl -X POST http://localhost:8080/api/v1/materiales \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Tablero de roble","tipo":"MADERA","unidad":"METRO","stockDisponible":50,"costeUnitario":18.90}'

curl -X POST http://localhost:8080/api/v1/materiales \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Bisagras de latón","tipo":"HERRAJE","unidad":"UNIDAD","stockDisponible":20,"costeUnitario":2.50}'
```

Anota los dos `id` devueltos (supongamos `1` y `2`).

### Paso 4 — Crear el proyecto para ese cliente

```bash
curl -X POST http://localhost:8080/api/v1/proyectos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Mesa de comedor a medida","clienteId":1,"fechaInicio":"2026-06-01","fechaEntregaPrevista":"2026-07-01","presupuesto":850.00, "estado":"EN_CURSO"}'
```

El proyecto se crea automáticamente en estado `EN_CURSO`. Anota su `id` (supongamos `1`).

### Paso 5 — Asignar los materiales necesarios

```bash
curl -X POST http://localhost:8080/api/v1/proyectos/1/materiales \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"materialId":1,"cantidadAsignada":5}'

curl -X POST http://localhost:8080/api/v1/proyectos/1/materiales \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"materialId":2,"cantidadAsignada":8}'
```

### Paso 6 — Consultar el proyecto con sus materiales

```bash
curl http://localhost:8080/api/v1/proyectos/1 -H "Authorization: Bearer $TOKEN"
```

Verás el proyecto junto con la lista de materiales asignados.

### Paso 7 — Marcar el proyecto como terminado

```bash
curl -X PUT http://localhost:8080/api/v1/proyectos/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Mesa de comedor a medida","clienteId":1,"fechaInicio":"2026-06-01","fechaEntregaPrevista":"2026-07-01","estado":"TERMINADO","presupuesto":850.00}'
```

### Paso 8 — Marcar el proyecto como entregado

```bash
curl -X PUT http://localhost:8080/api/v1/proyectos/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Mesa de comedor a medida","clienteId":1,"fechaInicio":"2026-06-01","fechaEntregaPrevista":"2026-07-01","estado":"ENTREGADO","presupuesto":850.00}'
```

Con esto, el ciclo completo de un encargo queda recogido: cliente, inventario, proyecto, asignación con control de stock, y cierre del proyecto.

---

## 8. Errores habituales y qué significan

| Código | Significado | Causa típica |
|---|---|---|
| 400 | Petición incorrecta | Datos no válidos: falta un campo obligatorio, la fecha de entrega es anterior a la de inicio o el cambio de estado no está permitido. También JSON mal formado, un valor no admitido en un campo enumerado (la respuesta indica los permitidos) o un campo de ordenación inexistente |
| 401 | No autenticado | Falta el token, el token es inválido, o ha caducado (vuelve a hacer login) |
| 403 | Sin permiso | El usuario autenticado es USER e intenta una acción reservada a ADMIN |
| 404 | No encontrado | El `id` indicado (cliente, material o proyecto) no existe |
| 409 | Conflicto | Se intenta asignar más cantidad de la que hay en stock, o eliminar un cliente/material que está en uso |
| 500 | Error interno | Fallo inesperado del servidor; revisar los logs |

Todos los errores devuelven un cuerpo JSON con esta forma:

```json
{
  "timestamp": "...",
  "status": 404,
  "error": "No encontrado",
  "message": "Descripción concreta del problema",
  "path": "/api/v1/proyectos/999"
}
```

---

## 9. Documentación interactiva (Swagger)

Toda la API está documentada de forma interactiva en:

```
http://localhost:8080/swagger-ui.html
```

Desde ahí puedes ver todos los endpoints, sus parámetros, y probarlos directamente desde el navegador sin necesidad de curl ni Postman.
