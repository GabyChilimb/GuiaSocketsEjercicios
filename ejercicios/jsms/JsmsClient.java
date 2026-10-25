package jsms;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente CLI del sistema JSMS con recepción asíncrona.
 *
 *  - Hilo "listener": escucha al servidor todo el tiempo e imprime lo que llega
 *    (así ves los mensajes de la sala aunque no estés escribiendo).
 *  - Hilo principal: lee System.in y envía al servidor.
 *
 * Uso: java -cp out jsms.JsmsClient [host] [puerto]
 */
public class JsmsClient {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        try (Socket socket = new Socket(host, puerto);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader consola = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            // Hilo dedicado a escuchar al servidor
            Thread listener = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        System.out.println("<< " + linea);
                        if (linea.equals("BYE") || linea.startsWith("SERVER_SHUTDOWN")
                                || linea.startsWith("408 TIMEOUT")) {
                            break;
                        }
                    }
                } catch (IOException ignored) {
                    // el socket se cerró: salimos
                }
                System.out.println("Conexion finalizada.");
                System.exit(0);
            }, "listener");
            listener.setDaemon(true);
            listener.start();

            // Hilo principal: consola -> servidor
            String linea;
            while ((linea = consola.readLine()) != null) {
                if (linea.isBlank()) continue;
                out.println(linea);
                if ("QUIT".equalsIgnoreCase(linea.trim())) {
                    listener.join(2000); // esperamos el BYE
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo conectar a " + host + ":" + puerto + " -> " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
