package jsms;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Ejercicio 5 - Servidor JSMS completo (Java Socket Messaging System).
 *
 *  - Thread pool fijo (Ej. 2)
 *  - Protocolo CONNECT / JOIN / MSG / QUIT (Ej. 3)
 *  - Salas con broadcast thread-safe (Ej. 4)
 *  - Timeout de inactividad + Shutdown Hook con aviso a los clientes (Ej. 5)
 *
 * Uso: java -cp out jsms.JsmsServer [puerto] [timeoutSegundos]
 */
public class JsmsServer {

    private static final int MAX_HILOS = 50;

    private final int puerto;
    private final int timeoutMs;
    private final RoomManager roomManager = new RoomManager();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_HILOS);
    private final Set<ClientHandler> clientesActivos = ConcurrentHashMap.newKeySet();

    private volatile ServerSocket serverSocket;
    private volatile boolean apagando = false;

    public JsmsServer(int puerto, int timeoutMs) {
        this.puerto = puerto;
        this.timeoutMs = timeoutMs;
    }

    public void iniciar() {
        registrarShutdownHook();

        try (ServerSocket ss = new ServerSocket(puerto)) {
            this.serverSocket = ss;
            System.out.println("Servidor JSMS escuchando en puerto " + puerto
                    + " | pool: " + MAX_HILOS + " hilos | timeout: " + timeoutMs / 1000 + "s");
            System.out.println("Ctrl+C para apagar de forma ordenada.");

            while (!apagando) {
                Socket socket = ss.accept();
                ClientHandler handler = new ClientHandler(socket, roomManager, this, timeoutMs);
                clientesActivos.add(handler);
                threadPool.execute(handler);
            }
        } catch (SocketException e) {
            if (!apagando) System.err.println("Error en el ServerSocket: " + e.getMessage());
            // si estamos apagando, accept() se cortó porque cerramos el socket: es lo esperado
        } catch (IOException e) {
            System.err.println("Error en bucle principal: " + e.getMessage());
        }
    }

    void desregistrar(ClientHandler handler) {
        clientesActivos.remove(handler);
    }

    /** Ej. 5: Graceful Shutdown al recibir SIGINT (Ctrl+C) o SIGTERM. */
    private void registrarShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            apagando = true;
            System.out.println("\nIniciando apagado controlado del servidor...");

            // 1) Avisar a todos los clientes conectados
            for (ClientHandler c : clientesActivos) {
                c.enviarMensajeDirecto("SERVER_SHUTDOWN El servidor se esta apagando. Hasta luego!");
            }
            System.out.println("Clientes notificados: " + clientesActivos.size());

            // 2) Dejar de aceptar conexiones (desbloquea accept())
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException ignored) { }

            // 3) Cerrar sockets de clientes (desbloquea sus readLine())
            for (ClientHandler c : clientesActivos) {
                c.cerrarConexion();
            }

            // 4) Apagar el pool
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor apagado correctamente.");
        }, "shutdown-hook"));
    }

    public static void main(String[] args) {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int timeoutSeg = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        new JsmsServer(puerto, timeoutSeg * 1000).iniciar();
    }
}
