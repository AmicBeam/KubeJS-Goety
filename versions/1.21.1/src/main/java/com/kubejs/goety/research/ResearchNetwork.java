package com.kubejs.goety.research;

import com.Polarice3.Goety.utils.SEHelper;
import com.kubejs.goety.KubeJSGoety;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ResearchNetwork {
    private static final String PROTOCOL = "1";

    private ResearchNetwork() {
    }

    public static void init(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, ResearchNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KubeJSGoety.MOD_ID).versioned(PROTOCOL);
        registrar.playToClient(
                SyncDefinitions.TYPE,
                SyncDefinitions.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ResearchData.installClientDefinitions(payload.definitions()))
        );
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new SyncDefinitions(ResearchData.serverDefinitions()));
        // Goety deserializes its capability packet through ResearchList. Send
        // definitions first, then refresh the capability so custom IDs are
        // resolvable on the client even during the initial login handshake.
        SEHelper.sendSEUpdatePacket(player);
    }

    public static void syncAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sync(player);
            }
        }
    }

    public record SyncDefinitions(List<ResearchDefinition> definitions) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncDefinitions> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(KubeJSGoety.MOD_ID, "research")
        );

        public static final StreamCodec<FriendlyByteBuf, SyncDefinitions> STREAM_CODEC = StreamCodec.of(
                (buffer, packet) -> packet.write(buffer),
                SyncDefinitions::decode
        );

        public SyncDefinitions(Collection<ResearchDefinition> definitions) {
            this(List.copyOf(definitions));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(definitions.size());
            for (ResearchDefinition definition : definitions) {
                buffer.writeUtf(definition.id());
                buffer.writeBoolean(definition.scrollItem() != null);
                if (definition.scrollItem() != null) {
                    buffer.writeResourceLocation(definition.scrollItem());
                }
                buffer.writeBoolean(definition.consumeScroll());
                buffer.writeVarInt(definition.prerequisites().size());
                for (String prerequisite : definition.prerequisites()) {
                    buffer.writeUtf(prerequisite);
                }
                buffer.writeUtf(definition.displayName());
                writeNullable(buffer, definition.learnMessage());
                writeNullable(buffer, definition.alreadyLearnedMessage());
                writeNullable(buffer, definition.missingPrerequisiteMessage());
            }
        }

        private static SyncDefinitions decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<ResearchDefinition> definitions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                String id = buffer.readUtf();
                ResourceLocation scroll = buffer.readBoolean() ? buffer.readResourceLocation() : null;
                boolean consume = buffer.readBoolean();
                int prerequisiteCount = buffer.readVarInt();
                List<String> prerequisites = new ArrayList<>(prerequisiteCount);
                for (int j = 0; j < prerequisiteCount; j++) {
                    prerequisites.add(buffer.readUtf());
                }
                definitions.add(new ResearchDefinition(id, scroll, consume, prerequisites, buffer.readUtf(),
                        readNullable(buffer), readNullable(buffer), readNullable(buffer)));
            }
            return new SyncDefinitions(definitions);
        }

        private static void writeNullable(FriendlyByteBuf buffer, String value) {
            buffer.writeBoolean(value != null);
            if (value != null) {
                buffer.writeUtf(value);
            }
        }

        private static String readNullable(FriendlyByteBuf buffer) {
            return buffer.readBoolean() ? buffer.readUtf() : null;
        }
    }
}
