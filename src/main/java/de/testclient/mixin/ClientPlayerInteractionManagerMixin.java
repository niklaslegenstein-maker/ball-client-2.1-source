package de.testclient.mixin;

import de.testclient.modules.CombatModules;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    /** Criticals: vor jedem Angriff (auch manuell) die Crit-Pakete senden. */
    @Inject(method = "attackEntity", at = @At("HEAD"), require = 0)
    private void tc$attack(PlayerEntity player, Entity target, CallbackInfo ci) {
        CombatModules.Criticals.onAttack(target);
    }
}
