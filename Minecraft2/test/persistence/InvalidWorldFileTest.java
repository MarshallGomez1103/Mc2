package persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CT-09 — una versión desconocida, un tipo inválido o una coordenada fuera de rango
 * no se cargan en silencio.
 *
 * <p>Cada caso parte de un documento válido y le aplica un único defecto, de modo que el
 * fallo solo puede venir de ese defecto y no de otro campo mal escrito por accidente.
 */
@DisplayName("CT-09 · archivos de mundo inválidos")
class InvalidWorldFileTest {

    private static final String SAMPLE_CHUNK =
            "{ \"x\": 0, \"z\": 0, \"blocks\": [ {\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"STONE\"} ] }";

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentosInvalidos")
    @DisplayName("se rechaza con un mensaje legible")
    void seRechazaConMensajeLegible(String caso, String json) {
        InvalidWorldFileException failure = assertThrows(
                InvalidWorldFileException.class,
                () -> WorldJsonCodec.read(json, WorldSamples.FACTORY),
                "el documento '" + caso + "' no debería cargarse");

        assertFalse(failure.getMessage() == null || failure.getMessage().isBlank(),
                "el error de '" + caso + "' debe traer un mensaje legible");
    }

    private static Stream<Arguments> documentosInvalidos() {
        return Stream.of(
                Arguments.of("json_incompleto", "{ \"schemaVersion\": 1, "),
                Arguments.of("json_basura", "esto no es json"),
                Arguments.of("raiz_no_objeto", "[ 1, 2, 3 ]"),
                Arguments.of("cadena_sin_cerrar", "{ \"id\": \"abierta }"),
                Arguments.of("campo_repetido",
                        "{ \"schemaVersion\": 1, \"schemaVersion\": 1 }"),
                Arguments.of("contenido_sobrante", render(validFields("sobrante")) + "{ \"otro\": 1 }"),
                Arguments.of("version_futura", broken(f -> f.put("schemaVersion", "2"))),
                Arguments.of("sin_version", broken(f -> f.remove("schemaVersion"))),
                Arguments.of("id_con_espacios", broken(f -> f.put("id", "\"id invalido\""))),
                Arguments.of("nombre_vacio", broken(f -> f.put("name", "\"\""))),
                Arguments.of("semilla_texto", broken(f -> f.put("seed", "\"48271\""))),
                Arguments.of("semilla_decimal", broken(f -> f.put("seed", "48271.5"))),
                Arguments.of("fecha_invalida", broken(f -> f.put("createdAt", "\"20 de agosto\""))),
                Arguments.of("jugador_ausente", broken(f -> f.remove("player"))),
                Arguments.of("jugador_nulo", broken(f -> f.put("player", "null"))),
                Arguments.of("jugador_sin_yaw", broken(f -> f.put("player",
                        "{ \"x\": 8.5, \"y\": 32.0, \"z\": 8.5, \"pitch\": 0.0 }"))),
                Arguments.of("jugador_fuera_del_mundo", broken(f -> f.put("player",
                        "{ \"x\": 0, \"y\": 200, \"z\": 0, \"yaw\": 0, \"pitch\": 0 }"))),
                Arguments.of("chunks_no_lista", broken(f -> f.put("chunks", "{}"))),
                Arguments.of("chunk_sin_bloques", broken(f -> f.put("chunks", "[ { \"x\": 0, \"z\": 0 } ]"))),
                Arguments.of("chunk_repetido",
                        broken(f -> f.put("chunks", "[ " + SAMPLE_CHUNK + ", " + SAMPLE_CHUNK + " ]"))),
                Arguments.of("bloque_fuera_de_rango", broken(f -> f.put("chunks",
                        chunkWith("{\"x\": 16, \"y\": 2, \"z\": 3, \"type\": \"STONE\"}")))),
                Arguments.of("altura_fuera_de_rango", broken(f -> f.put("chunks",
                        chunkWith("{\"x\": 1, \"y\": 64, \"z\": 3, \"type\": \"STONE\"}")))),
                Arguments.of("altura_negativa", broken(f -> f.put("chunks",
                        chunkWith("{\"x\": 1, \"y\": -1, \"z\": 3, \"type\": \"STONE\"}")))),
                Arguments.of("tipo_desconocido", broken(f -> f.put("chunks",
                        chunkWith("{\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"PIEDRA\"}")))),
                Arguments.of("tipo_aire", broken(f -> f.put("chunks",
                        chunkWith("{\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"AIR\"}")))),
                Arguments.of("coordenada_repetida", broken(f -> f.put("chunks", chunkWith(
                        "{\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"STONE\"},"
                                + " {\"x\": 1, \"y\": 2, \"z\": 3, \"type\": \"DIRT\"}"))))
        );
    }

    /** Documento válido salvo por el defecto que se le aplica. */
    private static String broken(Consumer<Map<String, String>> defect) {
        Map<String, String> fields = validFields("mundo_valido");
        defect.accept(fields);
        return render(fields);
    }

    private static Map<String, String> validFields(String id) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("schemaVersion", "1");
        fields.put("id", "\"" + id + "\"");
        fields.put("name", "\"" + id + "\"");
        fields.put("seed", "48271");
        fields.put("createdAt", "\"2026-08-20T20:30:00Z\"");
        fields.put("player", "{ \"x\": 8.5, \"y\": 32.0, \"z\": 8.5, \"yaw\": 0.0, \"pitch\": 0.0 }");
        fields.put("chunks", "[ " + SAMPLE_CHUNK + " ]");
        return fields;
    }

    private static String render(Map<String, String> fields) {
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            json.append("  \"").append(field.getKey()).append("\": ").append(field.getValue());
        }
        return json.append("\n}\n").toString();
    }

    private static String chunkWith(String blocks) {
        return "[ { \"x\": 0, \"z\": 0, \"blocks\": [ " + blocks + " ] } ]";
    }
}
