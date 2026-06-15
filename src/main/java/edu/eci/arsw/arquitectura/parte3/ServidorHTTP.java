package edu.eci.arsw.arquitectura.parte3;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Parte 3 - Servidor HTTP básico con Java puro (sin frameworks).
 *
 * HttpServer:   servidor HTTP incluido en el JDK, no requiere dependencias externas.
 * HttpHandler:  interfaz que define qué hacer cuando llega una petición a una ruta.
 * HttpExchange: representa una petición + respuesta HTTP completa.
 *
 * Ejecutar: mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte3.ServidorHTTP"
 * Probar:   http://localhost:8080/saludo
 *           http://localhost:8080/hora
 */
public class ServidorHTTP {

    public static void main(String[] args) throws IOException {
        // Crear servidor en el puerto 8080
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);

        // Registrar rutas (contextos)
        servidor.createContext("/saludo", new ManejadorSaludo());
        servidor.createContext("/hora",   new ManejadorHora());

        servidor.start();
        System.out.println("Servidor HTTP iniciado en http://localhost:8080");
        System.out.println("Rutas disponibles:");
        System.out.println("  GET http://localhost:8080/saludo");
        System.out.println("  GET http://localhost:8080/hora");
    }

    // Manejador para /saludo
    static class ManejadorSaludo implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String metodo = exchange.getRequestMethod();
            System.out.println("[Servidor] Peticion " + metodo + " a /saludo");

            String respuesta = "{ \"mensaje\": \"Hola desde el servidor HTTP!\" }";

            // Cabeceras de respuesta
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, respuesta.getBytes().length);

            // Cuerpo de respuesta
            OutputStream os = exchange.getResponseBody();
            os.write(respuesta.getBytes());
            os.close();
        }
    }

    // Manejador para /hora
    static class ManejadorHora implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String hora = java.time.LocalDateTime.now().toString();
            String respuesta = "{ \"hora\": \"" + hora + "\" }";

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, respuesta.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(respuesta.getBytes());
            os.close();
        }
    }
}
