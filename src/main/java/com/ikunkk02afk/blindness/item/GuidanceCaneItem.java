package com.ikunkk02afk.blindness.item;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import com.ikunkk02afk.blindness.contact.CaneContactService;
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
    private static final int SWEEP_START_TICK = 5;
    private static final int SWEEP_END_TICK = 20;
    private static final int[] SWEEP_CONTACT_TICKS = {6, 10, 14, 18};
    private static final float[] SWEEP_YAW_OFFSETS = {-24F, -8F, 8F, 24F};
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
            PlayerRuntimeState state = BlindnessRuntime.get(serverPlayer);
            state.caneSweepStarted = false;
            state.caneSweepFinished = false;
            state.nextSweepContact = 0;
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
        if (player.isSprinting()) {
            removeSlowdown(player);
            player.stopUsingItem();
            return;
        }
        if (elapsed >= SWEEP_START_TICK && !state.caneSweepStarted) {
            state.caneSweepStarted = true;
            addSlowdown(player);
            CaneContactService.playSweepAnimation(player);
        }
        while (state.caneSweepStarted && state.nextSweepContact < SWEEP_CONTACT_TICKS.length
                && elapsed >= SWEEP_CONTACT_TICKS[state.nextSweepContact]) {
            CaneContactService.performContact(player, SWEEP_YAW_OFFSETS[state.nextSweepContact], true);
            state.nextSweepContact++;
        }
        if (elapsed >= SWEEP_END_TICK && !state.caneSweepFinished) {
            state.caneSweepFinished = true;
            removeSlowdown(player);
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
        if (!state.caneSweepStarted && elapsed < SWEEP_START_TICK && !player.isSprinting()) {
            CaneContactService.playTapAnimation(player);
            CaneContactService.performContact(player, 0F, false);
            player.getItemCooldownManager().set(this, BlindnessMod.serverConfig().tapCooldownTicks());
        } else if (state.caneSweepStarted && !state.caneSweepFinished) {
            player.getItemCooldownManager().set(this, BlindnessMod.serverConfig().sweepCooldownTicks());
        }
        state.caneSweepStarted = false;
        state.caneSweepFinished = false;
        state.nextSweepContact = 0;
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
