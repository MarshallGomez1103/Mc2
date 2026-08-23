package persistence;

import java.io.IOException;

/**
 * Señala que un archivo de mundo está dañado o no cumple el esquema acordado.
 * Tener un tipo propio permite distinguir "el archivo es inválido" de cualquier otro
 * fallo de entrada/salida, que es lo que exige el caso de prueba CT-09.
 */
public final class InvalidWorldFileException extends IOException {
    private static final long serialVersionUID = 1L;

    public InvalidWorldFileException(String message) {
        super(message);
    }

    public InvalidWorldFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
