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

    @Test void veilPipelineUsesDepthAndNormalWithDepthFallback() throws Exception {
        String shader = Files.readString(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/shaders/program/blindness.fsh"));
        String full = Files.readString(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/post/blindness.json"));
        assertTrue(shader.contains("DiffuseDepthSampler"));
        assertTrue(shader.contains("VeilDynamicNormal"));
        assertTrue(full.contains("\"normal\""));
        assertTrue(Files.exists(ROOT.resolve("src/client/resources/assets/blindness/pinwheel/post/blindness_depth.json")));
    }

    @Test void readmeContainsRequiredDisclaimer() throws Exception {
        String readme = Files.readString(ROOT.resolve("README.md"));
        assertTrue(readme.contains("本模组以游戏化方式模拟部分重度视力障碍者可能使用的环境感知方式，不代表所有盲人或视障人士的真实体验。"));
    }
}
