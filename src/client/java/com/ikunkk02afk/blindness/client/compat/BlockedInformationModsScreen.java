package com.ikunkk02afk.blindness.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.file.Path;

public final class BlockedInformationModsScreen extends Screen {
    public BlockedInformationModsScreen() {
        super(Text.translatable("screen.blindness.blocked.title"));
    }

    @Override
    protected void init() {
        int buttonY = Math.min(height - 28, 170 + BlockedInformationMods.detected().size() * 12);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.blindness.blocked.return"),
                button -> client.setScreen(new TitleScreen())).dimensions(width / 2 - 154, buttonY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.blindness.blocked.open_mods"),
                button -> {
                    Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
                    Util.getOperatingSystem().open(mods);
                }).dimensions(width / 2 - 50, buttonY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.blindness.blocked.copy"),
                button -> client.keyboard.setClipboard(BlockedInformationMods.report()))
                .dimensions(width / 2 + 54, buttonY, 100, 20).build());
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 24, 0xFFFF7777);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.blindness.blocked.body_1"), width / 2, 48, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.blindness.blocked.body_2"), width / 2, 61, 0xFFFFFFFF);
        int y = 84;
        for (BlockedInformationMods.BlockedMod mod : BlockedInformationMods.detected()) {
            Text row = Text.translatable("screen.blindness.blocked.row", mod.name(), mod.id(), mod.version(),
                    Text.translatable(mod.category().translationKey()));
            context.drawCenteredTextWithShadow(textRenderer, row, width / 2, y, 0xFFE0E0E0);
            y += 12;
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
