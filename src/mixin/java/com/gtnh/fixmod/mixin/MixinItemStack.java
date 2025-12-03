package com.gtnh.fixmod.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Shadow
    public abstract Item getItem();

    /**
     * @author YourName
     * @reason 修复Item为null时的空指针异常
     *         重写getMaxStackSize方法以包含空值检查
     */
    @Overwrite
    public int getMaxStackSize() {
        Item item = getItem();
        if (item == null) {
            System.err.println("[FixMod] [ItemStack] 警告：Item为null，返回默认堆叠大小64");
            return 64;
        }
        return item.getItemStackLimit((ItemStack) (Object) this);
    }
}
