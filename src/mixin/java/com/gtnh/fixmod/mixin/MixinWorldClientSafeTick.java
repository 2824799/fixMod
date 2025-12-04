package com.gtnh.fixmod.mixin;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// 1. 目标改为原版 WorldClient
@Mixin(WorldClient.class)
public abstract class MixinWorldClientSafeTick {

    // 2. 拦截 doVoidFogParticles 方法中对 Block.randomDisplayTick 的调用
    // 注意：GTNH环境通常是SRG名，这里我们需要同时匹配开发环境名和SRG名，或者依靠remap
    // doVoidFogParticles 的 SRG 是 func_72833_b
    // randomDisplayTick 的 SRG 是 func_149734_b

    @Redirect(
        method = { "doVoidFogParticles", "func_72833_b" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;randomDisplayTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V"),
        require = 0 // 设置为0防止因为Optifine修改了WorldClient导致找不到目标而崩溃
    )
    public void redirectRandomDisplayTick(Block block, World world, int x, int y, int z, Random rand) {
        // 获取方块ID，判断是不是切肉机
        // 直接用字符串判断类名最稳妥，避免引入额外的类依赖导致NoClassDefFoundError
        if (block.getClass()
            .getName()
            .equals("crazypants.enderio.machine.slicensplice.BlockSliceAndSplice")) {
            TileEntity te = world.getTileEntity(x, y, z);
            // 这里执行你的核心修复逻辑
            if (te != null && !te.getClass()
                .getName()
                .equals("crazypants.enderio.machine.slicensplice.TileSliceAndSplice")) {
                // 类型不匹配（例如是 ExperienceObelisk），直接跳过，不调用原方法
                // System.out.println("[FixMod] 拦截了一次致命的粒子效果渲染");
                return;
            }
        }

        // 如果一切正常，或者不是切肉机，照常执行原版逻辑
        block.randomDisplayTick(world, x, y, z, rand);
    }
}
