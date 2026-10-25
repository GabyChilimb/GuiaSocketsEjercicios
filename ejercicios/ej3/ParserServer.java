package ej3;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ejercicio 3 - Servidor multihilo (Ej. 2) + ProtocolParser.
 * Reconoce CONNECT, JOIN, MSG y QUIT y responde según el protocolo.
 * Todavía SIN broadcast (eso es el Ejercicio 4): MSG solo se confirma al emisor.
 *
 * Prueba: nc localhost 8080
 */
public class ParserServer {

    private static final int PUERTO = 8080;
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servidor con protocolo JSMS (Ej. 3) en puerto " + PUERTO);
            while (true) {
                Socket socket = server.accept();
                pool.execute(() -> atender(socket));
            }
        }
    }

    private static void atender(Socket socket) {
        String usuario = null;
        String sala = null;

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String linea;
            while ((linea = in.readLine()) != null) {
                Command cmd = ProtocolParser.parse(linea);

                if (!cmd.isValid()) {                 // error descriptivo, el socket sigue vivo
                    out.println("400 ERROR " + cmd.getError());
                    continue;
                }

                switch (cmd.getType()) {
                    case CONNECT:
                        if (usuario != null) {
                            out.println("401 ERROR Ya estas conectado como " + usuario);
                        } else {
                            usuario = cmd.getArgument();
                            out.println("200 OK");
                        }
                        break;
                    case JOIN:
                        if (usuario == null) { out.println("403 ERROR Primero CONNECT <username>"); break; }
                        sala = cmd.getArgument();
                        out.println("JOINED " + sala);
                        break;
                    case MSG:
                        if (usuario == null) { out.println("403 ERROR Primero CONNECT <username>"); break; }
                        if (sala == null)    { out.println("403 ERROR Primero JOIN <nombre_sala>"); break; }
                        out.println("MSG " + usuario + ": " + cmd.getArgument());
                        break;
                    case QUIT:
                        out.println("BYE");
                        return;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Cliente desconectado: " + e.getMessage());
        }
    }
}
