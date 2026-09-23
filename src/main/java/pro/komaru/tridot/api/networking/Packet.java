package pro.komaru.tridot.api.networking;

import net.minecraft.network.*;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.neoforged.api.distmarker.*;
import net.neoforged.fml.loading.*;
import net.neoforged.neoforge.network.handling.*;

import java.util.function.*;

/**
 * PORT NOTE: SimpleChannel messages are now {@link CustomPacketPayload}s. Implementations keep {@link #save} and the
 * {@code (FriendlyByteBuf)} constructor and additionally declare a {@code TYPE} (via {@link #type(String, String)})
 * and a {@code STREAM_CODEC} (via {@link #codec(Function)}); {@link #handle(IPayloadContext)} is the payload handler.
 */
public interface Packet extends CustomPacketPayload {
    default void save(FriendlyByteBuf buf) {

    }

    /** Server-side handling. {@code sender} is the sending player when received on the server, null on the client. */
    default void handle(IPayloadContext ctx, ServerPlayer sender) {

    }

    default void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if(FMLEnvironment.dist.isClient()) doOnClient();
            handle(ctx, ctx.player() instanceof ServerPlayer sp ? sp : null);
        });
    }

    @OnlyIn(Dist.CLIENT)
    default void doOnClient() {

    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String modId, String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(modId, path));
    }

    /** Builds a stream codec from {@link #save} and a buffer-reading constructor. */
    static <T extends Packet> StreamCodec<RegistryFriendlyByteBuf, T> codec(Function<FriendlyByteBuf, T> reader) {
        return StreamCodec.of((buf, packet) -> packet.save(buf), reader::apply);
    }
}
