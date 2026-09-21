package bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldDirectoryTest {
    @Test
    void jarAndIdeFindTheSameProjectWorldsDirectory(@TempDir Path temp) throws Exception {
        Path project = temp.resolve("Minecraft2");
        Path target = project.resolve("target");
        Files.createDirectories(target.resolve("classes"));
        Files.writeString(project.resolve("pom.xml"), "<project/>");

        assertEquals(project.resolve("worlds"),
                WorldDirectory.resolve(target.resolve("classes"), null));
        assertEquals(project.resolve("worlds"),
                WorldDirectory.resolve(target.resolve("minecraft2.jar"), null));
        assertEquals(temp.resolve("other"),
                WorldDirectory.resolve(target.resolve("classes"), temp.resolve("other").toString()));
    }
}
