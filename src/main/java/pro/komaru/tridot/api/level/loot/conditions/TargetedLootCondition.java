package pro.komaru.tridot.api.level.loot.conditions;

import com.mojang.serialization.codecs.*;
import net.minecraft.world.level.storage.loot.LootContext.*;
import net.minecraft.world.level.storage.loot.predicates.*;

public abstract class TargetedLootCondition implements LootItemCondition{
    public EntityTarget target;
    public TargetedLootCondition(EntityTarget target) {
        this.target = target;
    }

    /** Shared "target" field for codecs of subclasses; defaults to {@code this} like the old serializer did. */
    public static <T extends TargetedLootCondition> RecordCodecBuilder<T, EntityTarget> targetCodec(){
        return EntityTarget.CODEC.optionalFieldOf("target", EntityTarget.THIS).forGetter(c -> c.target == null ? EntityTarget.THIS : c.target);
    }
}
