package com.ikunkk02afk.blindness.fall;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.ikunkk02afk.blindness.network.BlindnessNetworking;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

import java.util.Comparator;
import java.util.List;

public final class FallStateManager {
    private static final RegistryKey<DamageType> TRIP_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, BlindnessMod.id("trip"));
    private static final int FALL_TICKS = 18;
    private static final int GET_UP_TICKS = 10;

    private FallStateManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tick(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            BlindnessRuntime.clear(oldPlayer.getUuid());
            BlindnessRuntime.get(newPlayer).resetTransient(newPlayer.getWorld().getRegistryKey());
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) BlindnessRuntime.clear(player.getUuid());
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> locked(player) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> locked(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> locked(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> locked(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) -> locked(player)
                ? TypedActionResult.fail(player.getStackInHand(hand)) : TypedActionResult.pass(player.getStackInHand(hand)));
    }

    public static boolean isControlLocked(ServerPlayerEntity player) {
        return BlindnessRuntime.get(player).isFalling(player.getServerWorld().getTime());
    }

    private static boolean locked(net.minecraft.entity.player.PlayerEntity player) {
        return player instanceof ServerPlayerEntity serverPlayer && isControlLocked(serverPlayer);
    }

    private static void tick(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        PlayerRuntimeState runtime = BlindnessRuntime.get(player);
        BlindnessRuntime.prune(runtime, now);

        if (runtime.controlsUnlockAt > 0) {
            player.setSprinting(false);
            player.setVelocity(player.getVelocity().multiply(0.15, 1.0, 0.15));
            if (!runtime.getUpSent && now >= runtime.fallEndsAt) {
                runtime.getUpSent = true;
                BlindnessNetworking.sendToTrackingAndSelf(player,
                        new BlindnessPayloads.FallEnd(player.getId(), GET_UP_TICKS));
                BlindnessNetworking.sendToTrackingAndSelf(player,
                        new BlindnessPayloads.Animation(player.getId(), (byte) 2, GET_UP_TICKS));
            }
            if (now >= runtime.controlsUnlockAt) {
                runtime.controlsUnlockAt = 0;
                runtime.fallEndsAt = 0;
                runtime.protectedUntil = now + BlindnessMod.serverConfig().protectionCooldownTicks();
                runtime.getUpSent = false;
            }
            return;
        }

        if ((now & 1L) != 0L || !BlindnessComponents.PLAYER.get(player).blindnessEnabled() || excluded(player, now, runtime)) {
            runtime.hazardFingerprint = Long.MIN_VALUE;
            rememberVelocity(player, runtime);
            return;
        }

        Hazard hazard = detectHazard(player, runtime);
        if (hazard.baseRisk <= 0.0) {
            runtime.hazardFingerprint = Long.MIN_VALUE;
            recoverBalance(runtime, now);
            rememberVelocity(player, runtime);
            return;
        }

        runtime.balance = Math.max(0.0, runtime.balance - hazard.balanceCost);
        if (runtime.hazardFingerprint == hazard.fingerprint) {
            rememberVelocity(player, runtime);
            return;
        }
        runtime.hazardFingerprint = hazard.fingerprint;
        double speed = horizontalSpeed(player.getVelocity());
        boolean scanned = BlindnessRuntime.isScanned(player, hazard.pos, now);
        double probability = TripRiskCalculator.probability(new TripRiskCalculator.Inputs(
                hazard.baseRisk, speed, player.isSprinting(), hazard.materialFactor,
                runtime.balance, scanned, player.isSneaking(), BlindnessMod.serverConfig().tripRiskScale()));
        if (player.getRandom().nextDouble() < probability) beginFall(player, runtime, hazard, now);
        rememberVelocity(player, runtime);
    }

    private static boolean excluded(ServerPlayerEntity player, long now, PlayerRuntimeState runtime) {
        return now < runtime.protectedUntil || player.isSpectator() || player.getAbilities().flying
                || player.isFallFlying() || player.isSwimming() || player.hasVehicle()
                || horizontalSpeed(player.getVelocity()) <= 0.06;
    }

    private static Hazard detectHazard(ServerPlayerEntity player, PlayerRuntimeState runtime) {
        Vec3d velocity = player.getVelocity();
        double speed = horizontalSpeed(velocity);
        Vec3d forward = speed > 0 ? new Vec3d(velocity.x / speed, 0, velocity.z / speed) : player.getRotationVec(1).multiply(1, 0, 1).normalize();
        BlockPos feet = player.getBlockPos();
        BlockPos probe = BlockPos.ofFloored(player.getPos().add(forward.multiply(0.8)));
        BlockState under = player.getWorld().getBlockState(probe.down());
        BlockState atFeet = player.getWorld().getBlockState(probe);
        BlockState above = player.getWorld().getBlockState(probe.up());
        double material = materialFactor(under, velocity, runtime.lastHorizontalVelocity);

        if (player.horizontalCollision && speed > 0.12) return new Hazard(0.55, probe, fingerprint(5, probe), 15, material, (byte) 0);
        if (!atFeet.getCollisionShape(player.getWorld(), probe).isEmpty() && above.getCollisionShape(player.getWorld(), probe.up()).isEmpty()) {
            return new Hazard(0.20, probe, fingerprint(2, probe), 8, material, (byte) 0);
        }
        if (under.getCollisionShape(player.getWorld(), probe.down()).isEmpty()) {
            boolean secondDownEmpty = player.getWorld().getBlockState(probe.down(2)).getCollisionShape(player.getWorld(), probe.down(2)).isEmpty();
            double risk = secondDownEmpty ? 0.35 : 0.14;
            return new Hazard(risk, probe.down(), fingerprint(secondDownEmpty ? 4 : 3, probe), secondDownEmpty ? 12 : 7, material, (byte) 0);
        }
        if (under.getBlock() instanceof CarpetBlock || under.getBlock() instanceof SlabBlock || under.getBlock() instanceof StairsBlock) {
            return new Hazard(0.02, probe.down(), fingerprint(1, probe), 2, material, (byte) 0);
        }

        List<Entity> entities = player.getWorld().getOtherEntities(player,
                player.getBoundingBox().stretch(forward.multiply(1.2)).expand(0.2),
                entity -> entity instanceof LivingEntity && entity.isAlive());
        Entity nearest = entities.stream().min(Comparator.comparingDouble(player::squaredDistanceTo)).orElse(null);
        if (nearest != null) {
            double area = nearest.getWidth() * nearest.getHeight();
            double base = area < 0.55 ? 0.10 : area < 1.6 ? 0.22 : 0.38;
            Vec3d relative = nearest.getVelocity().subtract(velocity);
            double headOn = Math.min(0.12, Math.max(0, -relative.dotProduct(forward)) * 0.2);
            return new Hazard(base + headOn, nearest.getBlockPos(), ((long) 6 << 56) ^ nearest.getId(), 10, material, (byte) 1);
        }
        return Hazard.NONE;
    }

    private static double materialFactor(BlockState state, Vec3d velocity, Vec3d previous) {
        boolean ice = state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.BLUE_ICE) || state.isOf(Blocks.FROSTED_ICE);
        if (!ice || horizontalSpeed(velocity) < 0.08 || horizontalSpeed(previous) < 0.08) return 1.0;
        double dot = new Vec3d(velocity.x, 0, velocity.z).normalize().dotProduct(new Vec3d(previous.x, 0, previous.z).normalize());
        return dot < 0.65 ? 1.4 : 1.0;
    }

    private static void beginFall(ServerPlayerEntity player, PlayerRuntimeState runtime, Hazard hazard, long now) {
        runtime.fallEndsAt = now + FALL_TICKS;
        runtime.controlsUnlockAt = now + FALL_TICKS + GET_UP_TICKS;
        runtime.tinnitusUntil = hazard.baseRisk >= 0.5 ? now + 12 : 0;
        runtime.getUpSent = false;
        player.setSprinting(false);
        player.setVelocity(player.getVelocity().multiply(0.15, 1.0, 0.15));
        BlindnessComponents.PLAYER.get(player).incrementFalls();
        BlindnessNetworking.sendToTrackingAndSelf(player,
                new BlindnessPayloads.FallStart(player.getId(), hazard.direction, FALL_TICKS + GET_UP_TICKS, player.getRandom().nextInt()));
        BlindnessNetworking.sendToTrackingAndSelf(player,
                new BlindnessPayloads.Animation(player.getId(), hazard.direction == 0 ? (byte) 0 : (byte) 1, FALL_TICKS));
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT,
                SoundCategory.PLAYERS, 0.7F, 0.75F);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(),
                SoundCategory.PLAYERS, 0.45F, 0.65F);
        applyTripDamage(player);
    }

    private static void applyTripDamage(ServerPlayerEntity player) {
        BlockState ground = player.getWorld().getBlockState(player.getBlockPos().down());
        float damage = 0F;
        if (ground.isOf(Blocks.GRASS_BLOCK) || ground.isOf(Blocks.SNOW_BLOCK) || ground.isOf(Blocks.WHITE_WOOL)) return;
        if (ground.isOf(Blocks.DIRT) || ground.isOf(Blocks.SAND) || ground.getSoundGroup() == net.minecraft.sound.BlockSoundGroup.WOOD) {
            if (player.getRandom().nextFloat() < 0.25F) damage = 0.5F;
        } else if (ground.getSoundGroup() == net.minecraft.sound.BlockSoundGroup.STONE || ground.getSoundGroup() == net.minecraft.sound.BlockSoundGroup.METAL) {
            damage = 1.0F;
        }
        damage = TripDamageRules.cap(damage, BlindnessMod.serverConfig().maximumTripDamage());
        if (damage > 0) {
            var type = player.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(TRIP_DAMAGE);
            player.damage(new DamageSource(type), damage);
        }
    }

    private static void recoverBalance(PlayerRuntimeState runtime, long now) {
        if (now - runtime.lastStableRecoveryAt >= 5) {
            runtime.balance = Math.min(100.0, runtime.balance + 1.0);
            runtime.lastStableRecoveryAt = now;
        }
    }

    private static void rememberVelocity(ServerPlayerEntity player, PlayerRuntimeState runtime) {
        Vec3d velocity = player.getVelocity();
        runtime.lastHorizontalVelocity = new Vec3d(velocity.x, 0, velocity.z);
    }

    private static double horizontalSpeed(Vec3d velocity) { return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z); }
    private static long fingerprint(int type, BlockPos pos) { return ((long) type << 56) ^ pos.asLong(); }

    private record Hazard(double baseRisk, BlockPos pos, long fingerprint, double balanceCost, double materialFactor, byte direction) {
        private static final Hazard NONE = new Hazard(0, BlockPos.ORIGIN, 0, 0, 1, (byte) 0);
    }
}
