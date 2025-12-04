package com.gtnh.fixmod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldClient.class)
public class MixinWorldClientKeepChunks {

    // 保留半径（区块数，按你的需求改，32 对应你要的固定 32）
    private static final int KEEP_RADIUS = 32;

    @Inject(method = "doPreChunk", at = @At("HEAD"), cancellable = true)
    private void onDoPreChunk(int chunkX, int chunkZ, boolean load, CallbackInfo ci) {
        // 只拦截卸载（load == false）
        if (load) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        int playerChunkX = mc.thePlayer.chunkCoordX;
        int playerChunkZ = mc.thePlayer.chunkCoordZ;

        int dx = Math.abs(chunkX - playerChunkX);
        int dz = Math.abs(chunkZ - playerChunkZ);

        if (Math.max(dx, dz) <= KEEP_RADIUS) {
            // 取消卸载：客户端会继续保留该 chunk
            // 可以注释掉下面的 println 避免日志刷屏
            // System.out.println(
            // "[FixMod] [ChunkKeep] Prevented unload of chunk " + chunkX
            // + ","
            // + chunkZ
            // + " (within "
            // + KEEP_RADIUS
            // + ")");
            ci.cancel();
        }
    }
}
