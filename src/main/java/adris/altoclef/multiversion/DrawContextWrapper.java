package adris.altoclef.multiversion;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.Nullable;

public class DrawContextWrapper {


    public static DrawContextWrapper of(GuiGraphicsExtractor context) {
        if (context == null) return null;
        return new DrawContextWrapper(context);
    }
    private final GuiGraphicsExtractor context;

    private DrawContextWrapper(GuiGraphicsExtractor context) {
        this.context = context;
    }

    private RenderType renderLayer = null;

    // used only 1.20.1 and later... can pass null in earlier versions
    public void setRenderLayer(RenderType renderLayer) {
        this.renderLayer = renderLayer;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    public void drawHorizontalLine(int x1, int x2, int y, int color) {
        context.horizontalLine(x1, x2, y, color);
    }

    public void drawVerticalLine(int x, int y1, int y2, int color) {
        context.verticalLine(x, y1, y2, color);
    }

    public void drawText(Font textRenderer, @Nullable String text, int x, int y, int color, boolean shadow) {
        context.text(textRenderer, text, x, y, color, shadow);
    }


    public org.joml.Matrix3x2fStack getMatrices() {
        return context.pose();
    }

    public int getScaledWindowWidth() {
        return context.guiWidth();
    }

    public int getScaledWindowHeight() {
        return context.guiHeight();
    }


}
