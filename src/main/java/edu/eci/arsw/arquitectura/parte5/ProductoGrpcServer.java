package edu.eci.arsw.arquitectura.parte5;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parte 5 - Microservicio de Productos.
 *
 * Cada microservicio de dominio vive en su propio servidor con su propio
 * puerto. Este NO sabe nada del microservicio de Inventario: son
 * completamente independientes y se despliegan por separado.
 *
 * Ejecutar: mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte5.ProductoGrpcServer"
 */
public class ProductoGrpcServer {

    private static final Map<String, ProductoProto.ProductoResponse> PRODUCTOS = new HashMap<>();

    static {
        PRODUCTOS.put("P-001", ProductoProto.ProductoResponse.newBuilder()
                .setProductoId("P-001").setNombre("Teclado mecanico").setPrecio(250000).build());
        PRODUCTOS.put("P-002", ProductoProto.ProductoResponse.newBuilder()
                .setProductoId("P-002").setNombre("Mouse inalambrico").setPrecio(80000).build());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50101)
                .addService(new ProductoServiceImpl())
                .build();

        server.start();
        System.out.println("Microservicio de Productos en puerto 50101");
        server.awaitTermination();
    }

    static class ProductoServiceImpl extends ProductoServiceGrpc.ProductoServiceImplBase {
        @Override
        public void getProducto(ProductoProto.ProductoRequest request,
                                 StreamObserver<ProductoProto.ProductoResponse> responseObserver) {

            ProductoProto.ProductoResponse producto = PRODUCTOS.get(request.getProductoId());

            if (producto == null) {
                producto = ProductoProto.ProductoResponse.newBuilder()
                        .setProductoId(request.getProductoId())
                        .setNombre("No encontrado")
                        .setPrecio(0)
                        .build();
            }

            responseObserver.onNext(producto);
            responseObserver.onCompleted();
        }
    }
}
