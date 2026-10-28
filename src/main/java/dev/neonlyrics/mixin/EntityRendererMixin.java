package dev.neonlyrics.mixin;

import dev.neonlyrics.cosmetic.DeathFx;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// $VF: Compiled from EntityRendererMixin.java
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
   @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$hideForDeathFx(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      neonlyrics$hideForDeathFx(entity, cir);
   }

   @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDDF)Z", at = @At("HEAD"), cancellable = true, require = 0)
   private void neonlyrics$hideForDeathFx26_3(Entity entity, Frustum frustum, double x, double y, double z, float distance, CallbackInfoReturnable<Boolean> cir) {
      neonlyrics$hideForDeathFx(entity, cir);
   }

   private static void neonlyrics$hideForDeathFx(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (DeathFx.hides(entity)) {
         cir.setReturnValue(false);
      }
   }
}
