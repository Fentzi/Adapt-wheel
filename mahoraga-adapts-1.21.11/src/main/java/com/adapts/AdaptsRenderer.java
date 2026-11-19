package com.adapts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Projects the popped entity's head position onto the screen and draws a
 * spinning dharma wheel + "ADAPTS" there, with perspective-correct size.
 */
public final class AdaptsRenderer {
    private static final int DURATION = 80; // ticks
    private static final float WHEEL_R = 36f; // local radius of the drawn wheel
    private static final List<Effect> EFFECTS = new ArrayList<>();

    private record Effect(int entityId, long start) {}

    private AdaptsRenderer() {}

    public static void trigger(Entity entity, long worldTime) {
        EFFECTS.removeIf(e -> e.entityId() == entity.getId());
        EFFECTS.add(new Effect(entity.getId(), worldTime));
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.BLOCK_BELL_USE, 0.6f, 0.8f));
    }

    public static void render(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null || EFFECTS.isEmpty()) return;

        float partial = counter.getTickProgress(false);
        Camera camera = mc.gameRenderer.getCamera();
        float w = ctx.getScaledWindowWidth();
        float h = ctx.getScaledWindowHeight();
        float tanHalf = (float) Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);

        Iterator<Effect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            Effect fx = it.next();
            Entity e = world.getEntityById(fx.entityId());
            float age = (world.getTime() - fx.start()) + partial;
            if (e == null || age >= DURATION) { it.remove(); continue; }

            Vec3d head = e.getLerpedPos(partial)
                    .add(0, e.getHeight() + 0.9 + 0.05 * Math.sin(age * 0.2), 0);
            Vec3d rel = head.subtract(cameraPos(camera));
            Vector3f v = new Vector3f((float) rel.x, (float) rel.y, (float) rel.z);
            new Quaternionf(camera.getRotation()).conjugate().transform(v);
            float depth = -v.z;
            if (depth < 0.1f) continue; // behind the camera

            float sx = (1f + v.x / (depth * tanHalf * (w / h))) * 0.5f * w;
            float sy = (1f - v.y / (depth * tanHalf)) * 0.5f * h;
            float pxPerBlock = (h * 0.5f) / (tanHalf * depth);
            float radius = MathHelper.clamp(0.45f * pxPerBlock, 9f, 55f);

            drawEffect(ctx, mc, sx, sy, radius, age);
        }
    }

    // One-line fix point if Yarn renames this in your build (try camera.getPos()).
    private static Vec3d cameraPos(Camera camera) {
        return camera.getCameraPos();
    }

    private static void drawEffect(DrawContext ctx, MinecraftClient mc, float sx, float sy, float radius, float t) {
        float pop = MathHelper.clamp(t / 10f, 0f, 1f);
        float scale = easeOutBack(pop);
        float flip = (float) Math.cos((1f - easeOutCubic(pop)) * Math.PI * 2.0); // coin-flip turn
        float notch = easeInOutCubic(MathHelper.clamp((t - 16f) / 10f, 0f, 1f));
        float angle = notch * (float) (Math.PI / 4.0);        // one spoke = 45 degrees
        float kick = 1f + 0.12f * (float) Math.sin(notch * Math.PI);
        float sway = pop >= 1f ? 0.94f + 0.06f * (float) Math.cos(t * 0.15f) : 1f;
        float alpha = 1f - MathHelper.clamp((t - 65f) / 15f, 0f, 1f);

        float k = radius / WHEEL_R * scale * kick;
        float xs = flip * sway;
        if (Math.abs(xs) < 0.03f) xs = 0.03f;

        Matrix3x2fStack m = ctx.getMatrices();

        // Wheel
        m.pushMatrix();
        m.translate(sx, sy);
        m.scale(xs * k, k);
        m.rotate(angle);
        // soft glow
        for (int i = 0; i < 4; i++) {
            m.pushMatrix();
            m.rotate(i * (float) (Math.PI / 8));
            ctx.fill(-44, -44, 44, 44, argb(0xFFE9A0, 0.07f * alpha));
            m.popMatrix();
        }
        // fake thickness: darker copies pushed down-right
        for (int layer = 4; layer >= 1; layer--) {
            m.pushMatrix();
            m.translate(layer * 0.9f, layer * 0.9f);
            wheelShape(ctx, argb(0x5A3D00, alpha));
            m.popMatrix();
        }
        wheelShape(ctx, argb(0xFFD54A, alpha));
        m.pushMatrix();
        m.rotate((float) (Math.PI / 4));
        ctx.fill(-4, -4, 4, 4, argb(0xFFF6C2, alpha));
        m.popMatrix();
        m.popMatrix();

        // "ADAPTS"
        float tt = MathHelper.clamp((t - 24f) / 8f, 0f, 1f);
        float ta = Math.min(easeOutCubic(tt), alpha);
        int a = (int) (ta * 255f);
        if (a >= 8) {
            Text text = Text.literal("ADAPTS").formatted(Formatting.BOLD);
            float ts = MathHelper.clamp(radius / 14f, 1f, 3f);
            int tw = mc.textRenderer.getWidth(text);
            m.pushMatrix();
            m.translate(sx, sy + radius + 6f + (1f - easeOutCubic(tt)) * 6f);
            m.scale(ts, ts);
            ctx.drawTextWithShadow(mc.textRenderer, text, -tw / 2, 0, (a << 24) | 0xFFE066);
            m.popMatrix();
        }
    }

    private static void wheelShape(DrawContext ctx, int color) {
        Matrix3x2fStack m = ctx.getMatrices();
        // rim
        for (int i = 0; i < 48; i++) {
            m.pushMatrix();
            m.rotate(i * (float) (Math.PI * 2.0 / 48.0));
            ctx.fill(-3, -27, 3, -22, color);
            m.popMatrix();
        }
        // 8 spokes with handle tips
        for (int i = 0; i < 8; i++) {
            m.pushMatrix();
            m.rotate(i * (float) (Math.PI / 4.0));
            ctx.fill(-2, -34, 2, 0, color);
            ctx.fill(-5, -36, 5, -30, color);
            m.popMatrix();
        }
        // hub
        m.pushMatrix();
        m.rotate((float) (Math.PI / 4.0));
        ctx.fill(-8, -8, 8, 8, color);
        m.popMatrix();
    }

    private static int argb(int rgb, float a) {
        return (((int) (MathHelper.clamp(a, 0f, 1f) * 255f)) << 24) | (rgb & 0xFFFFFF);
    }

    private static float easeOutCubic(float x) { float f = 1f - x; return 1f - f * f * f; }

    private static float easeInOutCubic(float x) {
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        float f = x - 1f;
        return 1f + c3 * f * f * f + c1 * f * f;
    }
}
