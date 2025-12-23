package com.xingmot.gtmadvancedhatch.api;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.function.Function;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 画饼：tag匹配，耐久匹配，自动销毁
 */
public class ConfigNotifiableItemStack extends NotifiableItemStackHandler implements IConfigTransfer<ItemStackHandler, ItemStack, ItemStack> {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConfigNotifiableItemStack.class, NotifiableItemStackHandler.MANAGED_FIELD_HOLDER);
    @Setter
    @Getter
    private NonNullList<Function<ItemStack, Boolean>> filterRef; // filter引用
    @Persisted
    public MutableLimitAndFilterStackTransfer lockedItem;
    @Persisted
    public final int maxCapacity;
    @Persisted
    @DescSynced
    public int[] itemCapacity;
    // TODO 锁空
    @Persisted
    boolean isLockedEmptySlot;

    // MetaMachine machine, int slots, @NotNull IO handlerIO, @NotNull IO capabilityIO,
    public ConfigNotifiableItemStack(MetaMachine machine, int size, int maxCapacity, @NotNull IO handlerIO, @NotNull IO capabilityIO) {
        super(machine, size, handlerIO, capabilityIO, (s) -> new MutableLimitAndFilterStackTransfer(s, maxCapacity));
        this.itemCapacity = new int[size];
        this.maxCapacity = maxCapacity;
        Arrays.fill(this.itemCapacity, maxCapacity);
        if (this.storage instanceof MutableLimitAndFilterStackTransfer mt)
            this.filterRef = mt.getFilters();
        this.lockedItem = new MutableLimitAndFilterStackTransfer(size, itemCapacity, filterRef);
    }

    public ConfigNotifiableItemStack(MetaMachine machine, int size, int maxCapacity, @NotNull IO handlerIO) {
        this(machine, size, maxCapacity, handlerIO, handlerIO);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void init() {
        for (int i = 0; i < this.itemCapacity.length; i++) {
            this.resetOneBasicInfo(i, (int) getCapacity(i));
            int finalI = i;
            if (isLocked(i))
                this.setFilter(i, (stack) -> stack.is(this.lockedItem.getStackInSlot(finalI).getItem()));
            else
                this.setFilter(i, (stack) -> !isLockedEmptySlot);
        }
    }

    @Override
    public void setCapacity(int index, int capacity) {
        this.itemCapacity[index] = (int) capacity;
    }

    @Override
    public int getCapacity(int index) {
        return itemCapacity[index];
    }

    @Override
    public void newCapacity(int capacity) {
        capacity = Math.max(0, Math.min(capacity, this.maxCapacity));
        Arrays.fill(this.itemCapacity, capacity);
        resetBasicInfo(capacity);
    }

    @Override
    public void newCapacity(int index, int capacity) {
        capacity = Math.max(0, Math.min(capacity, this.maxCapacity));
        resetOneBasicInfo(index, capacity);
    }

    /**
     * 设置全部格子的堆叠上限
     *
     * @param capacity 容量
     */
    public void resetBasicInfo(int capacity) {
        for (int i = 0; i < this.getSize(); i++) {
            resetOneBasicInfo(i, capacity);
        }
    }

    /**
     * 单独设置某一格的堆叠上限
     *
     * @param index    格子索引
     * @param capacity 容量，最小为0，最大为int.max
     */
    public void resetOneBasicInfo(int index, int capacity) {
        if (machine instanceof IMultiCapacity && this.storage instanceof MutableLimitAndFilterStackTransfer mt) {
            capacity = Math.max(capacity, 0);
            // 容量小于流体量时进行截断
            if (isTruncate(index, capacity))
                this.getStackInSlot(index).setCount(capacity);
            this.itemCapacity[index] = capacity; // 持久化用的
            mt.setSlotLimit(index, capacity); // 实际操作格子容量，并且ConfigSlotWidget会自己拿
        }
    }

    @Override
    public boolean isTruncate(int index, int capacity) {
        if (capacity < 0) capacity = 0;
        return !this.getStackInSlot(index).isEmpty() && capacity < this.getStackInSlot(index).getCount();
    }

    @Override
    public ItemStackHandler getLockedRef() {
        return lockedItem;
    }

    @Override
    public @NotNull ItemStack getIndexLocked(int slot) {
        return this.lockedItem.getStackInSlot(slot);
    }

    @Override
    public void setLocked(boolean locked, int slot) {
        if (slot < this.getSlots() && this.isLocked(slot) != locked) {
            ItemStack stack = this.getStackInSlot(slot);
            setLocked(locked, slot, stack);
        }
    }

    @Override
    public void setLocked(boolean locked, int slot, ItemStack itemStack) {
        if (slot < this.getSlots() && this.isLocked(slot) != locked) {
            if (locked && !itemStack.isEmpty()) {
                this.lockedItem.setStackInSlot(slot, itemStack.copyWithCount(1));
                this.setFilter(slot, (stack) -> stack.is(this.lockedItem.getStackInSlot(slot).getItem()));
                this.onContentsChanged();
            } else {
                this.lockedItem.setStackInSlot(slot, ItemStack.EMPTY);
                this.setFilter(slot, (stack) -> !isLockedEmptySlot);
                this.onContentsChanged();
            }

        }
    }

    @Override
    public boolean isLocked(int index) {
        return !this.lockedItem.getStackInSlot(index).isEmpty();
    }

    public void setFilter(int slot, Function<ItemStack, Boolean> filter) {
        this.filterRef.set(slot, filter);
    }
}
