package pro.komaru.tridot.client.model.render.item;

import com.mojang.blaze3d.vertex.*;
import pro.komaru.tridot.client.model.item.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.resources.*;
import net.minecraft.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.api.distmarker.*;
import net.neoforged.neoforge.client.*;

import javax.annotation.*;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class LargeItemRenderer{

    // PORT NOTE: the "_in_hand" model is side-loaded (ModelEvent.RegisterAdditional). 1.20.1 used the "inventory" variant, which
    // loaded models/item/<item>_in_hand.json; NeoForge 1.21 requires the "standalone" variant, which loads models/<path>.json,
    // so the "item/" prefix keeps the same file. Renderers must look the model up with this exact key.
    public static ModelResourceLocation getModelResourceLocation(String modId, String item){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(modId, "item/" + item + "_in_hand"));
    }

    // PORT NOTE: the bake map is keyed by ModelResourceLocation in 1.21. The default model is the item's own "inventory" model
    // for real items; for side-loaded skins ("skin/<name>") it is the standalone model registered by SkinRegistryManager.
    public static void bakeModel(Map<ModelResourceLocation, BakedModel> map, String modId, String item, CustomItemOverrides itemOverrides){
        ModelResourceLocation modelInventory = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(modId, item));
        if(!map.containsKey(modelInventory)){
            modelInventory = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(modId, "item/" + item));
        }
        ModelResourceLocation modelHand = getModelResourceLocation(modId, item);

        BakedModel bakedModelDefault = map.get(modelInventory);
        BakedModel bakedModelHand = map.get(modelHand);
        BakedModel modelWrapper = new LargeItemModel(bakedModelDefault, bakedModelHand, itemOverrides);
        map.put(modelInventory, modelWrapper);
    }

    public static void bakeModel(Map<ModelResourceLocation, BakedModel> map, String modId, String item){
        bakeModel(map, modId, item, new CustomItemOverrides());
    }

    public static class LargeItemModel implements BakedModel{
        private final BakedModel bakedModelDefault;
        private final BakedModel bakedModelHand;
        private final CustomItemOverrides itemOverrides;

        public LargeItemModel(BakedModel bakedModelDefault, BakedModel bakedModelHand){
            this.bakedModelDefault = bakedModelDefault;
            this.bakedModelHand = bakedModelHand;
            this.itemOverrides = new CustomItemOverrides();
        }

        public LargeItemModel(BakedModel bakedModelDefault, BakedModel bakedModelHand, CustomItemOverrides itemOverrides){
            this.bakedModelDefault = bakedModelDefault;
            this.bakedModelHand = bakedModelHand;
            this.itemOverrides = itemOverrides;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random){
            return bakedModelDefault.getQuads(state, direction, random);
        }

        @Override
        public boolean useAmbientOcclusion(){
            return bakedModelDefault.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d(){
            return bakedModelDefault.isGui3d();
        }

        @Override
        public boolean usesBlockLight(){
            return bakedModelDefault.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer(){
            return bakedModelDefault.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleIcon(){
            return bakedModelDefault.getParticleIcon();
        }

        @Override
        public ItemOverrides getOverrides(){
            return itemOverrides;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform){
            BakedModel modelToUse = bakedModelDefault;
            if(context != ItemDisplayContext.GUI && context != ItemDisplayContext.GROUND && context != ItemDisplayContext.FIXED){
                modelToUse = bakedModelHand;
            }
            return ClientHooks.handleCameraTransforms(poseStack, modelToUse, context, applyLeftHandTransform);
        }
    }
}
