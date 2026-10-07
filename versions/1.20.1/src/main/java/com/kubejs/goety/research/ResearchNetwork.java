package com.kubejs.goety.research;

import com.Polarice3.Goety.utils.SEHelper;
import com.kubejs.goety.KubeJSGoety;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class ResearchNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KubeJSGoety.MOD_ID, "research"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ResearchNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(0, SyncDefinitions.class, SyncDefinitions::encode, SyncDefinitions::decode,
                SyncDefinitions::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncDefinitions(ResearchData.serverDefinitions()));
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

    private record SyncDefinitions(List<ResearchDefinition> definitions) {
        private SyncDefinitions(Collection<ResearchDefinition> definitions) {
            this(List.copyOf(definitions));
        }

        private static void encode(SyncDefinitions packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.definitions.size());
            for (ResearchDefinition definition : packet.definitions) {
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

        private static void handle(SyncDefinitions packet,
                                   java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
            net.minecraftforge.network.NetworkEvent.Context networkContext = context.get();
            networkContext.enqueueWork(() -> ResearchData.installClientDefinitions(packet.definitions));
            networkContext.setPacketHandled(true);
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
