package com.ikunkk02afk.blindness.client.render;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;

import java.util.OptionalDouble;

final class ContactRenderLayers extends RenderPhase {
    private static final Target MASK_TARGET = new Target("blindness_contact_mask_target", () -> {
        AdvancedFbo mask = ContactOutlineRenderer.maskFramebuffer();
        if (mask != null) mask.bindDraw(true);
    }, AdvancedFbo::unbindDraw);
    private static final ShaderProgram BLOCK_MASK_PROGRAM = new ShaderProgram(ContactOutlineRenderer::blockMaskShader);
    private static final ShaderProgram LINE_MASK_PROGRAM = new ShaderProgram(ContactOutlineRenderer::lineMaskShader);
    private static final RenderLayer BLOCK_MASK = RenderLayer.of("blindness_contact_model_mask",
            VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 786432,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(BLOCK_MASK_PROGRAM)
                    .texture(new Texture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, false, false))
                    .layering(VIEW_OFFSET_Z_LAYERING)
                    .depthTest(LEQUAL_DEPTH_TEST)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .target(MASK_TARGET)
                    .build(false));
    private static final RenderLayer LINE_MASK = RenderLayer.of("blindness_contact_voxel_mask",
            VertexFormats.LINES, VertexFormat.DrawMode.LINES, 1536,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(LINE_MASK_PROGRAM)
                    .lineWidth(new LineWidth(OptionalDouble.of(1.0)))
                    .layering(VIEW_OFFSET_Z_LAYERING)
                    .depthTest(LEQUAL_DEPTH_TEST)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .target(MASK_TARGET)
                    .build(false));

    private ContactRenderLayers() { super("blindness_contact_render_layers", () -> {}, () -> {}); }
    static RenderLayer blockMask() { return BLOCK_MASK; }
    static RenderLayer lineMask() { return LINE_MASK; }
}
