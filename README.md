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

## Roadmap

| Parte | Tema | Protocolo/Patrón | Estado |
|-------|------|-------------------|--------|
| **3** | Servidor HTTP con Java puro | HTTP (request/response, rutas, JSON) | ✅ Teoría + ejemplo listos. Ejercicio pendiente (calculadora REST) |
| **4** | Microservicios con gRPC | Protocol Buffers, stubs, `StreamObserver` | ⏳ Pendiente |
| **5** | Microservicios de dominio (bienestar: médico + gym) | gRPC con múltiples servicios independientes | ⏳ Pendiente |
| **6** | API Gateway | HTTP como fachada única sobre varios gRPC | ⏳ Pendiente |
| **Final** | Plataforma tipo ECICIENCIA (estudiantes, cursos, biblioteca) | Integración completa: microservicios + gateway | ⏳ Pendiente |

### Qué cubre cada parte

**Parte 3 — HTTP**
Por qué HTTP mejora sobre TCP puro: protocolo estándar, rutas, headers, códigos de estado. Se usa `com.sun.net.httpserver.HttpServer` (sin frameworks).

**Parte 4 — gRPC**
Por qué gRPC mejora sobre HTTP/JSON: contratos fuertemente tipados (`.proto`), serialización binaria (Protobuf) más rápida que JSON, generación automática de cliente/servidor. Se aprende `protobuf-maven-plugin`, `ServerBuilder`, `ManagedChannelBuilder`, `StreamObserver`.

**Parte 5 — Microservicios de dominio**
Cada responsabilidad de negocio (citas médicas, gimnasio) vive en su propio servicio gRPC independiente, con su propio puerto y su propio `.proto`. Introduce el problema de **namespace compartido** en Protobuf (todos los `.proto` comparten un espacio de nombres global).

**Parte 6 — API Gateway**
Por qué un cliente no debería hablar directo con N microservicios: el Gateway centraliza, agrega respuestas de varios servicios gRPC y expone una sola API HTTP simple para el cliente final.

**Final — Plataforma integrada**
Aplica todo lo anterior en un caso completo: 3 microservicios gRPC (estudiantes, cursos, biblioteca) + 1 Gateway HTTP que los une.

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

---

## Parte 4 — gRPC básico

### ¿Por qué gRPC y no HTTP/JSON?

HTTP + JSON es texto plano: legible pero pesado de parsear y sin un contrato fijo entre cliente y servidor (nada impide que el JSON cambie de forma silenciosa). **gRPC** resuelve esto con:

| Problema de HTTP/JSON | Solución de gRPC |
|----------------------|-------------------|
| Sin contrato fijo | Contrato `.proto` compartido entre cliente y servidor |
| JSON es texto, pesado de procesar | Protobuf serializa en binario, más rápido y compacto |
| Hay que escribir el cliente HTTP a mano | El cliente (stub) se **genera automáticamente** del `.proto` |
| No hay streaming nativo | gRPC soporta streaming bidireccional nativamente |

### Protocol Buffers (`.proto`)

Un archivo `.proto` define el **contrato**: qué métodos existen y qué forma tienen sus mensajes. De este archivo, `protoc` genera automáticamente las clases Java de cliente y servidor.

```protobuf
syntax = "proto3";

option java_package = "edu.eci.arsw.arquitectura.parte4";
option java_outer_classname = "SaludoProto";

service SaludoService {
    rpc Saludar (SaludoRequest) returns (SaludoResponse);
}

message SaludoRequest {
    string nombre = 1;
}

message SaludoResponse {
    string mensaje = 1;
}
```

```
.proto  ──protoc──►  SaludoServiceGrpc.java (stub + base del servidor)
                      SaludoProto.java       (clases SaludoRequest/SaludoResponse)
```

### Las piezas de gRPC en Java

| Pieza | Para qué sirve |
|-------|----------------|
| `XxxServiceGrpc.XxxServiceImplBase` | Clase generada que extiendes para implementar el servidor |
| `ServerBuilder.forPort(N)` | Crea el servidor gRPC en el puerto N |
| `StreamObserver<Response>` | Mecanismo para enviar la respuesta de vuelta al cliente |
| `ManagedChannelBuilder` | Crea la conexión de red del cliente hacia el servidor |
| `XxxServiceGrpc.newBlockingStub(channel)` | Cliente que llama métodos remotos como si fueran locales |

### Código — Servidor

```java
public class SaludoGrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50100)
                .addService(new SaludoServiceImpl())
                .build();
        server.start();
        server.awaitTermination();
    }

    static class SaludoServiceImpl extends SaludoServiceGrpc.SaludoServiceImplBase {
        @Override
        public void saludar(SaludoProto.SaludoRequest request,
                             StreamObserver<SaludoProto.SaludoResponse> responseObserver) {

            String mensaje = "Hola " + request.getNombre() + ", bienvenido a gRPC!";

            SaludoProto.SaludoResponse respuesta = SaludoProto.SaludoResponse.newBuilder()
                    .setMensaje(mensaje)
                    .build();

            responseObserver.onNext(respuesta);  // envía la respuesta
            responseObserver.onCompleted();      // cierra el stream
        }
    }
}
```

### Código — Cliente

```java
public class SaludoGrpcClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50100)
                .usePlaintext()  // sin TLS, solo desarrollo local
                .build();

        SaludoServiceGrpc.SaludoServiceBlockingStub stub = SaludoServiceGrpc.newBlockingStub(channel);

        SaludoProto.SaludoRequest peticion = SaludoProto.SaludoRequest.newBuilder()
                .setNombre("Brayan")
                .build();

        SaludoProto.SaludoResponse respuesta = stub.saludar(peticion);
        System.out.println(respuesta.getMensaje());

        channel.shutdown();
    }
}
```

### Cómo ejecutar

```bash
# Terminal 1 - servidor
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte4.SaludoGrpcServer"

# Terminal 2 - cliente
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte4.SaludoGrpcClient"
```

### Salida esperada

```
# Servidor
Servidor gRPC iniciado en puerto 50100
[Servidor gRPC] Peticion recibida de: Brayan

# Cliente
[Cliente] Respuesta del servidor: Hola Brayan, bienvenido a gRPC!
```

---

## Ejercicio aplicado — Parte 4

Crea un servicio gRPC de **conversión de monedas**.

### Requisitos

- Archivo `conversion.proto` con:
  - `service ConversionService` con método `Convertir(ConversionRequest) returns (ConversionResponse)`
  - `ConversionRequest`: `monto` (double), `monedaOrigen` (string), `monedaDestino` (string)
  - `ConversionResponse`: `montoConvertido` (double), `monedaDestino` (string)
- Tasas fijas soportadas: USD→COP = 4000, COP→USD = 1/4000, EUR→COP = 4300
- Puerto del servidor: `50110`
- Crea también un cliente que pida la conversión de `100 USD a COP` y muestre el resultado

### Pista de estructura

```protobuf
service ConversionService {
    rpc Convertir (ConversionRequest) returns (ConversionResponse);
}

message ConversionRequest {
    double monto = 1;
    string monedaOrigen = 2;
    string monedaDestino = 3;
}

message ConversionResponse {
    double montoConvertido = 1;
    string monedaDestino = 2;
}
```

Cuando lo tengas, me lo muestras y lo revisamos.

---

## Parte 5 — Microservicios de dominio

### ¿Qué es un microservicio?

Un microservicio es un servicio gRPC (o HTTP) **independiente**, responsable de una sola parte del negocio, con su propio puerto y su propio ciclo de vida. Si uno se cae, los demás siguen funcionando.

```
Microservicio Productos   (puerto 50101)  ──► sabe de nombres y precios
Microservicio Inventario  (puerto 50102)  ──► sabe de stock disponible

Ambos NO se conocen entre sí. Cada uno tiene su propio .proto.
```

### El problema del namespace compartido en Protobuf

`protoc` compila **todos** los `.proto` de un proyecto en el mismo espacio de nombres global, sin importar el `java_package` que declares. Esto significa que si dos archivos `.proto` definen un mensaje con el mismo nombre (ej. ambos definen `StockRequest`), `protoc` falla con un error de "ya definido".

```
producto.proto   define ProductoRequest, ProductoResponse
inventario.proto define ConsultaStockRequest, ConsultaStockResponse
                          ↑
                  nombre único para evitar colisión, aunque viva en
                  un java_package distinto
```

**Regla práctica:** nombra tus mensajes de forma específica al dominio (`ConsultaStockRequest`, no `StockRequest` o `Request`) para evitar colisiones cuando el proyecto crezca con más microservicios.

### Código — microservicio de Productos

```protobuf
// producto.proto
service ProductoService {
    rpc GetProducto (ProductoRequest) returns (ProductoResponse);
}
message ProductoRequest  { string productoId = 1; }
message ProductoResponse { string productoId = 1; string nombre = 2; double precio = 3; }
```

```java
public class ProductoGrpcServer {
    private static final Map<String, ProductoProto.ProductoResponse> PRODUCTOS = new HashMap<>();
    static {
        PRODUCTOS.put("P-001", ProductoProto.ProductoResponse.newBuilder()
                .setProductoId("P-001").setNombre("Teclado mecanico").setPrecio(250000).build());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50101).addService(new ProductoServiceImpl()).build();
        server.start();
        server.awaitTermination();
    }
    // ProductoServiceImpl busca en el mapa y responde
}
```

### Código — microservicio de Inventario

```protobuf
// inventario.proto
service InventarioService {
    rpc ConsultarStock (ConsultaStockRequest) returns (ConsultaStockResponse);
}
message ConsultaStockRequest  { string productoId = 1; }
message ConsultaStockResponse { string productoId = 1; int32 unidadesDisponibles = 2; }
```

Misma estructura que Productos, pero en el **puerto 50102** y con su propio mapa de stock.

### Cómo ejecutar

```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte5.ProductoGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte5.InventarioGrpcServer"
```

Ambos corren **al mismo tiempo**, en procesos separados, sin saber el uno del otro.

---

## Ejercicio aplicado — Parte 5

Crea un **tercer microservicio**: `ReseñaService`.

### Requisitos

- Archivo `resena.proto`:
  - `service ReseñaService` con método `GetReseñas(ConsultaReseñaRequest) returns (ReseñaListResponse)`
  - `ConsultaReseñaRequest`: `productoId` (string)
  - `ReseñaListResponse`: `repeated string comentarios`
- Puerto: `50103`
- Precarga al menos 2 productos con 2 reseñas cada uno
- Verifica que **no colisione** ningún nombre de mensaje con `producto.proto` o `inventario.proto`

### Pista de estructura

```protobuf
service ReseñaService {
    rpc GetReseñas (ConsultaReseñaRequest) returns (ReseñaListResponse);
}
message ConsultaReseñaRequest { string productoId = 1; }
message ReseñaListResponse    { repeated string comentarios = 1; }
```

Cuando lo tengas, me lo muestras y lo revisamos.

---

## Parte 6 — API Gateway

### ¿Por qué un Gateway?

Sin Gateway, el cliente final (navegador, app móvil) tendría que:
- Conocer las direcciones de **todos** los microservicios
- Hacer **varias llamadas** y combinar las respuestas él mismo
- Hablar el protocolo gRPC directamente (los navegadores no lo soportan nativamente)

```
SIN Gateway:
Cliente ──► gRPC Productos (50101)
Cliente ──► gRPC Inventario (50102)
        (el cliente combina las respuestas)

CON Gateway:
Cliente ──► HTTP Gateway (8090) ──► gRPC Productos (50101)
                                ──► gRPC Inventario (50102)
        (el Gateway combina las respuestas, el cliente solo ve UNA ruta)
```

El **API Gateway** centraliza: expone una sola API HTTP simple, y por dentro reparte las llamadas a los microservicios gRPC correspondientes, agregando sus respuestas en una sola.

### Código

```java
public class TiendaGateway {
    private static ProductoServiceGrpc.ProductoServiceBlockingStub productoStub;
    private static InventarioServiceGrpc.InventarioServiceBlockingStub inventarioStub;

    public static void main(String[] args) throws IOException {
        // Canales hacia cada microservicio gRPC
        ManagedChannel canalProducto = ManagedChannelBuilder.forAddress("localhost", 50101).usePlaintext().build();
        productoStub = ProductoServiceGrpc.newBlockingStub(canalProducto);

        ManagedChannel canalInventario = ManagedChannelBuilder.forAddress("localhost", 50102).usePlaintext().build();
        inventarioStub = InventarioServiceGrpc.newBlockingStub(canalInventario);

        // Una sola ruta HTTP para el cliente final
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8090), 0);
        servidor.createContext("/api/producto", TiendaGateway::manejarProducto);
        servidor.start();
    }

    private static void manejarProducto(HttpExchange exchange) throws IOException {
        String id = exchange.getRequestURI().getQuery().split("=")[1];

        // Llama a los dos microservicios gRPC
        ProductoProto.ProductoResponse producto = productoStub.getProducto(
                ProductoProto.ProductoRequest.newBuilder().setProductoId(id).build());

        InventarioProto.ConsultaStockResponse stock = inventarioStub.consultarStock(
                InventarioProto.ConsultaStockRequest.newBuilder().setProductoId(id).build());

        // Combina ambas respuestas en un solo JSON
        String json = String.format(
                "{\"productoId\":\"%s\",\"nombre\":\"%s\",\"precio\":%.0f,\"unidadesDisponibles\":%d}",
                producto.getProductoId(), producto.getNombre(), producto.getPrecio(), stock.getUnidadesDisponibles());

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.getBytes().length);
        exchange.getResponseBody().write(json.getBytes());
        exchange.getResponseBody().close();
    }
}
```

### Cómo ejecutar (3 terminales)

```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte5.ProductoGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte5.InventarioGrpcServer"

# Terminal 3
mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte6.TiendaGateway"
```

Prueba:
```bash
curl "http://localhost:8090/api/producto?id=P-001"
# {"productoId":"P-001","nombre":"Teclado mecanico","precio":250000,"unidadesDisponibles":15}
```

---

## Ejercicio aplicado — Parte 6

Extiende `TiendaGateway` para que también agregue las **reseñas** del microservicio que creaste en el ejercicio de la Parte 5.

### Requisitos

- Nueva ruta: `GET /api/producto/completo?id=X`
- Debe llamar a **3 microservicios**: Productos (50101), Inventario (50102) y Reseñas (50103)
- Respuesta esperada:
  ```json
  {
    "productoId": "P-001",
    "nombre": "Teclado mecanico",
    "precio": 250000,
    "unidadesDisponibles": 15,
    "comentarios": ["Excelente producto", "Muy buena calidad"]
  }
  ```

Cuando lo tengas, me lo muestras y lo revisamos.

---

## Ejercicio Final — Plataforma integrada

Diseña e implementa una plataforma de **biblioteca + préstamos** que integre todo lo aprendido.

### Requisitos

**3 microservicios gRPC independientes:**

| Microservicio | Puerto | Responsabilidad |
|--------------|--------|------------------|
| `LibroService` | 50120 | CRUD de libros: id, título, autor, disponible |
| `UsuarioService` | 50121 | Registro y consulta de usuarios: id, nombre, email |
| `PrestamoService` | 50122 | Registrar préstamo (libroId + usuarioId), validar disponibilidad, fecha de devolución |

**1 API Gateway HTTP (puerto 8095)** con estas rutas:

| Ruta | Método | Qué hace |
|------|--------|----------|
| `/api/libros?id=X` | GET | Consulta un libro |
| `/api/usuarios/registrar` | POST | Registra un usuario nuevo |
| `/api/prestamos` | POST | Registra un préstamo (llama a LibroService para validar disponibilidad, luego a PrestamoService para registrar, y marca el libro como no disponible) |

### Puntos clave a aplicar

- Cada microservicio en su propio `.proto`, sin colisión de nombres de mensajes
- El Gateway debe **orquestar** dos llamadas gRPC para `/api/prestamos` (no solo agregar, sino coordinar una operación que depende de otra)
- Manejo de error: si el libro no está disponible, el Gateway debe responder `400` con `{"error": "Libro no disponible"}` sin llamar a `PrestamoService`

### Pista de estructura

```
src/main/proto/
├── libro.proto
├── usuario.proto
└── prestamo.proto

src/main/java/edu/eci/arsw/arquitectura/final_ejercicio/
├── LibroGrpcServer.java
├── UsuarioGrpcServer.java
├── PrestamoGrpcServer.java
└── BibliotecaGateway.java
```

Este es el ejercicio que integra todo el roadmap: HTTP (Parte 3) → gRPC (Parte 4) → microservicios independientes (Parte 5) → Gateway que orquesta (Parte 6). Cuando lo tengas, me lo muestras y lo revisamos.

---

## Estructura del proyecto

```
src/main/proto/
├── saludo.proto        → Parte 4
├── producto.proto       → Parte 5
└── inventario.proto      → Parte 5

src/main/java/edu/eci/arsw/arquitectura/
├── parte3/
│   └── ServidorHTTP.java           → servidor HTTP con 2 rutas
├── parte4/
│   ├── SaludoGrpcServer.java       → servidor gRPC básico
│   └── SaludoGrpcClient.java       → cliente gRPC básico
├── parte5/
│   ├── ProductoGrpcServer.java     → microservicio independiente (50101)
│   └── InventarioGrpcServer.java   → microservicio independiente (50102)
└── parte6/
    └── TiendaGateway.java          → Gateway HTTP que agrega Productos + Inventario (8090)
```
