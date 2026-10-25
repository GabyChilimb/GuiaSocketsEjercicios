package jsms;

import ej3.Command;
import ej3.ProtocolParser;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Ejercicios 4 y 5 - Manejador de cada cliente del sistema JSMS.
 *
 * Ej. 4: integra RoomManager; MSG <texto> hace broadcast a la sala actual.
 * Ej. 5: timeout de lectura, captura de SocketException/SocketTimeoutException
 *        y liberación de recursos (usuario + sala) pase lo que pase (bloque finally).
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RoomManager roomManager;
    private final JsmsServer server;
    private final int timeoutMs;

    private PrintWriter out;
    private volatile String username;   // null hasta que hace CONNECT
    private volatile String salaActual; // null hasta que hace JOIN

    public ClientHandler(Socket socket, RoomManager roomManager, JsmsServer server, int timeoutMs) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.server = server;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void run() {
        String origen = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        log("Nueva conexion desde " + origen);

        try {
            socket.setSoTimeout(timeoutMs);  // Ej. 5: readLine() no bloquea para siempre
            socket.setKeepAlive(true);       // Keep-Alive a nivel S.O.

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            enviarMensajeDirecto("WELCOME JSMS v1.0 - Comandos: CONNECT <user> | JOIN <sala> | MSG <texto> | QUIT");

            String linea;
            while ((linea = in.readLine()) != null) {    // null = EOF, el cliente cerró
                if (!procesar(linea)) break;             // false = QUIT
            }

        } catch (SocketTimeoutException e) {
            log("Timeout de inactividad (" + timeoutMs / 1000 + "s) para " + nombreParaLog());
            enviarMensajeDirecto("408 TIMEOUT Conexion cerrada por inactividad");
        } catch (SocketException e) {
            // Connection reset, cliente cerrado abruptamente o socket cerrado por el shutdown
            log("Conexion cortada (" + nombreParaLog() + "): " + e.getMessage());
        } catch (IOException e) {
            log("Error de I/O con " + nombreParaLog() + ": " + e.getMessage());
        } finally {
            liberarRecursos();
        }
    }

    /** Ejecuta un comando. Devuelve false si la sesión debe terminar. */
    private boolean procesar(String linea) {
        Command cmd = ProtocolParser.parse(linea);

        if (!cmd.isValid()) {
            enviarMensajeDirecto("400 ERROR " + cmd.getError()); // error descriptivo, el socket sigue
            return true;
        }

        switch (cmd.getType()) {
            case CONNECT: {
                if (username != null) {
                    enviarMensajeDirecto("401 ERROR Ya estas conectado como " + username);
                } else if (roomManager.registrarUsuario(cmd.getArgument(), this)) {
                    username = cmd.getArgument();
                    enviarMensajeDirecto("200 OK Bienvenido " + username);
                    log("Usuario registrado: " + username);
                } else {
                    enviarMensajeDirecto("401 ERROR El usuario '" + cmd.getArgument() + "' ya esta en uso");
                }
                return true;
            }
            case JOIN: {
                if (username == null) {
                    enviarMensajeDirecto("403 ERROR Primero tenes que hacer CONNECT <username>");
                    return true;
                }
                String nueva = cmd.getArgument();
                if (nueva.equals(salaActual)) {
                    enviarMensajeDirecto("JOINED " + nueva);
                    return true;
                }
                // Si estaba en otra sala, sale de esa primero
                if (salaActual != null) {
                    roomManager.broadcast(salaActual, "SYSTEM " + username + " salio de " + salaActual, this);
                    roomManager.salirDeSala(salaActual, this);
                }
                roomManager.unirseASala(nueva, this);
                salaActual = nueva;
                enviarMensajeDirecto("JOINED " + nueva);
                roomManager.broadcast(nueva, "SYSTEM " + username + " se unio a " + nueva, this);
                log(username + " -> " + nueva + " (" + roomManager.miembrosEnSala(nueva) + " miembros)");
                return true;
            }
            case MSG: {
                if (username == null) {
                    enviarMensajeDirecto("403 ERROR Primero tenes que hacer CONNECT <username>");
                } else if (salaActual == null) {
                    enviarMensajeDirecto("403 ERROR Primero tenes que hacer JOIN <nombre_sala>");
                } else {
                    String formateado = "MSG " + username + ": " + cmd.getArgument();
                    enviarMensajeDirecto(formateado);                              // confirmación al emisor
                    roomManager.broadcast(salaActual, formateado, this);         // Ej. 4: broadcast
                }
                return true;
            }
            case QUIT:
                enviarMensajeDirecto("BYE");
                return false;
            default:
                return true;
        }
    }

    /** Envía una línea a este cliente. La llaman otros hilos (broadcast), por eso es thread-safe. */
    public void enviarMensajeDirecto(String mensaje) {
        PrintWriter w = out;
        if (w != null) {
            synchronized (w) {          // bloque mínimo: evita que dos broadcasts mezclen bytes
                w.println(mensaje);     // println agrega \n -> protocolo no ambiguo
            }
        }
    }

    /** Cierra el socket desde otro hilo (lo usa el shutdown hook para desbloquear readLine()). */
    public void cerrarConexion() {
        try { socket.close(); } catch (IOException ignored) { }
    }

    /** Ej. 5: siempre se ejecuta. Evita "mensajes fantasma" a usuarios que ya no están. */
    private void liberarRecursos() {
        if (salaActual != null) {
            roomManager.salirDeSala(salaActual, this);
            roomManager.broadcast(salaActual, "SYSTEM " + username + " se desconecto", this);
        }
        roomManager.removerUsuario(username, this);
        server.desregistrar(this);
        cerrarConexion();
        log("Recursos liberados de " + nombreParaLog());
    }

    private String nombreParaLog() {
        return username != null ? username : "cliente-anonimo";
    }

    private void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }
}
