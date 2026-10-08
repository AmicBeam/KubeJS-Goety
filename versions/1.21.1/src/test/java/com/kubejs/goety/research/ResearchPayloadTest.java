package com.kubejs.goety.research;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Exercises the exact client payload codec without a network connection. */
public final class ResearchPayloadTest {
    public static void main(String[] args) {
        List<ResearchDefinition> definitions = List.of(
                new ResearchDefinition("ancient", ResourceLocation.parse("kubejs:ancient_scroll"), true,
                        List.of("haunting", "forbidden"), "远古研究", "learned", null, "missing"),
                new ResearchDefinition("advanced", null, false, List.of("ancient"), "Advanced", null, "already", null)
        );
        var original = new ResearchNetwork.SyncDefinitions(definitions);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ResearchNetwork.SyncDefinitions.STREAM_CODEC.encode(buffer, original);
            var decoded = ResearchNetwork.SyncDefinitions.STREAM_CODEC.decode(buffer);
            if (!original.equals(decoded) || buffer.isReadable()) {
                throw new AssertionError("Research payload did not preserve fields or consume its exact byte sequence");
            }
            if (!decoded.type().id().equals(ResourceLocation.parse("kubejs_goety:research"))) {
                throw new AssertionError("Unexpected research payload type");
            }
        } finally {
            buffer.release();
        }
        System.out.println("Research payload codec round-trip passed");
    }
}
