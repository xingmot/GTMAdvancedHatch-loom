package com.xingmot.gtmadvancedhatch.api;

import cn.qiuye.gtmoremachine.api.capability.IBindable;

import java.util.UUID;

public interface IMutableBind extends IBindable {
    void setUUID(UUID uuid);
}
