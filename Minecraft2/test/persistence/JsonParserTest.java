package persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Analizador JSON propio: lo que acepta, lo que convierte y lo que rechaza según RFC 8259. */
class JsonParserTest {

    @Test
    void readsEveryKindOfValue() throws Exception {
        Object value = JsonParser.parse("{\"a\": [1, -2.5, 3e2, true, false, null], \"b\": {\"c\": \"d\"}}");

        Map<?, ?> object = (Map<?, ?>) value;
        List<?> list = (List<?>) object.get("a");
        assertEquals(1L, list.get(0));
        assertEquals(-2.5, list.get(1));
        assertEquals(300.0, list.get(2));
        assertEquals(Boolean.TRUE, list.get(3));
        assertEquals(Boolean.FALSE, list.get(4));
        assertNull(list.get(5));
        assertEquals("d", ((Map<?, ?>) object.get("b")).get("c"));
    }

    @Test
    void decodesEscapes() throws Exception {
        assertEquals("\"\\/\b\f\n\r\t", JsonParser.parse("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\""));
        assertEquals("Añ€", JsonParser.parse("\"A\\u00f1\\u20AC\""));
    }

    @Test
    void keepsSurrogatePairsTogether() throws Exception {
        assertEquals("😀", JsonParser.parse("\"\\ud83d\\ude00\""));
        assertEquals("😀", JsonParser.parse("\"😀\""));
    }

    @Test
    void acceptsZeroAndDecimalsStartingWithZero() throws Exception {
        assertEquals(0L, JsonParser.parse("0"));
        assertEquals(-0.5, JsonParser.parse("-0.5"));
        assertEquals(0.0, JsonParser.parse("0e1"));
    }

    @Test
    void ignoresALeadingUtf8Bom() throws Exception {
        assertEquals(Map.of("x", 1L), JsonParser.parse("\uFEFF{\"x\": 1}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"\\u+041\"",          // signo dentro de un escape unicode
            "\"\\u-001\"",
            "\"\\u12g4\"",
            "\"\\u12\"",            // escape incompleto
            "\"\\ud800\"",          // surrogate alto suelto
            "\"\\udc00\"",          // surrogate bajo suelto
            "\"\\ud800x\"",
            "007",                  // ceros a la izquierda
            "-01",
            "[00]",
            "1.",
            "-",
            "{\"a\": 1,}",
            "[1 2]",
            "\"sin cerrar",
            "{} {}",
            "\uFEFF",               // solo un BOM, sin documento
            "{\"a\": 1}\uFEFF"      // BOM fuera del principio
    })
    void rejectsInvalidDocuments(String document) {
        assertThrows(InvalidWorldFileException.class, () -> JsonParser.parse(document));
    }

    @Test
    void rejectsNull() {
        assertThrows(InvalidWorldFileException.class, () -> JsonParser.parse(null));
    }
}
