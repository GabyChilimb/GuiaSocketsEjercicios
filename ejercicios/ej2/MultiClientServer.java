package ej2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ejercicio 2 - Servidor multihilo con Thread Pool.
 * El hilo principal solo hace accept(); cada conexión se delega a un worker del pool.
 */
public class MultiClientServer {

    private static final int PUERTO = 8080;
    private static final int MAX_HILOS = 10;
    private static final ExecutorService pool = Executors.newFixedThreadPool(MAX_HILOS);

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servidor Multihilo listo en puerto " + PUERTO
                    + " (pool de " + MAX_HILOS + " hilos)");

            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = server.accept();          // bloquea solo al hilo acceptor
                pool.execute(new ClientHandler(socket));  // asigna un hilo del pool
            }
        } catch (IOException e) {
            System.err.println("Error en bucle principal: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
