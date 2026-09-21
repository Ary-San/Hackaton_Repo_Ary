# Hackathon_Repo_150 — Tuckersoft Branch Engine

Primera hackathon del curso DBP. Backend del motor de ramas de *Bandersnatch*:
Spring Boot 3.3.4, Java 21, PostgreSQL, Spring Security + JWT y notificación asíncrona
por correo.

El enunciado completo está en [ENUNCIADO.md](ENUNCIADO.md) y el informe de entrega, con
el resumen de estrellas y la explicación del flujo asíncrono, en [ENTREGA.md](ENTREGA.md).

> **Resultado:** ★★★★★ 5 / 5 — 52 autotests, 284 comprobaciones, 0 fallos.

---

## Cómo levantarlo

### 1. PostgreSQL

```bash
docker run --name bandersnatch-db -e POSTGRES_DB=bandersnatch -e POSTGRES_USER=tuckersoft -e POSTGRES_PASSWORD=colin1984 -p 5432:5432 -d postgres:16
```

Si el 5432 ya está ocupado en tu máquina, mapea otro puerto (`-p 5434:5432`) y ajusta
`DB_PORT` en tu `.env`.

### 2. El `.env`

Copia la plantilla y rellénala. **Nunca se commitea**: está en `.gitignore`.

```bash
cp .env.example .env
```

`JWT_SECRET` necesita al menos 32 caracteres o la aplicación no arranca
(`WeakKeyException`). `ADMIN_EMAIL` y `ADMIN_PASSWORD` tienen que ser
`colin@tuckersoft.co.uk` / `colin1984`: los autotests entran con esas credenciales para
probar los permisos de administrador.

### 3. La aplicación

```bash
./mvnw spring-boot:run
```

Arranca en `http://localhost:8080`. Al arrancar, el `DataInitializer` crea al
administrador con la contraseña codificada con BCrypt si no existía.

---

## Cómo se prueba

**Los autotests del TA** (con la aplicación corriendo en otra terminal):

```bash
cd autotests && ./mvnw test
```

**Los tests unitarios propios** (Mockito, sin PostgreSQL ni red), desde la raíz:

```bash
./mvnw test
```

---

## La API

Todo lo que no sea `/api/v1/auth/**` exige `Authorization: Bearer <token>`.

| Método | Endpoint | Acceso |
|:--|:--|:--|
| POST | `/api/v1/auth/register` | público |
| POST | `/api/v1/auth/login` | público |
| GET | `/api/v1/users/me` | autenticado |
| GET | `/api/v1/users` | ADMIN |
| PATCH | `/api/v1/users/{id}/role` | ADMIN |
| POST | `/api/v1/nodes` | ADMIN |
| GET | `/api/v1/nodes` · `/api/v1/nodes/{id}` | autenticado |
| POST | `/api/v1/playthroughs` | autenticado |
| GET | `/api/v1/playthroughs` | usuario: las suyas · admin: todas |
| GET | `/api/v1/playthroughs/{id}` · `/{id}/path` | dueño o admin |
| POST | `/api/v1/decisions` | solo el dueño de la partida |
| GET | `/api/v1/decisions` | paginado y filtrable |
| GET | `/api/v1/decisions/{id}` · `/{id}/reality-logs` | dueño o admin |

El administrador **supervisa, no juega**: puede leer cualquier partida, pero decidir
sobre una ajena le da 403 como a cualquiera.

Todos los errores, incluidos los 401 y 403 de Spring Security, salen con el mismo
formato: `{ error, message, timestamp, path }`.
