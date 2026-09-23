package pro.komaru.tridot.common.registry;

import com.mojang.serialization.*;
import net.minecraft.core.component.*;
import net.minecraft.network.codec.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.registries.*;
import pro.komaru.tridot.*;

/**
 * PORT NOTE: ItemStack NBT no longer exists in 1.21. Data Tridot used to keep in the stack's root tag now lives in
 * these data components. Old worlds are upgraded by vanilla's DFU into {@code minecraft:custom_data}; the
 * {@code "skin"} key is read from there as a fallback (see {@code ItemSkin#itemSkin}).
 */
public class TridotDataComponents{
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Tridot.ID);

    /** Id of the {@code ItemSkin} applied to the stack ({@code "namespace:skin_id"}). Replaces the {@code "skin"} NBT string. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SKIN = COMPONENTS.registerComponentType("skin",
        builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static void register(IEventBus eventBus){
        COMPONENTS.register(eventBus);
    }
}
