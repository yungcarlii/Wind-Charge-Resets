package lilcarlii.windcharge.mixin;

import lilcarlii.windcharge.WindChargeReset;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WindChargeItem.class)
public abstract class WindChargeItemMixin {
	@Redirect(
		method = "use",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;"
		)
	)
	private Projectile windChargeReset$resetWhenProjectileIsThrown(Projectile.ProjectileFactory<? extends Projectile> factory, ServerLevel level, ItemStack stack, LivingEntity shooter, float rotationOffset, float power, float uncertainty) {
		Projectile projectile = Projectile.spawnProjectileFromRotation(factory, level, stack, shooter, rotationOffset, power, uncertainty);
		if (WindChargeReset.tryResetOnThrow(shooter, level)) {
			projectile.discard();
		}

		return projectile;
	}
}
