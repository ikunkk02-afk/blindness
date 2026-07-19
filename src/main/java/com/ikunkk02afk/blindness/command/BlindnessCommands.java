package com.ikunkk02afk.blindness.command;

import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class BlindnessCommands {
    private BlindnessCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("blindness")
                .then(literal("enable").executes(context -> setEnabled(context.getSource(), true)))
                .then(literal("disable").executes(context -> setEnabled(context.getSource(), false)))
                .then(literal("status").executes(context -> status(context.getSource())))
                .then(literal("reset").requires(source -> source.hasPermissionLevel(2)
                                || (source.getEntity() instanceof ServerPlayerEntity player && player.isCreative()))
                        .executes(context -> reset(context.getSource()))));
    }

    private static int setEnabled(ServerCommandSource source, boolean enabled) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        BlindnessComponents.PLAYER.get(player).setBlindnessEnabled(enabled);
        if (!enabled) {
            BlindnessRuntime.get(player).resetTransient(player.getWorld().getRegistryKey());
            ServerPlayNetworking.send(player, BlindnessPayloads.ClearContactReveals.INSTANCE);
        }
        source.sendFeedback(() -> Text.translatable(enabled
                ? "commands.blindness.enable.success" : "commands.blindness.disable.success"), false);
        return 1;
    }

    private static int status(ServerCommandSource source) throws CommandSyntaxException {
        var component = BlindnessComponents.PLAYER.get(source.getPlayerOrThrow());
        source.sendFeedback(() -> Text.translatable("commands.blindness.status",
                Text.translatable(component.blindnessEnabled() ? "blindness.status.enabled" : "blindness.status.disabled"),
                component.visualMode(), component.caneProficiency(), component.totalFalls()), false);
        return component.blindnessEnabled() ? 1 : 0;
    }

    private static int reset(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        BlindnessComponents.PLAYER.get(player).reset();
        BlindnessRuntime.get(player).resetTransient(player.getWorld().getRegistryKey());
        ServerPlayNetworking.send(player, BlindnessPayloads.ClearContactReveals.INSTANCE);
        source.sendFeedback(() -> Text.translatable("commands.blindness.reset.success"), false);
        return 1;
    }
}
