package persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analizador JSON mínimo y sin librerías externas.
 * No conoce el mundo ni su esquema: solo convierte texto en estructuras de Java
 * ({@code Map}, {@code List}, {@code String}, {@code Long}, {@code Double}, {@code Boolean}
 * o {@code null}). La interpretación de esos datos corresponde a {@link WorldJsonCodec}.
 */
public final class JsonParser {
    /** Evita que un archivo con anidamiento extremo agote la pila. */
    private static final int MAX_DEPTH = 64;

    private final String source;
    private int index;
    private int line = 1;
    private int column = 1;

    private JsonParser(String source) {
        this.source = source;
    }

    /** Analiza un documento JSON completo y devuelve su valor principal. */
    public static Object parse(String source) throws InvalidWorldFileException {
        if (source == null) {
            throw new InvalidWorldFileException("El contenido del archivo es nulo");
        }
        JsonParser parser = new JsonParser(source);
        parser.skipWhitespace();
        Object value = parser.readValue(0);
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw parser.error("Se encontró contenido adicional después del valor principal");
        }
        return value;
    }

    private Object readValue(int depth) throws InvalidWorldFileException {
        if (depth > MAX_DEPTH) {
            throw error("El documento supera la profundidad máxima permitida (" + MAX_DEPTH + ")");
        }
        if (isAtEnd()) {
            throw error("Se esperaba un valor pero el archivo terminó");
        }

        char current = peek();
        return switch (current) {
            case '{' -> readObject(depth);
            case '[' -> readArray(depth);
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> {
                if (current == '-' || isDigit(current)) {
                    yield readNumber();
                }
                throw error("Valor JSON no reconocido que empieza por '" + current + "'");
            }
        };
    }

    private Map<String, Object> readObject(int depth) throws InvalidWorldFileException {
        expect('{');
        Map<String, Object> members = new LinkedHashMap<>();

        skipWhitespace();
        if (peekIs('}')) {
            advance();
            return members;
        }

        while (true) {
            skipWhitespace();
            if (!peekIs('"')) {
                throw error("Se esperaba el nombre de un campo entre comillas");
            }
            String field = readString();
            if (members.containsKey(field)) {
                throw error("Campo repetido dentro del mismo objeto: '" + field + "'");
            }

            skipWhitespace();
            expect(':');
            skipWhitespace();
            members.put(field, readValue(depth + 1));

            skipWhitespace();
            if (peekIs(',')) {
                advance();
                continue;
            }
            if (peekIs('}')) {
                advance();
                return members;
            }
            throw error("Se esperaba ',' o '}' dentro del objeto");
        }
    }

    private List<Object> readArray(int depth) throws InvalidWorldFileException {
        expect('[');
        List<Object> items = new ArrayList<>();

        skipWhitespace();
        if (peekIs(']')) {
            advance();
            return items;
        }

        while (true) {
            skipWhitespace();
            items.add(readValue(depth + 1));

            skipWhitespace();
            if (peekIs(',')) {
                advance();
                continue;
            }
            if (peekIs(']')) {
                advance();
                return items;
            }
            throw error("Se esperaba ',' o ']' dentro de la lista");
        }
    }

    private String readString() throws InvalidWorldFileException {
        expect('"');
        StringBuilder text = new StringBuilder();

        while (true) {
            if (isAtEnd()) {
                throw error("La cadena de texto no está cerrada");
            }
            char current = advance();
            if (current == '"') {
                return text.toString();
            }
            if (current == '\\') {
                text.append(readEscape());
                continue;
            }
            if (current < 0x20) {
                throw error("Carácter de control no permitido dentro de una cadena");
            }
            text.append(current);
        }
    }

    private char readEscape() throws InvalidWorldFileException {
        if (isAtEnd()) {
            throw error("Secuencia de escape incompleta");
        }
        char marker = advance();
        return switch (marker) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape();
            default -> throw error("Secuencia de escape desconocida: '\\" + marker + "'");
        };
    }

    private char readUnicodeEscape() throws InvalidWorldFileException {
        if (index + 4 > source.length()) {
            throw error("Secuencia de escape unicode incompleta");
        }
        String hexadecimal = source.substring(index, index + 4);
        for (int i = 0; i < 4; i++) {
            advance();
        }
        try {
            return (char) Integer.parseInt(hexadecimal, 16);
        } catch (NumberFormatException cause) {
            throw error("Secuencia de escape unicode inválida: '\\u" + hexadecimal + "'");
        }
    }

    private Object readNumber() throws InvalidWorldFileException {
        int start = index;
        if (peekIs('-')) {
            advance();
        }
        readDigits();

        boolean fractional = false;
        if (peekIs('.')) {
            fractional = true;
            advance();
            readDigits();
        }
        if (peekIs('e') || peekIs('E')) {
            fractional = true;
            advance();
            if (peekIs('+') || peekIs('-')) {
                advance();
            }
            readDigits();
        }

        String text = source.substring(start, index);
        try {
            return fractional ? (Object) Double.valueOf(text) : (Object) Long.valueOf(text);
        } catch (NumberFormatException cause) {
            throw error("Número mal formado o fuera de rango: '" + text + "'");
        }
    }

    private void readDigits() throws InvalidWorldFileException {
        int start = index;
        while (!isAtEnd() && isDigit(peek())) {
            advance();
        }
        if (index == start) {
            throw error("Se esperaban dígitos dentro del número");
        }
    }

    private Object readLiteral(String literal, Object value) throws InvalidWorldFileException {
        if (!source.startsWith(literal, index)) {
            throw error("Se esperaba el valor '" + literal + "'");
        }
        for (int i = 0; i < literal.length(); i++) {
            advance();
        }
        return value;
    }

    private void skipWhitespace() {
        while (!isAtEnd()) {
            char current = peek();
            if (current != ' ' && current != '\t' && current != '\n' && current != '\r') {
                return;
            }
            advance();
        }
    }

    private void expect(char expected) throws InvalidWorldFileException {
        if (isAtEnd() || peek() != expected) {
            throw error("Se esperaba '" + expected + "'");
        }
        advance();
    }

    private char advance() {
        char current = source.charAt(index++);
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }

    private char peek() {
        return source.charAt(index);
    }

    private boolean peekIs(char expected) {
        return !isAtEnd() && peek() == expected;
    }

    private boolean isAtEnd() {
        return index >= source.length();
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private InvalidWorldFileException error(String message) {
        return new InvalidWorldFileException(message + " (línea " + line + ", columna " + column + ")");
    }
}
