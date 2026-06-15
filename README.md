# Learning Architecture — Evolución de Arquitecturas Distribuidas

**Autor:** Brayan Loaiza Leal  
**Curso:** Arquitecturas de Software (ARSW) — Escuela Colombiana de Ingeniería Julio Garavito

---

## ¿Qué es una arquitectura distribuida?

Un sistema distribuido es aquel donde varios programas (procesos) corren en máquinas diferentes y se comunican entre sí a través de una red. El objetivo es dividir la carga, mejorar la disponibilidad y permitir que cada componente evolucione de forma independiente.

```
Cliente ──► [Protocolo de red] ──► Servidor
```

En este proyecto se aprende la evolución de los protocolos y patrones de comunicación:

```
TCP (sockets) → HTTP → gRPC → Microservicios → API Gateway
```

---

## Requisitos

- Java 17+
- Maven 3.8+

## Compilar

```bash
mvn compile
```

---

## Parte 3 — Servidor HTTP con Java puro

### ¿Qué es HTTP?

HTTP (HyperText Transfer Protocol) es el protocolo de comunicación más usado en la web. Define un formato estándar de **petición (request)** y **respuesta (response)** en texto plano sobre TCP.

```
Cliente                          Servidor
  │                                  │
  │── GET /saludo HTTP/1.1 ─────────►│
  │   Host: localhost:8080            │
  │                                  │
  │◄── HTTP/1.1 200 OK ─────────────│
  │    Content-Type: application/json │
  │                                  │
  │    {"mensaje": "Hola!"}          │
```

### Anatomía de una petición HTTP

```
[MÉTODO] [RUTA] [VERSIÓN]
[Cabeceras]

[Cuerpo - opcional]
```

Ejemplo real:
```
GET /saludo HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### Métodos HTTP principales

| Método | Para qué se usa |
|--------|----------------|
| `GET` | Obtener un recurso (sin modificar nada) |
| `POST` | Enviar datos para crear un recurso |
| `PUT` | Actualizar un recurso completo |
| `DELETE` | Eliminar un recurso |

### Códigos de respuesta HTTP

| Código | Significado |
|--------|------------|
| `200 OK` | Todo bien, respuesta exitosa |
| `201 Created` | Recurso creado exitosamente |
| `400 Bad Request` | La petición tiene errores |
| `404 Not Found` | El recurso no existe |
| `500 Internal Server Error` | Error en el servidor |

### HTTP vs TCP puro (Parte 1 y 2)

| Característica | TCP puro | HTTP |
|---------------|----------|------|
| Protocolo | Tú defines el formato | Formato estándar definido |
| Compatibilidad | Solo tu cliente funciona | Cualquier navegador o cliente HTTP |
| Rutas | No existe el concepto | `/usuarios`, `/productos`, etc. |
| Headers | No existen | `Content-Type`, `Authorization`, etc. |
| Herramientas | Ninguna | curl, Postman, navegador, fetch... |

### Servidor HTTP en Java — sin frameworks

Java incluye `com.sun.net.httpserver.HttpServer` en el JDK. No requiere ninguna dependencia externa.

Las 3 clases clave:

| Clase | Para qué sirve |
|-------|---------------|
| `HttpServer` | Crea y arranca el servidor en un puerto |
| `HttpHandler` | Interfaz que implementas para manejar peticiones de una ruta |
| `HttpExchange` | Objeto que contiene la petición recibida y la respuesta a enviar |

### Código

**`ServidorHTTP.java`**:
```java
// 1. Crear servidor en el puerto 8080
HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);

// 2. Registrar rutas: cada ruta tiene su propio manejador
servidor.createContext("/saludo", new ManejadorSaludo());
servidor.createContext("/hora",   new ManejadorHora());

// 3. Arrancar
servidor.start();
```

**Implementar un `HttpHandler`**:
```java
static class ManejadorSaludo implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Leer método de la petición
        String metodo = exchange.getRequestMethod(); // "GET", "POST", etc.

        // Construir respuesta
        String respuesta = "{ \"mensaje\": \"Hola desde el servidor HTTP!\" }";

        // Enviar cabeceras
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, respuesta.getBytes().length);

        // Enviar cuerpo
        OutputStream os = exchange.getResponseBody();
        os.write(respuesta.getBytes());
        os.close();
    }
}
```

### Flujo completo de una petición

```
1. Cliente envía:  GET /saludo HTTP/1.1
2. HttpServer recibe la petición y busca el contexto "/saludo"
3. Llama a ManejadorSaludo.handle(exchange)
4. El manejador construye la respuesta y la escribe en exchange
5. HttpServer envía la respuesta al cliente
```

### Cómo probar el servidor

Arranca el servidor:
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte3.ServidorHTTP"
```

Prueba con curl:
```bash
curl http://localhost:8080/saludo
# {"mensaje": "Hola desde el servidor HTTP!"}

curl http://localhost:8080/hora
# {"hora": "2026-06-15T17:00:00.123"}
```

O abre directamente en el navegador: `http://localhost:8080/saludo`

### Salida esperada del servidor

```
Servidor HTTP iniciado en http://localhost:8080
Rutas disponibles:
  GET http://localhost:8080/saludo
  GET http://localhost:8080/hora
[Servidor] Peticion GET a /saludo
```

---

## Estructura del proyecto

```
src/main/java/edu/eci/arsw/arquitectura/
└── parte3/
    └── ServidorHTTP.java    → servidor HTTP con 2 rutas
```

---

## Ejercicio aplicado — Parte 3

Crea un servidor HTTP en Java que funcione como una **calculadora REST**.

### Requisitos

- Puerto: `8080`
- Debe tener **4 rutas**, una por operación:

| Ruta | Parámetros | Ejemplo | Respuesta esperada |
|------|-----------|---------|-------------------|
| `GET /suma` | `?a=5&b=3` | `/suma?a=5&b=3` | `{"operacion":"suma","a":5,"b":3,"resultado":8}` |
| `GET /resta` | `?a=10&b=4` | `/resta?a=10&b=4` | `{"operacion":"resta","a":10,"b":4,"resultado":6}` |
| `GET /multiplicacion` | `?a=6&b=7` | `/multiplicacion?a=6&b=7` | `{"operacion":"multiplicacion","a":6,"b":7,"resultado":42}` |
| `GET /division` | `?a=10&b=2` | `/division?a=10&b=2` | `{"operacion":"division","a":10,"b":2,"resultado":5}` |

- Si se intenta dividir entre 0, responder con código `400` y:
  `{"error": "No se puede dividir entre cero"}`

### Pista — cómo leer parámetros de la URL

Los parámetros `?a=5&b=3` se leen así:
```java
// exchange.getRequestURI().getQuery() retorna el string "a=5&b=3"
String query = exchange.getRequestURI().getQuery(); // "a=5&b=3"
String[] partes = query.split("&");                 // ["a=5", "b=3"]
int a = Integer.parseInt(partes[0].split("=")[1]);  // 5
int b = Integer.parseInt(partes[1].split("=")[1]);  // 3
```

### Pista de estructura

```java
public class CalculadoraHTTP {
    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);
        servidor.createContext("/suma",           exchange -> manejar(exchange, "suma"));
        servidor.createContext("/resta",          exchange -> manejar(exchange, "resta"));
        servidor.createContext("/multiplicacion", exchange -> manejar(exchange, "multiplicacion"));
        servidor.createContext("/division",       exchange -> manejar(exchange, "division"));
        servidor.start();
        System.out.println("Calculadora HTTP en http://localhost:8080");
    }

    static void manejar(HttpExchange exchange, String operacion) throws IOException {
        // 1. leer parámetros a y b de la URL
        // 2. calcular resultado según operación
        // 3. manejar división entre cero (respuesta 400)
        // 4. enviar respuesta JSON con código 200
    }
}
```

Prueba con:
```bash
curl "http://localhost:8080/suma?a=5&b=3"
curl "http://localhost:8080/division?a=10&b=0"
```

Cuando lo tengas, me lo muestras y lo revisamos.
