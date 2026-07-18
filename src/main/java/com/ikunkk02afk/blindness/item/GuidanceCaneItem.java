package com.ikunkk02afk.blindness.item;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import com.ikunkk02afk.blindness.scan.CaneScanService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class GuidanceCaneItem extends Item {
    private static final int SWEEP_TICKS = 20;
    private static final Identifier SLOW_MODIFIER_ID = BlindnessMod.id("cane_sweep_slowdown");
    private static final EntityAttributeModifier SLOW_MODIFIER = new EntityAttributeModifier(
            SLOW_MODIFIER_ID, -0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public GuidanceCaneItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            BlindnessRuntime.get(serverPlayer).sweepCompleted = false;
            removeSlowdown(serverPlayer);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) return;
        int elapsed = getMaxUseTime(stack, user) - remainingUseTicks;
        PlayerRuntimeState state = BlindnessRuntime.get(player);
        if (elapsed > 2 && elapsed < SWEEP_TICKS) addSlowdown(player);
        if (player.isSprinting()) {
            removeSlowdown(player);
            player.stopUsingItem();
            return;
        }
        if (elapsed >= SWEEP_TICKS && !state.sweepCompleted) {
            state.sweepCompleted = true;
            removeSlowdown(player);
            CaneScanService.performSweep(player);
            player.getItemCooldownManager().set(this, BlindnessMod.serverConfig().sweepCooldownTicks());
            player.stopUsingItem();
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) return;
        removeSlowdown(player);
        PlayerRuntimeState state = BlindnessRuntime.get(player);
        int elapsed = getMaxUseTime(stack, user) - remainingUseTicks;
        if (!state.sweepCompleted && elapsed < SWEEP_TICKS && !player.isSprinting()) {
            CaneScanService.performTap(player);
            player.getItemCooldownManager().set(this, BlindnessMod.serverConfig().tapCooldownTicks());
        }
        state.sweepCompleted = false;
    }

    @Override public int getMaxUseTime(ItemStack stack, LivingEntity user) { return 72000; }
    @Override public UseAction getUseAction(ItemStack stack) { return UseAction.NONE; }

    private static void addSlowdown(ServerPlayerEntity player) {
        var instance = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instance != null && !instance.hasModifier(SLOW_MODIFIER_ID)) instance.addTemporaryModifier(SLOW_MODIFIER);
    }

    public static void removeSlowdown(ServerPlayerEntity player) {
        var instance = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instance != null) instance.removeModifier(SLOW_MODIFIER_ID);
    }
}
