package pro.komaru.tridot.mixin.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.komaru.tridot.client.gfx.text.DotStyle;

/**
 * PORT NOTE: replaces the 1.20.1 trick of swapping {@code Component.Serializer.GSON}. Components are codec-driven now,
 * and {@code ComponentSerialization} reads {@code Style.Serializer.MAP_CODEC} lazily, so wrapping the codecs at the end
 * of {@code Style.Serializer}'s static initializer makes every Component (JSON, NBT and network) DotStyle-aware.
 */
@Mixin(Style.Serializer.class)
public class StyleSerializerMixin {
    @Shadow @Final @Mutable public static MapCodec<Style> MAP_CODEC;
    @Shadow @Final @Mutable public static Codec<Style> CODEC;
    @Shadow @Final @Mutable public static StreamCodec<RegistryFriendlyByteBuf, Style> TRUSTED_STREAM_CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void tridot$wrapStyleCodecs(CallbackInfo ci) {
        MAP_CODEC = DotStyle.Codecs.wrap(MAP_CODEC);
        CODEC = MAP_CODEC.codec();
        TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
    }
}
