package dev.neonlyrics.mixin;

import dev.neonlyrics.sky.WorldSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// $VF: Compiled from FogRendererMixin.java
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
   @Inject(method = "computeFogColor", at = @At("RETURN"), require = 0)
   private void neonlyrics$skyFog(Camera camera, float pt, ClientLevel level, int renderDistance, float darken, Vector4f out, CallbackInfo ci) {
      if (camera.getFluidInCamera() == FogType.NONE) {
         WorldSky.tintFog(out);
      }
   }
}
