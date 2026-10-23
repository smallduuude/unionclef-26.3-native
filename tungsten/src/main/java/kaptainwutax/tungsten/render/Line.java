package kaptainwutax.tungsten.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import kaptainwutax.tungsten.TungstenModDataContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class Line extends Renderer {

    public Vec3 start;
    public Vec3 end;
    public Color color;

    public Line() {
        this(Vec3.ZERO, Vec3.ZERO, Color.WHITE);
    }

    public Line(Vec3 start, Vec3 end) {
        this(start, end, Color.WHITE);
    }

    public Line(Vec3 start, Vec3 end, Color color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    @Override
    public void render(BufferBuilder builder) {
        if(TungstenModDataContainer.gameRenderer == null || this.start == null || this.end == null || this.color == null)return;
        //#if MC < 12111
        //$$ Vec3d camPos = TungstenModDataContainer.gameRenderer.getCamera().getPos();
        //$$ this.putVertex(builder, camPos, this.start);
        //$$ this.putVertex(builder, camPos, this.end);
        //#else
        // Two passes so the cue reads against real terrain (user 2026-09-10): a SOLID,
        // depth-tested line where it is actually visible, plus the same line at 50% alpha drawn
        // THROUGH walls where it is occluded. A single ignoreOcclusion pass (what this was) made
        // every route/break/place overlay a flat full-alpha smear that ignored the world; a single
        // depth-tested pass would vanish behind terrain. Every box (Cuboid = 12 Lines) and the
        // route line inherit this, so break outlines (red) and place outlines (green) get it too.
        net.minecraft.gizmos.Gizmos.line(this.start, this.end, this.color.toARGB(255), 3.0f);
        net.minecraft.gizmos.Gizmos.line(this.start, this.end, this.color.toARGB(128), 2.0f).setAlwaysOnTop();
        //#endif
    }

    protected void putVertex(BufferBuilder buffer, Vec3 camPos, Vec3 pos) {
        buffer.addVertex(
                (float) (pos.x() - camPos.x),
                (float) (pos.y() - camPos.y),
                (float) (pos.z() - camPos.z)
        ).setColor(
                this.color.getFRed(),
                this.color.getFGreen(),
                this.color.getFBlue(),
                1.0F
        );
    }

    @Override
    public BlockPos getPos() {
        double x = (this.end.x() - this.start.x()) / 2 + this.start.x();
        double y = (this.end.y() - this.start.y()) / 2 + this.start.y();
        double z = (this.end.z() - this.start.z()) / 2 + this.start.z();
        return new BlockPos((int) x, (int) y, (int) z);
    }

}
