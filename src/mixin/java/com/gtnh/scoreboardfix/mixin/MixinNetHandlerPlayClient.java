package com.gtnh.scoreboardfix.mixin;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S3EPacketTeams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    /**
     * @author Manus
     * @reason Blocks all scoreboard team packets to prevent NullPointerException.
     *         This is a complete workaround as requested by the user ("直接把计分板屏蔽掉").
     */
    @Inject(method = "handleTeams", at = @At("HEAD"), cancellable = true)
    private void scoreboardfix$blockHandleTeams(S3EPacketTeams packet, CallbackInfo ci) {
        System.out.println("[ScoreboardFix] Blocked scoreboard team packet to prevent NullPointerException.");
        ci.cancel(); // Cancel the original method execution
    }
}
