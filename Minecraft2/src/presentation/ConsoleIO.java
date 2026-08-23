package presentation;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

/**
 * Entrada y salida por consola. Concentra el {@link Scanner} y el formato de los mensajes
 * para que las pantallas del menú solo decidan qué pedir y qué mostrar.
 *
 * <p>No conoce el dominio ni los casos de uso: es únicamente presentación.
 *
 * <p>Todas las lecturas devuelven {@link Optional#empty()} cuando la entrada se agota
 * (una tubería que termina o un Ctrl+D). Así el menú puede cerrarse con normalidad en vez
 * de morir con {@code NoSuchElementException}.
 */
public final class ConsoleIO {
    private static final String SEPARATOR = "=".repeat(40);
    private static final String AFFIRMATIVE = "s|si|sí|y|yes";

    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleIO(InputStream input, PrintStream output) {
        this.scanner = new Scanner(Objects.requireNonNull(input, "input no puede ser null"));
        this.output = Objects.requireNonNull(output, "output no puede ser null");
    }

    public ConsoleIO() {
        this(System.in, System.out);
    }

    public void title(String text) {
        output.println();
        output.println(SEPARATOR);
        output.println("  " + text);
        output.println(SEPARATOR);
    }

    public void info(String text) {
        output.println(text);
    }

    public void error(String text) {
        output.println("[!] " + text);
    }

    public void blank() {
        output.println();
    }

    /** Lee una línea completa; vacío si ya no hay más entrada. */
    public Optional<String> readLine() {
        if (!scanner.hasNextLine()) {
            return Optional.empty();
        }
        return Optional.of(scanner.nextLine().trim());
    }

    /** Muestra una etiqueta y lee la respuesta, tal cual venga. */
    public Optional<String> prompt(String label) {
        output.print(label + ": ");
        output.flush();
        return readLine();
    }

    /** Insiste hasta obtener un valor no vacío; vacío solo si se agota la entrada. */
    public Optional<String> promptRequired(String label) {
        while (true) {
            Optional<String> answer = prompt(label);
            if (answer.isEmpty()) {
                return Optional.empty();
            }
            if (!answer.get().isBlank()) {
                return answer;
            }
            error("El valor no puede estar vacío.");
        }
    }

    /**
     * Pregunta de sí o no. Solo una respuesta afirmativa explícita devuelve {@code true};
     * cualquier otra cosa, incluido el fin de la entrada, se interpreta como "no".
     */
    public boolean confirm(String question) {
        Optional<String> answer = prompt(question + " [s/N]");
        return answer
                .map(text -> text.toLowerCase(Locale.ROOT).matches(AFFIRMATIVE))
                .orElse(false);
    }
}
