package presentation.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GraphicsDiagnosticsTest {
    @Test void userInterfaceShowsCategoryAndTerminalKeepsDevice() {
        var intel = new GraphicsDiagnostics("Intel", "Mesa Intel(R) Graphics", "4.6", "LWJGL3");
        var nvidia = new GraphicsDiagnostics("NVIDIA Corporation", "GeForce RTX 4060", "4.6", "LWJGL3");
        assertEquals("Integrada", intel.deviceLabel());
        assertEquals("Dedicada", nvidia.deviceLabel());
        assertTrue(nvidia.summary().contains("GeForce RTX 4060"));
        assertEquals("Automática", new GraphicsDiagnostics("unknown", "unknown", "", "").deviceLabel());
    }
}
