package com.xingmot.gtmadvancedhatch.api;

/**
 * 表示一个仓室每个槽位的容量是不同的,其中两个方法仅作为获取持久化数据使用
 */
public interface IMultiCapacity {

    void setCapacity(int index, int capacity);

    int getCapacity(int index);
}
