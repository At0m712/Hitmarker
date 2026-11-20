package atom.hitmarker.mixin;

import atom.hitmarker.HitMarkerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

				if (hitEntity instanceof LivingEntity livingEntity
						&& !(livingEntity instanceof ArmorStand)
						&& !livingEntity.isInvulnerable()
						&& !(livingEntity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
					client.execute(() -> HitMarkerClient.projectileHit(livingEntity));
				}
			}
		}
	}
}
