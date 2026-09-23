package pro.komaru.tridot.common.networking.packets;

import net.minecraft.network.*;
import net.minecraft.network.protocol.common.custom.*;
import net.neoforged.api.distmarker.*;
import net.neoforged.neoforge.network.handling.*;

/**
 * Base for server-to-client payloads. PORT NOTE: subclasses now also implement {@link #type()} and register a
 * {@code StreamCodec} built from {@link #encode} and their {@code decode}; {@link #handle(IPayloadContext)} is the handler.
 */
public abstract class ClientPacket implements CustomPacketPayload{
    public void encode(FriendlyByteBuf buf){
    }

    public final void handle(IPayloadContext context){
        context.enqueueWork(() -> {
            if(context.flow().isClientbound()){
                ClientOnly.clientData(this, context);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    public void execute(IPayloadContext context){
    }

    public static class ClientOnly{
        public static void clientData(ClientPacket packet, IPayloadContext context){
            packet.execute(context);
        }
    }
}
