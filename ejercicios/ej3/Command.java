package ej3;

/**
 * Ejercicio 3 - Objeto estructurado que representa un comando ya parseado.
 * Si la sintaxis es inválida, type = INVALID y error tiene el motivo.
 */
public final class Command {

    public enum Type { CONNECT, JOIN, MSG, QUIT, INVALID }

    private final Type type;
    private final String argument;
    private final String error;

    private Command(Type type, String argument, String error) {
        this.type = type;
        this.argument = argument;
        this.error = error;
    }

    public static Command of(Type type, String argument) {
        return new Command(type, argument, null);
    }

    public static Command invalid(String error) {
        return new Command(Type.INVALID, null, error);
    }

    public Type getType()        { return type; }
    public String getArgument()  { return argument; }
    public String getError()     { return error; }
    public boolean isValid()     { return type != Type.INVALID; }

    @Override
    public String toString() {
        return isValid() ? type + (argument != null ? "(" + argument + ")" : "")
                         : "INVALID(" + error + ")";
    }
}
