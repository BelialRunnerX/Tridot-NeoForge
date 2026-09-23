package pro.komaru.tridot.mixin.common;

import com.google.gson.*;
import com.mojang.serialization.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.storage.loot.*;
import net.neoforged.neoforge.common.conditions.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import pro.komaru.tridot.*;

/**
 * PORT NOTE: replaces {@code ForgeHooksMixin}. Forge's {@code ForgeHooks.loadLootTable} is gone; NeoForge evaluates
 * {@code neoforge:conditions} natively while parsing every loot data entry through
 * {@code LootDataType.deserialize}. Tridot's {@code tridot:conditions} key is kept working by copying it into the
 * NeoForge key before the conditional codec runs, so existing data packs need no changes.
 */
@Mixin(LootDataType.class)
public class LootDataTypeMixin{
    @Unique
    private static final String TRIDOT_CONDITIONS = Tridot.ID + ":conditions";

    @ModifyVariable(method = "deserialize", at = @At("HEAD"), argsOnly = true)
    private <V> V tridot$aliasConditions(V value, ResourceLocation location, DynamicOps<V> ops){
        if(value instanceof JsonObject json && json.has(TRIDOT_CONDITIONS)){
            if(!json.has(ConditionalOps.DEFAULT_CONDITIONS_KEY)){
                json.add(ConditionalOps.DEFAULT_CONDITIONS_KEY, json.get(TRIDOT_CONDITIONS));
            }
            json.remove(TRIDOT_CONDITIONS);
        }
        return value;
    }
}
