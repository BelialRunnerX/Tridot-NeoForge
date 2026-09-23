package pro.komaru.tridot.common.registry;

import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import pro.komaru.tridot.*;

/**
 * PORT NOTE: enchantments are datapack registry entries in 1.21. The nine {@code Enchantment} subclasses were
 * replaced by JSON definitions under {@code data/tridot/enchantment/} and this class now exposes their
 * {@link ResourceKey}s plus level lookup helpers.
 * <p>
 * The old {@code EnchantmentCategory} predicates ({@code item instanceof DashItem} etc.) cannot be expressed in
 * data, so each category became an item tag under {@code tridot:enchantable/*}. Dependent mods must add their
 * items to those tags for the enchantments to apply (see {@link TagsRegistry}).
 */
public class EnchantmentsRegistry {
    public static final TagKey<Item> DASH_WEAPON = TagsRegistry.item(Tridot.ofTridot("enchantable/dash_weapon"));
    public static final TagKey<Item> RADIUS_WEAPON = TagsRegistry.item(Tridot.ofTridot("enchantable/radius_weapon"));
    public static final TagKey<Item> OVERDRIVE_CATEGORY = TagsRegistry.item(Tridot.ofTridot("enchantable/overdrive"));
    public static final TagKey<Item> SHIELD_CATEGORY = TagsRegistry.item(Tridot.ofTridot("enchantable/shield"));

    public static final ResourceKey<Enchantment> DASH = key("dash");
    public static final ResourceKey<Enchantment> RADIUS = key("radius");
    public static final ResourceKey<Enchantment> OVERDRIVE = key("overdrive");
    public static final ResourceKey<Enchantment> RESONANCE = key("resonance");
    public static final ResourceKey<Enchantment> VIGILANCE = key("vigilance");
    public static final ResourceKey<Enchantment> VANGUARD = key("vanguard");
    public static final ResourceKey<Enchantment> IRON_GRIP = key("iron_grip");
    public static final ResourceKey<Enchantment> DEFLECT = key("deflect");
    public static final ResourceKey<Enchantment> PUSH = key("push");

    private static ResourceKey<Enchantment> key(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT, Tridot.ofTridot(id));
    }

    /**
     * Level of {@code key} stored on the stack (the {@code minecraft:enchantments} component). Works without
     * registry access, so it can be used from item code that only has the stack; it does not fire NeoForge's
     * {@code GetEnchantmentLevelEvent}. Use {@link #getLevel(Level, ItemStack, ResourceKey)} when a level is available.
     */
    public static int getLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(key)) return entry.getIntValue();
        }
        return 0;
    }

    /** Level of {@code key} on the stack, resolved through the level's registry (fires NeoForge's level event). */
    public static int getLevel(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getEnchantmentLevel(holder(level.registryAccess(), key));
    }

    public static Holder<Enchantment> holder(HolderLookup.Provider registries, ResourceKey<Enchantment> key) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
