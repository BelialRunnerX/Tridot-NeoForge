package pro.komaru.tridot.client.gfx.particle.type;

import com.mojang.serialization.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.*;
import net.minecraft.network.codec.*;
import pro.komaru.tridot.client.gfx.particle.options.GenericParticleOptions;

/**
 * PORT NOTE: ParticleOptions.Deserializer is gone; particle types now expose a MapCodec and a StreamCodec. Tridot's
 * options are client-only objects that carry no serializable state, so both codecs produce a fresh default instance.
 */
public class AbstractParticleType<T extends GenericParticleOptions> extends ParticleType<T>{

    public AbstractParticleType(){
        super(false);
    }

    @Override
    public MapCodec<T> codec(){
        return MapCodec.unit(() -> newOptions(this));
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec(){
        return StreamCodec.unit(newOptions(this));
    }

    @SuppressWarnings("unchecked")
    public static <K extends GenericParticleOptions> K newOptions(ParticleType<K> type){
        return (K)new GenericParticleOptions(type);
    }

    public static <K extends GenericParticleOptions> Codec<K> genericCodec(ParticleType<K> type){
        return Codec.unit(() -> newOptions(type));
    }
}
