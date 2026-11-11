package de.testclient.mixin;

import de.testclient.modules.CombatModules;
import de.testclient.modules.WorldModules;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** ESP (Glow): Entity wird mit Umriss durch Waende gezeichnet. */
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void tc$glow(CallbackInfoReturnable<Boolean> cir) {
        if (WorldModules.ESP.shouldGlow((Entity) (Object) this)) cir.setReturnValue(true);
    }

    /** Hitbox: groessere Trefferflaeche fuer das Fadenkreuz. */
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true, require = 0)
    private void tc$margin(CallbackInfoReturnable<Float> cir) {
        float extra = CombatModules.Hitbox.margin((Entity) (Object) this);
        if (extra > 0f) cir.setReturnValue(cir.getReturnValueF() + extra);
    }
}
