package com.gtnh.fixmod.mixin;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

import org.joml.Vector3d;
import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import gcewing.sg.blocks.orientation.Orient4WaysByState;
import gcewing.sg.interfaces.IBlockState;
import gcewing.sg.utils.Trans3;

@Mixin(value = Orient4WaysByState.class, remap = false)
public abstract class MixinOrient4WaysByState {

    /**
     * @author GTNH
     * @reason 修复facing为null时的空指针异常，高性能实现
     *         使用@Overwrite进行最小化修改，避免注入开销
     */
    @Overwrite(remap = false)
    public Trans3 localToGlobalTransformation(IBlockAccess world, Vector3i pos, IBlockState state, Vector3d origin) {
        // 高性能修复：快速获取并检查facing
        Object facingValue = state.getValue(Orient4WaysByState.FACING);

        // 如果facing无效，直接返回默认值
        if (!(facingValue instanceof EnumFacing f)) {
            // 不打印日志以保持性能
            System.err.println("[FixMod] [Facing] 警告：facing为null，返回0");
            return new Trans3(origin).turn(0); // NORTH对应0
        }

        // 原始的switch逻辑
        int i = switch (f) {
            case NORTH -> 0;
            case WEST -> 1;
            case SOUTH -> 2;
            case EAST -> 3;
            default -> 0; // 对于其他方向，使用默认值
        };
        return new Trans3(origin).turn(i);
    }
}
