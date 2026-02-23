package com.xingmot.gtmadvancedhatch.config;

import com.xingmot.gtmadvancedhatch.GTMAdvancedHatch;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = GTMAdvancedHatch.MODID)
public class AHConfig {

    public static AHConfig INSTANCE;
    public static final Object lock = new Object();

    public static void init() {
        synchronized (lock) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(AHConfig.class, ConfigFormats.yaml()).getConfigInstance();
            }
        }
    }

    @Configurable
    @Configurable.Comment({
            "是否显示大安培卡顿警告，这里主要是给gtl以外的整合包使用的配置项，默认为true。",
            "如果你制作的整合包修复相关bug，且可以无障碍使用大安培电网仓，请设为false。"
    })
    public boolean isDisplayNoFixCrashWarning = true;

    // TODO 能量槽科学计数法
    @Configurable
    @Configurable.Comment({
            "能量槽显示的格式",
    })
    public String energyJadeFormatPartern = "%s/%s";

    @Configurable
    @Configurable.Comment({ "无线电网监视器净功率为0时显示的文本，不要太长！！！" })
    public String WirelessEnergyMonitorZeroFormat = "0";

    @Configurable
    @Configurable.Range(min = 100, max = Integer.MAX_VALUE)
    @Configurable.Comment({ "默认是一般复制工具的100倍容量:100M" })
    public int buildingGadgetMaxPower = 100000000;

    @Configurable
    @Configurable.Comment({ "复制工具是否能建造AE线缆、终端等原先不能复制的东西（技术问题只能无消耗）" })
    public boolean buildingGadgetBuildAE2 = false;

    @Configurable
    @Configurable.Range(min = 1, max = 5)
    @Configurable.Comment({ "可配置流体仓、可配置总成的流体自动输入输出间隔（越小越快），默认：5" })
    public int configurableFluidIOTick = 5;

    @Configurable
    @Configurable.Comment({ "是否默认开启批处理" })
    public boolean isBatchEnable = true;
}
