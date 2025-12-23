package com.xingmot.gtmadvancedhatch.api;

/**
 * 可配置容量的物品/流体槽位
 * //TODO 流体槽位需要重构为多流体槽形式（以前是一个TankSlot一格流体，现在它支持多格了
 */
public interface IConfigTransfer<P, C, T> extends IMultiCapacity {

    void newCapacity(int capacity);

    /**
     * 此方法需要实现具体的容量变化
     */
    void newCapacity(int index, int capacity);

    /**
     * 是否会截断
     * 
     * @param index    格子索引
     * @param capacity 容量
     */
    boolean isTruncate(int index, int capacity);

    /**
     * 对于流体来说，因为capacity并非FluidStorage持久化数据，因此需要手动实现持久化读取。
     * 故在此留上一个接口，帮助进行初始化（可以在onLoad中加载持久化数据）
     */
    default void init() {};

    P getLockedRef();

    C getIndexLocked(int index);

    void setLocked(boolean locked, int index, T stack);

    void setLocked(boolean locked, int index);

    boolean isLocked(int index);
}
