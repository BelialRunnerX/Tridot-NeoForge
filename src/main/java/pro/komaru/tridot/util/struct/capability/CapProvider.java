package pro.komaru.tridot.util.struct.capability;

import net.minecraft.nbt.*;
import net.neoforged.neoforge.common.util.*;

/**
 * PORT NOTE: Forge's ICapabilityProvider no longer exists. On 1.20.1 this wrapped a {@link CapImpl} and exposed it
 * through the capability system; with data attachments the {@link CapImpl} value is attached directly, so this type
 * is only kept as a serializable marker for code that still references it. Prefer {@link CapImpl}.
 */
public interface CapProvider extends INBTSerializable<CompoundTag> {
}
