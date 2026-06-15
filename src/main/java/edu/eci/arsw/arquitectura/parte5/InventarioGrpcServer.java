package edu.eci.arsw.arquitectura.parte5;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parte 5 - Microservicio de Inventario.
 *
 * Independiente del microservicio de Productos: distinto puerto, distinto
 * proceso, distinto ciclo de vida. Si este servicio se cae, Productos sigue
 * funcionando con normalidad.
 *
 * Ejecutar: mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte5.InventarioGrpcServer"
 */
public class InventarioGrpcServer {

    private static final Map<String, Integer> STOCK = new HashMap<>();

    static {
        STOCK.put("P-001", 15);
        STOCK.put("P-002", 0);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50102)
                .addService(new InventarioServiceImpl())
                .build();

        server.start();
        System.out.println("Microservicio de Inventario en puerto 50102");
        server.awaitTermination();
    }

    static class InventarioServiceImpl extends InventarioServiceGrpc.InventarioServiceImplBase {
        @Override
        public void consultarStock(InventarioProto.ConsultaStockRequest request,
                                    StreamObserver<InventarioProto.ConsultaStockResponse> responseObserver) {

            int unidades = STOCK.getOrDefault(request.getProductoId(), 0);

            InventarioProto.ConsultaStockResponse respuesta = InventarioProto.ConsultaStockResponse.newBuilder()
                    .setProductoId(request.getProductoId())
                    .setUnidadesDisponibles(unidades)
                    .build();

            responseObserver.onNext(respuesta);
            responseObserver.onCompleted();
        }
    }
}
