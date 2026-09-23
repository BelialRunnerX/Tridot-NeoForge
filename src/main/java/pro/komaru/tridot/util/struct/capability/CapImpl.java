package pro.komaru.tridot.util.struct.capability;

import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.common.util.*;

/**
 * A player data attachment value. PORT NOTE: NeoForge's INBTSerializable now passes a HolderLookup.Provider to
 * serializeNBT/deserializeNBT; implementations must accept it.
 */
public interface CapImpl extends INBTSerializable<CompoundTag> {
    void sync(ServerPlayer player);
}
