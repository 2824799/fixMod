package com.gtnh.scoreboardfix.mixin;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import kubatech.loaders.block.kubablock.KubaBlock;

@Mixin(KubaBlock.class)
public abstract class MixinKubaBlock {

    @Shadow(remap = false)
    private WeakReference<World> lastAccessor;

    @Shadow(remap = false)
    private int X, Y, Z;

    @Shadow(remap = false)
    private static HashMap<Integer, kubatech.loaders.block.kubablock.BlockProxy> blocks;

    /**
     * @author YourName
     * @reason 修复跨服时因无效元数据导致的空指针异常
     *         重写getMaterial方法以包含空值检查
     */
    @Overwrite
    public Material getMaterial() {
        if (this.lastAccessor == null) {
            return Material.anvil; // 返回默认材质
        }

        World world = this.lastAccessor.get();
        if (world == null) {
            this.lastAccessor = null;
            return Material.anvil; // 返回默认材质
        }

        // 安全检查：确保坐标处的方块仍然是KubaBlock
        if (world.getBlock(this.X, this.Y, this.Z) != (Object) this) {
            return Material.anvil; // 返回默认材质
        }

        try {
            int metadata = world.getBlockMetadata(this.X, this.Y, this.Z);

            // 直接访问静态blocks字段
            if (blocks == null) {
                System.err.println("[KubaBlockFix] 警告：blocks字段为null");
                return Material.anvil;
            }

            kubatech.loaders.block.kubablock.BlockProxy proxy = blocks.get(metadata);
            if (proxy == null) {
                // 如果元数据无效，返回默认材质
                System.err.println("[KubaBlockFix] 警告：获取到无效的元数据 " + metadata + "，使用默认材质");
                return Material.anvil;
            }

            return proxy.getMaterial();
        } catch (Exception e) {
            // 捕获任何异常，避免客户端崩溃
            System.err.println("[KubaBlockFix] 获取材质时出错: " + e.getMessage());
            return Material.anvil; // 使用默认材质
        }
    }
}
