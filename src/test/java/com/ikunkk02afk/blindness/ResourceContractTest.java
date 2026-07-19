package com.ikunkk02afk.blindness;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ResourceContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test void metadataContainsNoTemplateIdentityAndKeepsClientSplit() throws Exception {
        String json = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));
        assertTrue(json.contains("\"id\": \"blindness\""));
        assertTrue(json.contains("com.ikunkk02afk.blindness.client.BlindnessClient"));
        assertTrue(json.contains("\"cardinal-components\": [\"blindness:player\"]"));
        assertFalse(json.contains("blindness_mod"));
        assertFalse(json.contains("CC0"));
    }

    @Test void guidanceCaneHasRealSizedTextures() throws Exception {
        var item = ImageIO.read(ROOT.resolve("src/main/resources/assets/blindness/textures/item/guidance_cane.png").toFile());
        var icon = ImageIO.read(ROOT.resolve("src/main/resources/assets/blindness/icon.png").toFile());
        assertEquals(16, item.getWidth()); assertEquals(16, item.getHeight());
        assertEquals(64, icon.getWidth()); assertEquals(64, icon.getHeight());
        assertTrue(item.getColorModel().hasAlpha());
    }

    @Test void veilPipelineUsesDetailedAlphaMaskAndContainsNoWaveUniforms() throws Exception {
        String mask = Files.readString(ROOT.resolve("src/client/resources/assets/blindness/shaders/core/contact_mask_block.fsh"));
        String edge = Files.readString(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/shaders/program/contact_edge.fsh"));
        String horizontal = Files.readString(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/shaders/program/contact_dilate_horizontal.fsh"));
        String composite = Files.readString(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/shaders/program/contact_composite.fsh"));
        String full = Files.readString(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/post/blindness.json"));
        assertTrue(mask.contains("textureAlpha < 0.1"));
        assertTrue(edge.contains("ContactMaskSampler"));
        assertTrue(edge.contains("DiffuseDepthSampler"));
        assertTrue(edge.contains("VeilDynamicNormal"));
        assertTrue(horizontal.contains("CenterGlowRadius"));
        assertTrue(composite.contains("vec3 outputColor"));
        assertTrue(full.contains("contact_edges"));
        assertTrue(full.contains("contact_horizontal"));
        assertFalse(edge.contains("ScanOrigin"));
        assertFalse(composite.contains("ScanRadius"));
        assertFalse(composite.contains("ScanProgress"));
        assertTrue(full.contains("\"normal\""));
        assertTrue(Files.exists(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/framebuffers/contact_mask.json")));
        assertTrue(Files.exists(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/post/blindness_depth.json")));
    }

    @Test void readmeContainsRequiredDisclaimer() throws Exception {
        String readme = Files.readString(ROOT.resolve("README.md"));
        assertTrue(readme.contains("不代表所有盲人或视障人士的真实体验"));
        assertTrue(readme.contains("不是安全级反作弊"));
        assertTrue(readme.contains("JEI、REI、EMI"));
    }

    @Test void compatibilityMixinsStayClientOnlyAndNoBreaksAreDeclared() throws Exception {
        String metadata = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));
        String clientMixins = Files.readString(ROOT.resolve("src/client/resources/blindness.client.mixins.json"));
        String commonMixins = Files.readString(ROOT.resolve("src/main/resources/blindness.mixins.json"));
        assertFalse(metadata.contains("\"breaks\""));
        assertTrue(clientMixins.contains("ConnectScreenMixin"));
        assertTrue(clientMixins.contains("SubtitlesHudMixin"));
        assertFalse(commonMixins.contains("ConnectScreenMixin"));
    }
}
