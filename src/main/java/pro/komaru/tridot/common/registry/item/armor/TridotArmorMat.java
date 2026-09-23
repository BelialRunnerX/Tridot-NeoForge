package pro.komaru.tridot.common.registry.item.armor;

import net.minecraft.world.item.ArmorItem.*;
import pro.komaru.tridot.common.registry.item.builders.*;

/**
 * Tridot-specific data attached to a registered {@code ArmorMaterial}. Looked up through
 * {@link AbstractArmorRegistry#tridotMaterial}.
 */
public interface TridotArmorMat{
    AbstractArmorBuilder<?> builder();
    float getPercentDefenseForType(Type pType);
}
