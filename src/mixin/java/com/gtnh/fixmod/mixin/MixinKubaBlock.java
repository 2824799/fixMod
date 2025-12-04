package com.gtnh.fixmod.mixin;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import kubatech.loaders.block.kubablock.BlockProxy;
import kubatech.loaders.block.kubablock.KubaBlock;

@Mixin(KubaBlock.class)
public abstract class MixinKubaBlock extends Block {

    // 必须调用父类构造函数，虽然Mixin中不会实际执行
    public MixinKubaBlock(Material material) {
        super(material);
    }

    @Shadow(remap = false)
    private WeakReference<World> lastAccessor;

    @Shadow(remap = false)
    private int X, Y, Z;

    @Shadow(remap = false)
    private static HashMap<Integer, BlockProxy> blocks;

    /**
     * @author Fixer
     * @reason 修复报错日志中的核心崩溃：KubaBlock.getIcon 空指针异常
     */
    @Overwrite
    public IIcon getIcon(int side, int metadata) {
        try {
            // 检查 Map 是否初始化
            if (blocks == null) {
                // 如果没有对应的Proxy，返回基岩或石头的贴图防止崩溃
                System.err.println("[FixMod] [KubaBlock] [Render-Error] blocks == null");
                return Blocks.bedrock.getIcon(side, 0);
            }

            BlockProxy proxy = blocks.get(metadata);

            // 核心修复点：检查 proxy 是否为 null
            if (proxy == null) {
                // GTNH跨服常见情况：元数据不同步导致获取不到代理对象
                // 返回安全默认值（基岩），避免渲染崩溃
                System.err.println("[FixMod] [KubaBlock] [Render-Error] proxy == null");
                return Blocks.bedrock.getIcon(side, 0);
            }

            return proxy.getIcon(side);
        } catch (Exception e) {
            // 捕获所有其他潜在异常，Sodium渲染线程对异常非常敏感
            System.err.println("[FixMod] [KubaBlock] [Render-Error] e");
            return Blocks.bedrock.getIcon(side, 0);
        }
    }

    /**
     * @author Fixer
     * @reason 你原本提供的修复：修复 getMaterial 空指针异常
     */
    @Overwrite
    public Material getMaterial() {
        // 你的原始逻辑保留，这部分没问题，也是防御性编程的好习惯
        if (this.lastAccessor == null) {
            return Material.anvil;
        }

        World world = this.lastAccessor.get();
        if (world == null) {
            this.lastAccessor = null;
            return Material.anvil;
        }

        // 安全检查
        if (world.getBlock(this.X, this.Y, this.Z) != (Object) this) {
            return Material.anvil;
        }

        try {
            int metadata = world.getBlockMetadata(this.X, this.Y, this.Z);

            if (blocks == null) {
                System.err.println("[FixMod] [KubaBlock] [Material-Error] blocks == null");
                return Material.anvil;
            }

            BlockProxy proxy = blocks.get(metadata);
            if (proxy == null) {
                System.err.println("[FixMod] [KubaBlock] [Material-Error] proxy == null");
                return Material.anvil;
            }

            return proxy.getMaterial();
        } catch (Exception e) {
            System.err.println("[FixMod] [KubaBlock] [Material-Error] e");
            return Material.anvil;
        }
    }
}
