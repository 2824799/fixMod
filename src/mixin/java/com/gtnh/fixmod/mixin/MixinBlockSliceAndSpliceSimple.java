package com.gtnh.fixmod.mixin;

import java.util.Random;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import crazypants.enderio.machine.slicensplice.BlockSliceAndSplice;
import crazypants.enderio.machine.slicensplice.TileSliceAndSplice;

@Mixin(value = BlockSliceAndSplice.class, remap = false)
public abstract class MixinBlockSliceAndSpliceSimple {

    @Inject(method = "randomDisplayTick", at = @At("HEAD"), cancellable = true)
    private void onRandomDisplayTick(World world, int x, int y, int z, Random rand, CallbackInfo ci) {
        TileEntity te = world.getTileEntity(x, y, z);
        // 如果 TE 为空或者类型不对，直接取消该方法的执行
        if (!(te instanceof TileSliceAndSplice)) {
            System.err.println("[FixMod] [EnderIO] randomDisplayTick修复");
            ci.cancel();
        }
    }
}
