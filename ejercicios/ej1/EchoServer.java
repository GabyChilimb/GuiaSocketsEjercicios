package ej1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Ejercicio 1 - Servidor de Eco mono-hilo.
 * Acepta UN cliente, le devuelve cada línea con el prefijo "ECO: " y termina con "CHAO".
 */
public class EchoServer {

    public static void main(String[] args) throws IOException {
        int puerto = 8080;

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en puerto " + puerto);

            // accept() bloquea hasta que llega un cliente (handshake SYN -> SYN-ACK -> ACK)
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress());

                String mensaje;
                // readLine() devuelve null cuando el cliente cierra la conexión (EOF)
                while ((mensaje = in.readLine()) != null) {
                    if ("CHAO".equalsIgnoreCase(mensaje.trim())) {
                        out.println("ECO: CHAO");
                        break;
                    }
                    out.println("ECO: " + mensaje); // autoFlush=true -> se envía de inmediato
                }
                System.out.println("Cliente desconectado. Servidor finalizado.");
            }
        }
    }
}
