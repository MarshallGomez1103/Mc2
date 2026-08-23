package persistence;

import domain.world.World;

import java.io.IOException;
import java.util.List;

/**
 * Frontera sencilla de persistencia. Expone CRUD sin introducir una capa DAO.
 */
public interface WorldStorage {
    void create(World world) throws IOException;

    World read(String id) throws IOException;

    void update(World world) throws IOException;

    void delete(String id) throws IOException;

    List<String> list() throws IOException;
}
