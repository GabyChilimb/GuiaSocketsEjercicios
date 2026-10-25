package ej1;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Ejercicio 1 - Consigna: Cliente de Eco.
 * Se conecta a localhost:8080, lee líneas de System.in, las envía al servidor
 * y muestra la respuesta. Escribir "CHAO" para terminar.
 */
public class EchoClient {

    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 8080;

        // try-with-resources: socket y streams se cierran solos al salir del bloque
        try (Socket socket = new Socket(host, puerto);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader consola = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("Conectado a " + host + ":" + puerto + " (escribí CHAO para salir)");

            String linea;
            while ((linea = consola.readLine()) != null) {
                out.println(linea);                 // println agrega el \n que necesita readLine() del servidor
                String respuesta = in.readLine();   // bloquea hasta recibir la respuesta
                if (respuesta == null) {
                    System.out.println("El servidor cerró la conexión.");
                    break;
                }
                System.out.println(respuesta);
                if ("CHAO".equalsIgnoreCase(linea.trim())) break;
            }
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
}
