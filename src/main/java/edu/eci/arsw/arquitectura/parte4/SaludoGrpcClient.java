package edu.eci.arsw.arquitectura.parte4;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Parte 4 - Cliente gRPC básico.
 *
 * ManagedChannel: la conexión de red hacia el servidor gRPC.
 * Stub generado (SaludoServiceGrpc.newBlockingStub): permite llamar al método
 * remoto como si fuera un método local. "Blocking" significa que espera la
 * respuesta antes de continuar (sincrono).
 *
 * Ejecutar: mvn exec:java -Dexec.mainClass="edu.eci.arsw.arquitectura.parte4.SaludoGrpcClient"
 * (con el servidor SaludoGrpcServer corriendo)
 */
public class SaludoGrpcClient {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50100)
                .usePlaintext() // sin TLS, solo para desarrollo local
                .build();

        SaludoServiceGrpc.SaludoServiceBlockingStub stub = SaludoServiceGrpc.newBlockingStub(channel);

        SaludoProto.SaludoRequest peticion = SaludoProto.SaludoRequest.newBuilder()
                .setNombre("Brayan")
                .build();

        SaludoProto.SaludoResponse respuesta = stub.saludar(peticion);

        System.out.println("[Cliente] Respuesta del servidor: " + respuesta.getMensaje());

        channel.shutdown();
    }
}
