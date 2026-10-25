package ej2;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Ejercicio 2 - Manejador de un cliente (se ejecuta en un hilo del pool).
 * Mantiene el comportamiento de eco del Ejercicio 1, pero ahora por cliente.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String quien = socket.getInetAddress() + ":" + socket.getPort(); // puerto efímero del cliente
        String hilo = Thread.currentThread().getName();
        System.out.println("[" + hilo + "] Atendiendo a " + quien);

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String linea;
            while ((linea = in.readLine()) != null) {
                if ("CHAO".equalsIgnoreCase(linea.trim())) {
                    out.println("ECO: CHAO");
                    break;
                }
                out.println("ECO: " + linea);
            }
        } catch (IOException e) {
            System.err.println("[" + hilo + "] Cliente desconectado: " + e.getMessage());
        }
        System.out.println("[" + hilo + "] Conexión cerrada con " + quien + " (hilo liberado)");
    }
}
