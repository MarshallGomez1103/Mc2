package presentation.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Recursos pequeños propios; Skin es dueño de textura y fuente y los libera una vez. */
final class MenuSkin {
    private MenuSkin() { }
    static Skin create() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        Texture texture;
        try { pixel.setColor(Color.WHITE); pixel.fill(); texture = new Texture(pixel); }
        finally { pixel.dispose(); }
        skin.add("white", texture);
        Drawable panel = skin.newDrawable("white", new Color(0, 0, 0, 0));
        Drawable up = skin.newDrawable("white", new Color(0, 0, 0, 0));
        Drawable over = skin.newDrawable("white", new Color(1, 1, 1, .18f));
        Drawable down = skin.newDrawable("white", new Color(0, 0, 0, .35f));
        skin.add("panel", panel, Drawable.class);
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle(up, down, over, font);
        button.over = over;
        skin.add("default", button);
        TextField.TextFieldStyle field = new TextField.TextFieldStyle(font, Color.WHITE,
                skin.newDrawable("white", Color.WHITE), over, down);
        field.background = down;
        skin.add("default", field);
        List.ListStyle list = new List.ListStyle(font, Color.WHITE, Color.LIGHT_GRAY, over);
        list.background = skin.newDrawable("white", new Color(0, 0, 0, .25f));
        skin.add("default", list);
        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.background = panel;
        scroll.vScroll = down;
        scroll.vScrollKnob = over;
        skin.add("default", scroll);
        SelectBox.SelectBoxStyle select = new SelectBox.SelectBoxStyle(font, Color.WHITE, down, scroll, list);
        skin.add("default", select);
        return skin;
    }
}
