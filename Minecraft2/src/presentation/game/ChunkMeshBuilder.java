package presentation.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.World;

import java.util.Objects;

/**
 * Convierte un {@link Chunk} en una malla dibujable, emitiendo <b>solo las caras visibles</b>.
 *
 * <p>Dibujar los seis lados de cada cubo sería inviable: cuatro chunks son unos 22 000 bloques,
 * es decir 132 000 caras, y casi todas están enterradas. Aquí se emite una cara únicamente
 * cuando su bloque vecino es aire o no existe, lo que deja solo la piel del terreno.
 *
 * <p>La consulta del vecino va contra el {@link World} y no contra el chunk, porque si no las
 * caras del borde entre dos chunks contiguos se dibujarían aunque estén tapadas por el chunk
 * de al lado.
 */
public final class ChunkMeshBuilder {
    private final World world;

    public ChunkMeshBuilder(World world) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
    }

    /** Malla de un chunk junto con cuántas caras acabó emitiendo. */
    public record ChunkMesh(Model model, int faceCount, int blockCount) {
    }

    public ChunkMesh build(Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk no puede ser null");

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder part = modelBuilder.part(
                "chunk_" + chunk.getChunkX() + "_" + chunk.getChunkZ(),
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position
                        | VertexAttributes.Usage.Normal
                        | VertexAttributes.Usage.ColorPacked,
                new Material(ColorAttribute.createDiffuse(Color.WHITE)));

        int faces = 0;
        int blocks = 0;
        for (Block block : chunk.getBlocks()) {
            if (block.getType() == BlockType.AIR) {
                continue;
            }
            blocks++;
            Position position = block.getPosition();
            for (BlockAppearance.Face face : BlockAppearance.Face.values()) {
                if (isSolidAt(position.x() + face.offsetX(),
                        position.y() + face.offsetY(),
                        position.z() + face.offsetZ())) {
                    continue; // Cara tapada por el vecino: no se dibuja.
                }
                part.setColor(BlockAppearance.colorOf(block.getType(), face));
                appendFace(part, position.x(), position.y(), position.z(), face);
                faces++;
            }
        }
        return new ChunkMesh(modelBuilder.end(), faces, blocks);
    }

    /**
     * Un bloque es sólido si existe y no es aire. Fuera del rango vertical del mundo, o fuera de
     * cualquier chunk generado, se trata como aire — el mismo criterio que usan
     * {@code CollisionResolver} y {@code PlayerInteractionService}.
     *
     * <p>La comprobación de altura va primero a propósito: {@code Chunk.getBlock} lanza excepción
     * si la altura sale de 0..63.
     */
    private boolean isSolidAt(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        Position position = new Position(x, y, z);
        return world.findChunk(position)
                .flatMap(chunk -> chunk.getBlock(position))
                .map(block -> block.getType() != BlockType.AIR)
                .orElse(false);
    }

    /**
     * Emite una cara del cubo que ocupa de {@code (x, y, z)} a {@code (x+1, y+1, z+1)}.
     *
     * <p>El orden de los cuatro vértices no es arbitrario: debe recorrerse en sentido antihorario
     * visto desde el lado hacia el que apunta la normal. Si se invierte, OpenGL considera la cara
     * como trasera, la descarta, y aparecen agujeros en el terreno.
     */
    private static void appendFace(MeshPartBuilder part, int x, int y, int z,
                                   BlockAppearance.Face face) {
        float x0 = x;
        float y0 = y;
        float z0 = z;
        float x1 = x + 1f;
        float y1 = y + 1f;
        float z1 = z + 1f;

        switch (face) {
            case TOP -> part.rect(
                    x0, y1, z0,
                    x0, y1, z1,
                    x1, y1, z1,
                    x1, y1, z0,
                    0f, 1f, 0f);
            case BOTTOM -> part.rect(
                    x0, y0, z0,
                    x1, y0, z0,
                    x1, y0, z1,
                    x0, y0, z1,
                    0f, -1f, 0f);
            case EAST -> part.rect(
                    x1, y0, z0,
                    x1, y1, z0,
                    x1, y1, z1,
                    x1, y0, z1,
                    1f, 0f, 0f);
            case WEST -> part.rect(
                    x0, y0, z0,
                    x0, y0, z1,
                    x0, y1, z1,
                    x0, y1, z0,
                    -1f, 0f, 0f);
            case NORTH -> part.rect(
                    x0, y0, z1,
                    x1, y0, z1,
                    x1, y1, z1,
                    x0, y1, z1,
                    0f, 0f, 1f);
            case SOUTH -> part.rect(
                    x0, y0, z0,
                    x0, y1, z0,
                    x1, y1, z0,
                    x1, y0, z0,
                    0f, 0f, -1f);
        }
    }
}
