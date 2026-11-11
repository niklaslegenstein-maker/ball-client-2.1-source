package de.testclient.mixin;

import de.testclient.modules.WorldModules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    /** Freecam: Bewegungspakete nicht senden, solange die Kamera frei fliegt. */
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true, require = 0)
    private void tc$send(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof PlayerMoveC2SPacket && WorldModules.Freecam.active()) ci.cancel();
    }
}
