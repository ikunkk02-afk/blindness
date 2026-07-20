package com.ikunkk02afk.blindness.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.client.contact.ContactRevealManager;
import com.ikunkk02afk.blindness.client.contact.RevealedBlock;
import com.ikunkk02afk.blindness.client.ore.OreRevealManager;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11C;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ContactOutlineRenderer {
    static final Identifier MASK_FRAMEBUFFER = BlindnessMod.id("contact_mask");
    private static final int MAX_MODEL_CACHE = ContactRevealManager.MAX_ACTIVE_REVEALS;
    private static final long DEBUG_INTERVAL_NANOS = 1_000_000_000L;
    private static final Map<BlockPos, CachedModel> MODEL_CACHE = new HashMap<>();
    private static final Set<Identifier> LOGGED_FALLBACKS = new HashSet<>();
    private static ShaderProgram blockMaskShader;
    private static ShaderProgram lineMaskShader;
    private static RegistryKey<World> cachedWorld;
    private static long lastDebugNanos;

    private ContactOutlineRenderer() {}

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(BlindnessMod.id("contact_mask_block"),
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, shader -> {
                        blockMaskShader = shader;
                        clearModelCache();
                    });
            context.register(BlindnessMod.id("contact_mask_line"), VertexFormats.LINES, shader -> {
                lineMaskShader = shader;
                clearModelCache();
            });
        });
        FabricVeilRenderLevelStageEvent.EVENT.register((stage, levelRenderer, bufferSource, matrixStack,
                                                        frustumMatrix, projectionMatrix, renderTick,
                                                        deltaTracker, camera, frustum) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) {
                renderMask(camera, frustumMatrix, projectionMatrix);
            }
        });
    }

    private static void renderMask(Camera camera, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        MinecraftClient client = MinecraftClient.getInstance();
        AdvancedFbo mask = maskFramebuffer();
        if (mask == null) {
            logDebug(client, camera, 0, false, false);
            return;
        }

        // The mask is manual so it can receive a copy of this frame's world depth. Clear only
        // color; clearing depth here would make hidden models visible through walls.
        try {
            mask.clear(GL11C.GL_COLOR_BUFFER_BIT);
            AdvancedFbo.getMainFramebuffer().resolveToAdvancedFbo(mask,
                    GL11C.GL_DEPTH_BUFFER_BIT, GL11C.GL_NEAREST);
        } catch (Exception e) {
            BlindnessMod.LOGGER.debug("Mask FBO operation failed, skipping outline pass: {}", e.toString());
            return;
        }

        List<RevealedBlock> reveals = ContactRevealManager.snapshot();
        boolean enabled = client.world != null && client.player != null && !reveals.isEmpty()
                && BlindnessPostProcessor.isReady()
                && BlindnessComponents.PLAYER.maybeGet(client.player)
                .map(component -> component.blindnessEnabled()).orElse(true);
        if (!enabled || blockMaskShader == null || lineMaskShader == null) {
            logDebug(client, camera, reveals.size(), false, true);
            return;
        }

        if (cachedWorld != client.world.getRegistryKey()) {
            clearModelCache();
            cachedWorld = client.world.getRegistryKey();
        }
        Set<BlockPos> activePositions = new HashSet<>();
        for (RevealedBlock reveal : reveals) activePositions.add(reveal.pos());
        MODEL_CACHE.keySet().removeIf(pos -> !activePositions.contains(pos));

        VertexConsumerProvider.Immediate buffers = client.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrices = new MatrixStack();
        Vec3d cameraPos = camera.getPos();
        long now = System.nanoTime();
        Random random = Random.create();

        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            // AFTER_LEVEL does not guarantee a populated PoseStack. Bind the exact camera matrices
            // supplied by Veil so model vertices and the copied world depth share one projection.
            RenderSystem.setProjectionMatrix(new Matrix4f(projectionMatrix), VertexSorter.BY_DISTANCE);
            modelView.set(viewMatrix);
            RenderSystem.applyModelViewMatrix();

            for (RevealedBlock reveal : reveals) {
                float alpha = reveal.alpha(now);
                if (alpha <= 0.001F) continue;
                BlockPos pos = reveal.pos();
                BlockState state = client.world.getBlockState(pos);
                if (state.isAir() && state.getFluidState().isEmpty()) continue;

                // Determine if this block is a detected ore.
                OreRevealManager.OreRevealEntry oreEntry = OreRevealManager.get(pos);
                boolean isOre = oreEntry != null;
                // Ore blocks get pulsating effect for emphasis.
                if (isOre) {
                    long age = now - oreEntry.startNanos();
                    float pulse = (float) (0.7 + 0.3 * Math.sin(age / 200_000_000.0));
                    alpha = Math.min(1F, alpha * pulse * 1.2F);
                }

                boolean fallback = !BlindnessClient.CONFIG.useDetailedModelOutlines()
                        || client.world.getBlockEntity(pos) != null;
                if (!fallback && !state.getFluidState().isEmpty()
                        && state.getRenderType() == net.minecraft.block.BlockRenderType.INVISIBLE) {
                    fallback = true;
                }

                if (!fallback) {
                    BakedModel model = cachedModel(client, pos, state);
                    fallback = model == null || model.isBuiltin();
                    if (!fallback) {
                        try {
                            matrices.push();
                            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y,
                                    pos.getZ() - cameraPos.z);
                            random.setSeed(state.getRenderingSeed(pos));
                            VertexConsumer raw = buffers.getBuffer(ContactRenderLayers.blockMask());
                            VertexConsumer channel = isOre
                                    ? new OreMaskVertexConsumer(raw, alpha)
                                    : new MaskChannelVertexConsumer(raw, reveal.isCenter(), alpha);
                            client.getBlockRenderManager().getModelRenderer().render(client.world, model, state, pos,
                                    matrices, channel, true, random, state.getRenderingSeed(pos),
                                    OverlayTexture.DEFAULT_UV);
                        } catch (RuntimeException exception) {
                            fallback = true;
                            logFallback(state, exception);
                        } finally {
                            matrices.pop();
                        }
                    }
                }

                if (fallback) {
                    drawVoxelFallback(client, buffers, matrices, cameraPos, reveal, state, alpha, isOre);
                    logFallback(state, null);
                }
            }

            buffers.draw(ContactRenderLayers.blockMask());
            buffers.draw(ContactRenderLayers.lineMask());
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorter.BY_DISTANCE);
            AdvancedFbo.unbind();
        }
        logDebug(client, camera, reveals.size(), true, true);
    }

    private static BakedModel cachedModel(MinecraftClient client, BlockPos pos, BlockState state) {
        CachedModel cached = MODEL_CACHE.get(pos);
        if (cached != null && cached.state().equals(state)) return cached.model();
        BakedModel model = client.getBlockRenderManager().getModel(state);
        if (MODEL_CACHE.size() >= MAX_MODEL_CACHE && !MODEL_CACHE.containsKey(pos)) {
            BlockPos oldest = MODEL_CACHE.keySet().iterator().next();
            MODEL_CACHE.remove(oldest);
        }
        MODEL_CACHE.put(pos.toImmutable(), new CachedModel(state, model));
        return model;
    }

    private static void drawVoxelFallback(MinecraftClient client, VertexConsumerProvider.Immediate buffers,
                                          MatrixStack matrices, Vec3d camera, RevealedBlock reveal,
                                          BlockState state, float alpha, boolean isOre) {
        BlockPos pos = reveal.pos();
        VoxelShape shape = state.getOutlineShape(client.world, pos, ShapeContext.of(client.player));
        if (shape.isEmpty() && !state.getFluidState().isEmpty()) {
            shape = state.getFluidState().getShape(client.world, pos);
        }
        if (shape.isEmpty()) return;
        if (isOre) {
            // Bright golden color for ore outlines.
            WorldRenderer.drawShapeOutline(matrices, buffers.getBuffer(ContactRenderLayers.lineMask()), shape,
                    pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z,
                    1F, 0.8F, 0F, alpha, false);
        } else {
            WorldRenderer.drawShapeOutline(matrices, buffers.getBuffer(ContactRenderLayers.lineMask()), shape,
                    pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z,
                    reveal.isCenter() ? 1F : 0F, reveal.isCenter() ? 0F : 1F, 0F, alpha, false);
        }
    }

    private static void logFallback(BlockState state, RuntimeException exception) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment() && !BlindnessClient.CONFIG.debugOutlineRendering()) return;
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        if (!LOGGED_FALLBACKS.add(id)) return;
        if (exception == null) {
            BlindnessMod.LOGGER.info("Falling back to voxel outline for {}", id);
        } else {
            BlindnessMod.LOGGER.warn("Falling back to voxel outline for {} after baked-model rendering failed: {}",
                    id, exception.toString());
        }
    }

    private static void logDebug(MinecraftClient client, Camera camera, int active, boolean rendered, boolean depthCopied) {
        if (!BlindnessClient.CONFIG.debugOutlineRendering()) return;
        long now = System.nanoTime();
        if (now - lastDebugNanos < DEBUG_INTERVAL_NANOS) return;
        lastDebugNanos = now;
        AdvancedFbo mask = maskFramebuffer();
        Vec3d pos = camera.getPos();
        BlindnessMod.LOGGER.info(
                "Contact outline debug: perspective={}, active={}, event=after_level_mask, rendered={}, "
                        + "maskFbo={} ({}x{}), camera=({}, {}, {}), submersion={}, modelShader={}, "
                        + "sceneDepthCopied={}, compositePass={}",
                client.options.getPerspective(), active, rendered, mask != null ? mask.getId() : -1,
                mask != null ? mask.getWidth() : 0, mask != null ? mask.getHeight() : 0,
                format(pos.x), format(pos.y), format(pos.z), camera.getSubmersionType(),
                blockMaskShader != null && lineMaskShader != null, depthCopied, BlindnessPostProcessor.isReady());
    }

    static AdvancedFbo maskFramebuffer() {
        try {
            AdvancedFbo fbo = VeilRenderSystem.renderer().getFramebufferManager()
                    .getFramebuffer(MASK_FRAMEBUFFER);
            if (fbo != null && (fbo.getWidth() <= 0 || fbo.getHeight() <= 0)) return null;
            return fbo;
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isMaskFboAvailable() {
        return maskFramebuffer() != null;
    }

    static ShaderProgram blockMaskShader() { return blockMaskShader; }
    static ShaderProgram lineMaskShader() { return lineMaskShader; }

    public static void clearModelCache() {
        MODEL_CACHE.clear();
        LOGGED_FALLBACKS.clear();
        cachedWorld = null;
    }

    private static String format(double value) { return String.format(Locale.ROOT, "%.3f", value); }
    private record CachedModel(BlockState state, BakedModel model) {}

    private record MaskChannelVertexConsumer(VertexConsumer delegate, boolean center, float intensity)
            implements VertexConsumer {
        @Override public VertexConsumer vertex(float x, float y, float z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(center ? 255 : 0, center ? 0 : 255, 0, Math.round(intensity * 255F));
            return this;
        }
        @Override public VertexConsumer texture(float u, float v) { delegate.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
    }

    /**
     * Vertex consumer for ore blocks — uses a bright golden-orange color
     * with pulsating alpha for the mask rendering.
     */
    private record OreMaskVertexConsumer(VertexConsumer delegate, float intensity)
            implements VertexConsumer {
        @Override public VertexConsumer vertex(float x, float y, float z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
            // Bright golden: R=255, G=200, B=0
            delegate.color(255, 200, 0, Math.round(intensity * 255F));
            return this;
        }
        @Override public VertexConsumer texture(float u, float v) { delegate.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
    }
}
