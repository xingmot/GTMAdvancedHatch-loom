package com.xingmot.gtmadvancedhatch.api;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import cn.qiuye.gtmoremachine.api.misc.UnlimitedItemStackTransfer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.function.Function;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class MutableLimitAndFilterStackTransfer extends UnlimitedItemStackTransfer {

    @Setter
    @Getter
    private NonNullList<Function<ItemStack, Boolean>> filters;
    public int[] limit;

    public MutableLimitAndFilterStackTransfer(int size, int limit) {
        super(size);
        this.limit = new int[size];
        Arrays.fill(this.limit, limit);
        this.filters = NonNullList.withSize(size, stack -> true);
    }

    public MutableLimitAndFilterStackTransfer(int size, int[] limit, NonNullList<Function<ItemStack, Boolean>> filter) {
        super(size);
        this.limit = limit;
        this.filters = filter;
    }

    public MutableLimitAndFilterStackTransfer(NonNullList<ItemStack> stacks, int[] limit, NonNullList<Function<ItemStack, Boolean>> filters) {
        super(stacks);
        this.limit = limit;
        this.filters = filters;
    }

    public void setSlotLimit(int slot, int limit) {
        this.limit[slot] = limit;
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.limit[slot];
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return Math.min(this.limit[slot], stack.getMaxStackSize() * (this.limit[slot] / 64));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return this.filters.get(slot).apply(stack);
    }
}
