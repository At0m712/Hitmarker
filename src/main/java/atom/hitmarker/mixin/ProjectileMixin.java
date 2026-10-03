package atom.hitmarker.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import atom.hitmarker.HitMarkerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

	@Inject(at = @At("HEAD"), method = "onHit")
	private void onHit(HitResult hitResult, CallbackInfo ci) {
		if (hitResult.getType() == HitResult.Type.ENTITY) {
			Projectile projectile = (Projectile) (Object) this;
			Entity owner = projectile.getOwner();
			Minecraft client = Minecraft.getInstance();

			if (client.player != null && (owner == client.player || (owner != null && owner.getId() == client.player.getId()))) {
				EntityHitResult entityHit = (EntityHitResult) hitResult;
				Entity hitEntity = entityHit.getEntity();

				if (hitEntity instanceof LivingEntity livingEntity) {
					client.execute(() -> {
						HitMarkerClient.projectileHit(livingEntity);
					});
				}
			}
		}
	}
}