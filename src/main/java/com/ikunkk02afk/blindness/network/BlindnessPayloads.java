package com.ikunkk02afk.blindness.network;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.contact.ContactRevealMath;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class BlindnessPayloads {
    private BlindnessPayloads() {}

    public record FallStart(int entityId, byte direction, int durationTicks, int seed) implements CustomPayload {
        public static final Id<FallStart> ID = new Id<>(BlindnessMod.id("fall_start"));
        public static final PacketCodec<RegistryByteBuf, FallStart> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, FallStart::entityId, PacketCodecs.BYTE, FallStart::direction,
                PacketCodecs.VAR_INT, FallStart::durationTicks, PacketCodecs.INTEGER, FallStart::seed, FallStart::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FallEnd(int entityId, int getUpTicks) implements CustomPayload {
        public static final Id<FallEnd> ID = new Id<>(BlindnessMod.id("fall_end"));
        public static final PacketCodec<RegistryByteBuf, FallEnd> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, FallEnd::entityId, PacketCodecs.VAR_INT, FallEnd::getUpTicks, FallEnd::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ContactEntry(byte dx, byte dy, byte dz, byte flags) {
        private static final int CENTER_FLAG = 1;
        private static final int FACE_SHIFT = 1;
        private static final int FACE_MASK = 0x3F;
        public static final PacketCodec<RegistryByteBuf, ContactEntry> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, ContactEntry::dx, PacketCodecs.BYTE, ContactEntry::dy,
                PacketCodecs.BYTE, ContactEntry::dz, PacketCodecs.BYTE, ContactEntry::flags, ContactEntry::new);

        public static ContactEntry relativeTo(BlockPos center, BlockPos pos, boolean isCenter, int visibleFaces) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            if (Math.abs(dx) > 2 || Math.abs(dy) > 2 || Math.abs(dz) > 2) {
                throw new IllegalArgumentException("Contact reveal is outside the local packet range");
            }
            int packedFlags = (isCenter ? CENTER_FLAG : 0) | ((visibleFaces & FACE_MASK) << FACE_SHIFT);
            return new ContactEntry((byte) dx, (byte) dy, (byte) dz, (byte) packedFlags);
        }

        public BlockPos resolve(BlockPos center) { return center.add(dx, dy, dz); }
        public boolean isCenter() { return (flags & CENTER_FLAG) != 0; }
        public int visibleFaces() { return (Byte.toUnsignedInt(flags) >>> FACE_SHIFT) & FACE_MASK; }
        public boolean isValid() {
            int manhattan = Math.abs((int) dx) + Math.abs((int) dy) + Math.abs((int) dz);
            return manhattan <= 2 && visibleFaces() != 0;
        }
    }

    public record ContactReveal(int sequence, BlockPos center, List<ContactEntry> entries) implements CustomPayload {
        public static final int MAX_ENTRIES = ContactRevealMath.MAX_REVEALS_PER_CONTACT;
        public static final Id<ContactReveal> ID = new Id<>(BlindnessMod.id("contact_reveal"));
        private static final PacketCodec<RegistryByteBuf, List<ContactEntry>> ENTRIES_CODEC = PacketCodecs.collection(
                size -> new ArrayList<>(Math.min(size, MAX_ENTRIES)), ContactEntry.CODEC, MAX_ENTRIES);
        public static final PacketCodec<RegistryByteBuf, ContactReveal> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ContactReveal::sequence, BlockPos.PACKET_CODEC, ContactReveal::center,
                ENTRIES_CODEC, ContactReveal::entries, ContactReveal::new);

        public ContactReveal {
            entries = List.copyOf(entries.size() > MAX_ENTRIES ? entries.subList(0, MAX_ENTRIES) : entries);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ClearContactReveals() implements CustomPayload {
        public static final Id<ClearContactReveals> ID = new Id<>(BlindnessMod.id("clear_contact_reveals"));
        public static final ClearContactReveals INSTANCE = new ClearContactReveals();
        public static final PacketCodec<RegistryByteBuf, ClearContactReveals> CODEC = PacketCodec.unit(INSTANCE);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CliffWarning(byte risk) implements CustomPayload {
        public static final Id<CliffWarning> ID = new Id<>(BlindnessMod.id("cliff_warning"));
        public static final PacketCodec<RegistryByteBuf, CliffWarning> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, CliffWarning::risk, CliffWarning::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SoundRevealEntry(byte dx, byte dy, byte dz, byte visibleFaces) {
        private static final int FACE_MASK = 0x3F;
        public static final PacketCodec<RegistryByteBuf, SoundRevealEntry> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, SoundRevealEntry::dx, PacketCodecs.BYTE, SoundRevealEntry::dy,
                PacketCodecs.BYTE, SoundRevealEntry::dz, PacketCodecs.BYTE, SoundRevealEntry::visibleFaces,
                SoundRevealEntry::new);

        public static SoundRevealEntry relativeTo(BlockPos center, BlockPos pos, int faces) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            if (Math.abs(dx) > 4 || Math.abs(dy) > 2 || Math.abs(dz) > 4) {
                throw new IllegalArgumentException("Entity sound reveal is outside the local packet range");
            }
            return new SoundRevealEntry((byte) dx, (byte) dy, (byte) dz, (byte) (faces & FACE_MASK));
        }

        public BlockPos resolve(BlockPos center) { return center.add(dx, dy, dz); }
        public int faces() { return Byte.toUnsignedInt(visibleFaces) & FACE_MASK; }
        public boolean isValid() {
            return Math.abs((int) dx) <= 4 && Math.abs((int) dy) <= 2 && Math.abs((int) dz) <= 4
                    && dx * dx + dz * dz <= 16 && faces() != 0;
        }
    }

    public record SoundEchoPosition(double x, double y, double z) {
        public static final PacketCodec<RegistryByteBuf, SoundEchoPosition> CODEC = PacketCodec.tuple(
                PacketCodecs.DOUBLE, SoundEchoPosition::x, PacketCodecs.DOUBLE, SoundEchoPosition::y,
                PacketCodecs.DOUBLE, SoundEchoPosition::z, SoundEchoPosition::new);
        public boolean isValid() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    }

    public record SoundEchoMetadata(byte category, float strength, boolean hostile, byte occlusion, long serverGameTime) {
        public static final PacketCodec<RegistryByteBuf, SoundEchoMetadata> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, SoundEchoMetadata::category, PacketCodecs.FLOAT, SoundEchoMetadata::strength,
                PacketCodecs.BOOL, SoundEchoMetadata::hostile, PacketCodecs.BYTE, SoundEchoMetadata::occlusion,
                PacketCodecs.VAR_LONG, SoundEchoMetadata::serverGameTime, SoundEchoMetadata::new);
        public boolean isValid() {
            return category >= 0 && category < com.ikunkk02afk.blindness.awareness.EntitySoundCategory.values().length
                    && Float.isFinite(strength) && strength >= 0F && strength <= 1F
                    && occlusion >= 0 && occlusion < com.ikunkk02afk.blindness.awareness.SoundOcclusion.values().length;
        }
    }

    public record EntitySoundEcho(SoundEchoPosition position, SoundEchoMetadata metadata, BlockPos blockCenter,
                                  List<SoundRevealEntry> entries) implements CustomPayload {
        public static final int MAX_ENTRIES = 80;
        public static final Id<EntitySoundEcho> ID = new Id<>(BlindnessMod.id("entity_sound_echo"));
        private static final PacketCodec<RegistryByteBuf, List<SoundRevealEntry>> ENTRIES_CODEC = PacketCodecs.collection(
                size -> new ArrayList<>(Math.min(size, MAX_ENTRIES)), SoundRevealEntry.CODEC, MAX_ENTRIES);
        public static final PacketCodec<RegistryByteBuf, EntitySoundEcho> CODEC = PacketCodec.tuple(
                SoundEchoPosition.CODEC, EntitySoundEcho::position, SoundEchoMetadata.CODEC, EntitySoundEcho::metadata,
                BlockPos.PACKET_CODEC, EntitySoundEcho::blockCenter, ENTRIES_CODEC, EntitySoundEcho::entries,
                EntitySoundEcho::new);

        public EntitySoundEcho {
            entries = List.copyOf(entries.size() > MAX_ENTRIES ? entries.subList(0, MAX_ENTRIES) : entries);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record HostileWarning(byte volumePercent, boolean critical) implements CustomPayload {
        public static final Id<HostileWarning> ID = new Id<>(BlindnessMod.id("hostile_warning"));
        public static final PacketCodec<RegistryByteBuf, HostileWarning> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, HostileWarning::volumePercent, PacketCodecs.BOOL, HostileWarning::critical,
                HostileWarning::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record Animation(int entityId, byte animation, int durationTicks) implements CustomPayload {
        public static final Id<Animation> ID = new Id<>(BlindnessMod.id("animation"));
        public static final PacketCodec<RegistryByteBuf, Animation> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, Animation::entityId, PacketCodecs.BYTE, Animation::animation,
                PacketCodecs.VAR_INT, Animation::durationTicks, Animation::new);
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

    public record UpdateSoundAwarenessSettings(int listeningChunkRadius, int blockRevealRadius) implements CustomPayload {
        public static final Id<UpdateSoundAwarenessSettings> ID = new Id<>(BlindnessMod.id("update_sound_awareness_settings"));
        public static final PacketCodec<RegistryByteBuf, UpdateSoundAwarenessSettings> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, UpdateSoundAwarenessSettings::listeningChunkRadius,
                PacketCodecs.VAR_INT, UpdateSoundAwarenessSettings::blockRevealRadius,
                UpdateSoundAwarenessSettings::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SoundAwarenessSettings(int listeningChunkRadius, int blockRevealRadius) implements CustomPayload {
        public static final Id<SoundAwarenessSettings> ID = new Id<>(BlindnessMod.id("sound_awareness_settings"));
        public static final PacketCodec<RegistryByteBuf, SoundAwarenessSettings> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, SoundAwarenessSettings::listeningChunkRadius,
                PacketCodecs.VAR_INT, SoundAwarenessSettings::blockRevealRadius,
                SoundAwarenessSettings::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ServerConfig(int tapCooldown, int sweepCooldown, int pathTtl, int maxEntries) implements CustomPayload {
        public static final Id<ServerConfig> ID = new Id<>(BlindnessMod.id("server_config"));
        public static final PacketCodec<RegistryByteBuf, ServerConfig> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ServerConfig::tapCooldown, PacketCodecs.VAR_INT, ServerConfig::sweepCooldown,
                PacketCodecs.VAR_INT, ServerConfig::pathTtl, PacketCodecs.VAR_INT, ServerConfig::maxEntries,
                ServerConfig::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
