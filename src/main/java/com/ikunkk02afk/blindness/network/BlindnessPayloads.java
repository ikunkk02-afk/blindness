package com.ikunkk02afk.blindness.network;

import com.ikunkk02afk.blindness.BlindnessMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class BlindnessPayloads {
    private BlindnessPayloads() {}

    public record FallStart(int entityId, byte direction, int durationTicks, int seed) implements CustomPayload {
        public static final Id<FallStart> ID = new Id<>(BlindnessMod.id("fall_start"));
        public static final PacketCodec<RegistryByteBuf, FallStart> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, FallStart::entityId,
                PacketCodecs.BYTE, FallStart::direction,
                PacketCodecs.VAR_INT, FallStart::durationTicks,
                PacketCodecs.INTEGER, FallStart::seed,
                FallStart::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FallEnd(int entityId, int getUpTicks) implements CustomPayload {
        public static final Id<FallEnd> ID = new Id<>(BlindnessMod.id("fall_end"));
        public static final PacketCodec<RegistryByteBuf, FallEnd> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, FallEnd::entityId,
                PacketCodecs.VAR_INT, FallEnd::getUpTicks,
                FallEnd::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ScanHit(byte dx, byte dy, byte dz, byte face) {
        public static final PacketCodec<RegistryByteBuf, ScanHit> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, ScanHit::dx,
                PacketCodecs.BYTE, ScanHit::dy,
                PacketCodecs.BYTE, ScanHit::dz,
                PacketCodecs.BYTE, ScanHit::face,
                ScanHit::new);

        public static ScanHit relativeTo(BlockPos origin, BlockPos hit, Direction face) {
            int dx = hit.getX() - origin.getX();
            int dy = hit.getY() - origin.getY();
            int dz = hit.getZ() - origin.getZ();
            if (dx < Byte.MIN_VALUE || dx > Byte.MAX_VALUE || dy < Byte.MIN_VALUE || dy > Byte.MAX_VALUE
                    || dz < Byte.MIN_VALUE || dz > Byte.MAX_VALUE) {
                throw new IllegalArgumentException("Scan hit is outside relative packet range");
            }
            return new ScanHit((byte) dx, (byte) dy, (byte) dz, (byte) face.getId());
        }

        public BlockPos resolve(BlockPos origin) {
            return origin.add(dx, dy, dz);
        }

        public boolean isValid(int maxRelativeDistance) {
            return face >= 0 && face < Direction.values().length
                    && Math.abs((int) dx) <= maxRelativeDistance
                    && Math.abs((int) dy) <= maxRelativeDistance
                    && Math.abs((int) dz) <= maxRelativeDistance;
        }
    }

    public record ScanResult(int scanId, byte mode, BlockPos origin, List<ScanHit> hits) implements CustomPayload {
        public static final int MAX_HITS = 96;
        public static final Id<ScanResult> ID = new Id<>(BlindnessMod.id("scan_result"));
        private static final PacketCodec<RegistryByteBuf, List<ScanHit>> HITS_CODEC = PacketCodecs.collection(
                size -> new ArrayList<>(Math.min(size, MAX_HITS)), ScanHit.CODEC, MAX_HITS);
        public static final PacketCodec<RegistryByteBuf, ScanResult> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ScanResult::scanId,
                PacketCodecs.BYTE, ScanResult::mode,
                BlockPos.PACKET_CODEC, ScanResult::origin,
                HITS_CODEC, ScanResult::hits,
                ScanResult::new);

        public ScanResult {
            hits = List.copyOf(hits.size() > MAX_HITS ? hits.subList(0, MAX_HITS) : hits);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ScanWave(int scanId, BlockPos origin, float maxRadius, int durationTicks, byte mode) implements CustomPayload {
        public static final Id<ScanWave> ID = new Id<>(BlindnessMod.id("scan_wave"));
        public static final PacketCodec<RegistryByteBuf, ScanWave> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ScanWave::scanId,
                BlockPos.PACKET_CODEC, ScanWave::origin,
                PacketCodecs.FLOAT, ScanWave::maxRadius,
                PacketCodecs.VAR_INT, ScanWave::durationTicks,
                PacketCodecs.BYTE, ScanWave::mode,
                ScanWave::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record Animation(int entityId, byte animation, int durationTicks) implements CustomPayload {
        public static final Id<Animation> ID = new Id<>(BlindnessMod.id("animation"));
        public static final PacketCodec<RegistryByteBuf, Animation> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, Animation::entityId,
                PacketCodecs.BYTE, Animation::animation,
                PacketCodecs.VAR_INT, Animation::durationTicks,
                Animation::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record TutorialPrompt() implements CustomPayload {
        public static final Id<TutorialPrompt> ID = new Id<>(BlindnessMod.id("tutorial_prompt"));
        public static final TutorialPrompt INSTANCE = new TutorialPrompt();
        public static final PacketCodec<RegistryByteBuf, TutorialPrompt> CODEC = PacketCodec.unit(INSTANCE);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record TutorialAck() implements CustomPayload {
        public static final Id<TutorialAck> ID = new Id<>(BlindnessMod.id("tutorial_ack"));
        public static final TutorialAck INSTANCE = new TutorialAck();
        public static final PacketCodec<RegistryByteBuf, TutorialAck> CODEC = PacketCodec.unit(INSTANCE);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ConfigRequest() implements CustomPayload {
        public static final Id<ConfigRequest> ID = new Id<>(BlindnessMod.id("config_request"));
        public static final ConfigRequest INSTANCE = new ConfigRequest();
        public static final PacketCodec<RegistryByteBuf, ConfigRequest> CODEC = PacketCodec.unit(INSTANCE);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ServerConfig(int tapRange, int sweepRange, int tapCooldown, int sweepCooldown,
                               int pathTtl, int maxHits) implements CustomPayload {
        public static final Id<ServerConfig> ID = new Id<>(BlindnessMod.id("server_config"));
        public static final PacketCodec<RegistryByteBuf, ServerConfig> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ServerConfig::tapRange,
                PacketCodecs.VAR_INT, ServerConfig::sweepRange,
                PacketCodecs.VAR_INT, ServerConfig::tapCooldown,
                PacketCodecs.VAR_INT, ServerConfig::sweepCooldown,
                PacketCodecs.VAR_INT, ServerConfig::pathTtl,
                PacketCodecs.VAR_INT, ServerConfig::maxHits,
                ServerConfig::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
