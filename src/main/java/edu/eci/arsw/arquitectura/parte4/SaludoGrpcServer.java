package edu.eci.arsw.arquitectura.parte4;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

/**
 * Parte 4 - Servidor gRPC básico.
 *
 * extends SaludoServiceGrpc.SaludoServiceImplBase: clase generada por protoc
 * a partir de saludo.proto. Aquí solo se implementa la lógica del método.
 *
 * Ejecutar: mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte4.SaludoGrpcServer"
 */
public class SaludoGrpcServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50100)
                .addService(new SaludoServiceImpl())
                .build();

        server.start();
        System.out.println("Servidor gRPC iniciado en puerto 50100");
        server.awaitTermination();
    }

    static class SaludoServiceImpl extends SaludoServiceGrpc.SaludoServiceImplBase {
        @Override
        public void saludar(SaludoProto.SaludoRequest request,
                             StreamObserver<SaludoProto.SaludoResponse> responseObserver) {

            String nombre = request.getNombre();
            System.out.println("[Servidor gRPC] Peticion recibida de: " + nombre);

            String mensaje = "Hola " + nombre + ", bienvenido a gRPC!";

            SaludoProto.SaludoResponse respuesta = SaludoProto.SaludoResponse.newBuilder()
                    .setMensaje(mensaje)
                    .build();

            responseObserver.onNext(respuesta); // enviar la respuesta
            responseObserver.onCompleted();      // cerrar el stream
        }
    }
}
