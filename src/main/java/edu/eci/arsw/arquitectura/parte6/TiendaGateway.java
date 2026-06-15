package edu.eci.arsw.arquitectura.parte6;

import com.sun.net.httpserver.HttpServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import edu.eci.arsw.arquitectura.parte5.ProductoServiceGrpc;
import edu.eci.arsw.arquitectura.parte5.InventarioServiceGrpc;
import edu.eci.arsw.arquitectura.parte5.ProductoProto;
import edu.eci.arsw.arquitectura.parte5.InventarioProto;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Parte 6 - API Gateway.
 *
 * El cliente final (navegador, app movil) NO sabe que existen dos
 * microservicios gRPC distintos (Productos en 50101, Inventario en 50102).
 * Solo conoce UNA ruta HTTP: /api/producto?id=X
 *
 * El Gateway:
 *   1. Recibe la petición HTTP
 *   2. Llama a los dos servicios gRPC internamente
 *   3. Combina las dos respuestas en un solo JSON
 *   4. Devuelve la respuesta combinada al cliente
 *
 * Ejecutar: mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte6.TiendaGateway"
 * (con ProductoGrpcServer e InventarioGrpcServer corriendo)
 *
 * Probar: curl "http://localhost:8090/api/producto?id=P-001"
 */
public class TiendaGateway {

    private static ProductoServiceGrpc.ProductoServiceBlockingStub productoStub;
    private static InventarioServiceGrpc.InventarioServiceBlockingStub inventarioStub;

    public static void main(String[] args) throws IOException {
        // Canal hacia el microservicio de Productos (puerto 50101)
        ManagedChannel canalProducto = ManagedChannelBuilder.forAddress("localhost", 50101)
                .usePlaintext().build();
        productoStub = ProductoServiceGrpc.newBlockingStub(canalProducto);

        // Canal hacia el microservicio de Inventario (puerto 50102)
        ManagedChannel canalInventario = ManagedChannelBuilder.forAddress("localhost", 50102)
                .usePlaintext().build();
        inventarioStub = InventarioServiceGrpc.newBlockingStub(canalInventario);

        HttpServer servidor = HttpServer.create(new InetSocketAddress(8090), 0);
        servidor.createContext("/api/producto", TiendaGateway::manejarProducto);
        servidor.start();

        System.out.println("Gateway HTTP iniciado en http://localhost:8090");
        System.out.println("GET http://localhost:8090/api/producto?id=P-001");
    }

    private static void manejarProducto(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery(); // "id=P-001"
        String id = query.split("=")[1];

        // 1. Llamar al microservicio de Productos
        ProductoProto.ProductoRequest peticionProducto = ProductoProto.ProductoRequest.newBuilder()
                .setProductoId(id).build();
        ProductoProto.ProductoResponse producto = productoStub.getProducto(peticionProducto);

        // 2. Llamar al microservicio de Inventario
        InventarioProto.ConsultaStockRequest peticionStock = InventarioProto.ConsultaStockRequest.newBuilder()
                .setProductoId(id).build();
        InventarioProto.ConsultaStockResponse stock = inventarioStub.consultarStock(peticionStock);

        // 3. Combinar ambas respuestas en un solo JSON
        String json = String.format(
                "{\"productoId\":\"%s\",\"nombre\":\"%s\",\"precio\":%.0f,\"unidadesDisponibles\":%d}",
                producto.getProductoId(), producto.getNombre(), producto.getPrecio(), stock.getUnidadesDisponibles()
        );

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(json.getBytes());
        os.close();
    }
}
