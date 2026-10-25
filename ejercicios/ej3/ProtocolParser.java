package ej3;

/**
 * Ejercicio 3 - Parser del protocolo de aplicación JSMS.
 *
 *   CONNECT <username>     JOIN <nombre_sala>     MSG <texto>     QUIT
 *
 * Nunca lanza excepciones: ante sintaxis inválida devuelve Command.invalid(...)
 * con un mensaje descriptivo, así el socket NO se cae.
 */
public final class ProtocolParser {

    private static final String USERNAME_REGEX = "[A-Za-z0-9_]{3,20}";
    private static final String SALA_REGEX = "#?[A-Za-z0-9_-]{1,30}";

    private ProtocolParser() { }

    public static Command parse(String lineaBruta) {
        if (lineaBruta == null || lineaBruta.trim().isEmpty()) {
            return Command.invalid("Linea vacia");
        }

        // Máximo 2 partes: comando + resto (el texto de MSG puede tener espacios)
        String[] partes = lineaBruta.trim().split("\\s+", 2);
        String comando = partes[0].toUpperCase();
        String argumento = partes.length > 1 ? partes[1].trim() : "";

        switch (comando) {
            case "CONNECT":
                if (argumento.isEmpty())
                    return Command.invalid("Falta el usuario. Uso: CONNECT <username>");
                if (!argumento.matches(USERNAME_REGEX))
                    return Command.invalid("Usuario invalido: 3 a 20 caracteres (letras, numeros o _), sin espacios");
                return Command.of(Command.Type.CONNECT, argumento);

            case "JOIN":
                if (argumento.isEmpty())
                    return Command.invalid("Falta la sala. Uso: JOIN <nombre_sala>");
                if (!argumento.matches(SALA_REGEX))
                    return Command.invalid("Nombre de sala invalido: sin espacios, maximo 30 caracteres");
                // Normalizamos: "general" y "#general" son la misma sala
                String sala = argumento.startsWith("#") ? argumento : "#" + argumento;
                return Command.of(Command.Type.JOIN, sala.toLowerCase());

            case "MSG":
                if (argumento.isEmpty())
                    return Command.invalid("Mensaje vacio. Uso: MSG <texto>");
                return Command.of(Command.Type.MSG, argumento);

            case "QUIT":
                if (!argumento.isEmpty())
                    return Command.invalid("QUIT no lleva argumentos");
                return Command.of(Command.Type.QUIT, null);

            default:
                return Command.invalid("Comando no reconocido: " + partes[0]);
        }
    }
}
