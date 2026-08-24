package presentation.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import domain.block.BlockType;

/**
 * Atlas original de materiales del mundo. Una sola textura contiene una cuadrícula 4 x 2:
 * césped, tierra, piedra y arena en la primera fila; grava, madera, hojas y aire en la segunda.
 * Compartir el atlas evita cargar una textura independiente por cada tipo de bloque.
 */
public final class BlockTextureAtlas implements Disposable {
    private static final String RESOURCE_PATH = "textures/block-atlas-v1.png";
    private static final int COLUMNS = 4;
    private static final int ROWS = 2;

    private final Texture texture;
    private final TextureRegion grass;
    private final TextureRegion dirt;
    private final TextureRegion stone;
    private final TextureRegion sand;
    private final TextureRegion gravel;
    private final TextureRegion wood;
    private final TextureRegion leaves;

    public BlockTextureAtlas() {
        texture = new Texture(Gdx.files.internal(RESOURCE_PATH));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        grass = regionAt(0, 0);
        dirt = regionAt(1, 0);
        stone = regionAt(2, 0);
        sand = regionAt(3, 0);
        gravel = regionAt(0, 1);
        wood = regionAt(1, 1);
        leaves = regionAt(2, 1);
    }

    public Texture texture() {
        return texture;
    }

    /** Región correspondiente a un bloque sólido; AIR no se renderiza y no requiere región. */
    public TextureRegion regionFor(BlockType type) {
        return switch (type) {
            case GRASS -> grass;
            case DIRT -> dirt;
            case STONE -> stone;
            case SAND -> sand;
            case GRAVEL -> gravel;
            case WOOD -> wood;
            case LEAVES -> leaves;
            case AIR -> throw new IllegalArgumentException("AIR no tiene textura renderizable");
        };
    }

    private TextureRegion regionAt(int column, int row) {
        int left = column * texture.getWidth() / COLUMNS;
        int right = (column + 1) * texture.getWidth() / COLUMNS;
        int top = row * texture.getHeight() / ROWS;
        int bottom = (row + 1) * texture.getHeight() / ROWS;
        return new TextureRegion(texture, left, top, right - left, bottom - top);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
