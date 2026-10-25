package jsms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ejercicio 4 - Gestor central de salas y usuarios (estado compartido thread-safe).
 *
 *  - salas:    NombreSala -> lista de ClientHandler   (ConcurrentHashMap + CopyOnWriteArrayList)
 *  - usuarios: username   -> ClientHandler            (ConcurrentHashMap, putIfAbsent = registro atómico)
 *
 * No se usa "synchronized" alrededor de I/O de red: la concurrencia la resuelven las colecciones.
 */
public class RoomManager {

    private final Map<String, List<ClientHandler>> salas = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> usuarios = new ConcurrentHashMap<>();

    // ---------- Usuarios ----------

    /** Registra el nombre si está libre. Devuelve false si ya existe (operación atómica). */
    public boolean registrarUsuario(String username, ClientHandler cliente) {
        return usuarios.putIfAbsent(username, cliente) == null;
    }

    public void removerUsuario(String username, ClientHandler cliente) {
        if (username != null) usuarios.remove(username, cliente); // solo si sigue siendo el mismo handler
    }

    public int cantidadUsuarios() {
        return usuarios.size();
    }

    // ---------- Salas ----------

    public void unirseASala(String nombreSala, ClientHandler cliente) {
        salas.computeIfAbsent(nombreSala, k -> new CopyOnWriteArrayList<>()).add(cliente);
    }

    public void salirDeSala(String nombreSala, ClientHandler cliente) {
        if (nombreSala == null) return;
        // computeIfPresent es atómico por clave: saca al cliente y borra la sala si quedó vacía
        salas.computeIfPresent(nombreSala, (k, miembros) -> {
            miembros.remove(cliente);
            return miembros.isEmpty() ? null : miembros;
        });
    }

    public int miembrosEnSala(String nombreSala) {
        List<ClientHandler> miembros = salas.get(nombreSala);
        return miembros == null ? 0 : miembros.size();
    }

    /** Envía el mensaje a todos los miembros de la sala menos al emisor (emisor null = a todos). */
    public void broadcast(String nombreSala, String mensaje, ClientHandler emisor) {
        List<ClientHandler> miembros = salas.get(nombreSala);
        if (miembros != null) {
            // Iterar una CopyOnWriteArrayList nunca lanza ConcurrentModificationException
            for (ClientHandler miembro : miembros) {
                if (miembro != emisor) {
                    miembro.enviarMensajeDirecto(mensaje);
                }
            }
        }
    }
}
