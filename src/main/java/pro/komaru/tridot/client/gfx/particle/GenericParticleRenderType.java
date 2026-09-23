package pro.komaru.tridot.client.gfx.particle;

import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.*;
import net.neoforged.api.distmarker.*;
import pro.komaru.tridot.client.compatibility.*;
import pro.komaru.tridot.client.render.LevelRenderHandler;

public class GenericParticleRenderType implements ParticleRenderType{
    public static final GenericParticleRenderType INSTANCE = new GenericParticleRenderType();

    // PORT NOTE: ParticleRenderType.begin now returns the BufferBuilder to render into and end() is gone. Tridot's
    // particles write into the delayed render buffers instead, so an empty builder is returned (ParticleEngine skips
    // uploading an empty mesh).
    @Override
    public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager){
        if(ShadersIntegration.shouldApply()) LevelRenderHandler.MATRIX4F = RenderSystem.getModelViewMatrix();
        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }
}
