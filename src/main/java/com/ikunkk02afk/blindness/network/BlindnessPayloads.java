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

    public record ServerConfig(int tapCooldown, int sweepCooldown, int pathTtl, int maxEntries) implements CustomPayload {
        public static final Id<ServerConfig> ID = new Id<>(BlindnessMod.id("server_config"));
        public static final PacketCodec<RegistryByteBuf, ServerConfig> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ServerConfig::tapCooldown, PacketCodecs.VAR_INT, ServerConfig::sweepCooldown,
                PacketCodecs.VAR_INT, ServerConfig::pathTtl, PacketCodecs.VAR_INT, ServerConfig::maxEntries,
                ServerConfig::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
