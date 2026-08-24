package presentation.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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
    private final BlockTextureAtlas textureAtlas;

    public ChunkMeshBuilder(World world, BlockTextureAtlas textureAtlas) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
        this.textureAtlas = Objects.requireNonNull(textureAtlas, "textureAtlas no puede ser null");
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
                        | VertexAttributes.Usage.ColorPacked
                        | VertexAttributes.Usage.TextureCoordinates,
                new Material(TextureAttribute.createDiffuse(textureAtlas.texture())));

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
                appendFace(part, position.x(), position.y(), position.z(), block.getType(), face);
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
    private void appendFace(MeshPartBuilder part, int x, int y, int z, BlockType type,
                            BlockAppearance.Face face) {
        float x0 = x;
        float y0 = y;
        float z0 = z;
        float x1 = x + 1f;
        float y1 = y + 1f;
        float z1 = z + 1f;

        Color color = BlockAppearance.colorOf(type, face);
        TextureRegion region = textureAtlas.regionFor(type);
        switch (face) {
            case TOP -> appendTexturedQuad(part, region, color, 0f, 1f, 0f,
                    x0, y1, z0,
                    x0, y1, z1,
                    x1, y1, z1,
                    x1, y1, z0);
            case BOTTOM -> appendTexturedQuad(part, region, color, 0f, -1f, 0f,
                    x0, y0, z0,
                    x1, y0, z0,
                    x1, y0, z1,
                    x0, y0, z1);
            case EAST -> appendTexturedQuad(part, region, color, 1f, 0f, 0f,
                    x1, y0, z0,
                    x1, y1, z0,
                    x1, y1, z1,
                    x1, y0, z1);
            case WEST -> appendTexturedQuad(part, region, color, -1f, 0f, 0f,
                    x0, y0, z0,
                    x0, y0, z1,
                    x0, y1, z1,
                    x0, y1, z0);
            case NORTH -> appendTexturedQuad(part, region, color, 0f, 0f, 1f,
                    x0, y0, z1,
                    x1, y0, z1,
                    x1, y1, z1,
                    x0, y1, z1);
            case SOUTH -> appendTexturedQuad(part, region, color, 0f, 0f, -1f,
                    x0, y0, z0,
                    x0, y1, z0,
                    x1, y1, z0,
                    x1, y0, z0);
        }
    }

    /** Emite una cara con color de sombreado y las coordenadas UV de su región del atlas. */
    private static void appendTexturedQuad(
            MeshPartBuilder part, TextureRegion region, Color color, float normalX, float normalY, float normalZ,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz
    ) {
        short a = part.vertex(vertex(ax, ay, az, normalX, normalY, normalZ, color, region.getU(), region.getV2()));
        short b = part.vertex(vertex(bx, by, bz, normalX, normalY, normalZ, color, region.getU2(), region.getV2()));
        short c = part.vertex(vertex(cx, cy, cz, normalX, normalY, normalZ, color, region.getU2(), region.getV()));
        short d = part.vertex(vertex(dx, dy, dz, normalX, normalY, normalZ, color, region.getU(), region.getV()));
        part.rect(a, b, c, d);
    }

    private static MeshPartBuilder.VertexInfo vertex(
            float x, float y, float z, float normalX, float normalY, float normalZ,
            Color color, float u, float v
    ) {
        return new MeshPartBuilder.VertexInfo().set(
                new Vector3(x, y, z), new Vector3(normalX, normalY, normalZ), color, new Vector2(u, v));
    }
}
