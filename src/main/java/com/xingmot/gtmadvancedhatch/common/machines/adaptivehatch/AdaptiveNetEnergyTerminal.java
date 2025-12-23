package com.xingmot.gtmadvancedhatch.common.machines.adaptivehatch;

import cn.qiuye.gtmoremachine.utils.TeamUtils;
import com.xingmot.gtmadvancedhatch.api.IMutableBind;
import com.xingmot.gtmadvancedhatch.api.adaptivenet.*;
import com.xingmot.gtmadvancedhatch.common.data.AHItems;
import com.xingmot.gtmadvancedhatch.common.data.MachinesConstants;
import com.xingmot.gtmadvancedhatch.common.data.TagConstants;
import com.xingmot.gtmadvancedhatch.util.AHFormattingUtil;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.xingmot.gtmadvancedhatch.api.adaptivenet.AdaptiveConstants.NET_TYPE_ENERGY;
import static com.xingmot.gtmadvancedhatch.common.data.MachinesConstants.getMaxCapacity;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * 自适配电网系统终端
 * - 根据uuid和频率区分网络，默认为公共+频道0
 * - 当同频道终端出现多个时，后加载的会直接使用先加载的配置
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AdaptiveNetEnergyTerminal extends MetaMachine implements IFancyUIMachine, IMachineLife, IInteractedMachine, IMutableBind, IFrequency, INetEndpoint {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(AdaptiveNetEnergyTerminal.class, MetaMachine.MANAGED_FIELD_HOLDER);
    // 是否被覆盖（当存在已经注册的终端时被覆盖）
    @Getter
    @Persisted
    @DescSynced
    protected boolean isSlave = false;
    // 是否迁移模式（启用时更改频道和uuid，同网络适配仓会自动迁移至新频道和uuid
    @Getter
    @Setter
    @Persisted
    protected boolean isAutoRebind = true;
    // 适配网络所属玩家/队伍的uuid，默认为公用
    @Persisted
    @DescSynced
    protected UUID uuid = MachinesConstants.UUID_ZERO;
    // 适配网络频率
    @Getter
    @Persisted
    @DescSynced
    protected long frequency = 0L;
    AdaptiveMaster adaptiveMaster;
    AdaptiveSlave adaptiveSlave;
    // 这里用于存放四类电网仓的适配数据 这玩意不支持客户端同步，因此手动同步数据
    protected final AdaptiveData[] adaptiveData = new AdaptiveData[4];
    // 四个槽位的存储电网系列仓室(动力仓、能源仓、激光源仓、激光靶仓）以及槽位变化监听
    @Getter
    @Persisted
    protected final NotifiableItemStackHandler[] netEnergyInventory = new NotifiableItemStackHandler[4];
    @DescSynced
    public int[] stasticNum = new int[4];
    @Nullable
    protected ISubscription[] inventorySubs = new ISubscription[4];
    private TickableSubscription updNet;

    public AdaptiveNetEnergyTerminal(IMachineBlockEntity holder) {
        super(holder);
        initNetEnergyInventory();
        initAdaptiveData();
        this.adaptiveMaster = new AdaptiveMaster(this, NET_TYPE_ENERGY);
        this.adaptiveSlave = new AdaptiveSlave(this, NET_TYPE_ENERGY);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // region 四个槽位
    private void initNetEnergyInventory() {
        netEnergyInventory[0] = (new NotifiableItemStackHandler(this, 1, IO.BOTH, IO.NONE)).setFilter(AdaptiveTerminalBehaviour::isNetEnergyOutputHatch);
        netEnergyInventory[1] = (new NotifiableItemStackHandler(this, 1, IO.BOTH, IO.NONE)).setFilter(AdaptiveTerminalBehaviour::isNetEnergyInputHatch);
        netEnergyInventory[2] = (new NotifiableItemStackHandler(this, 1, IO.BOTH, IO.NONE)).setFilter(AdaptiveTerminalBehaviour::isNetLaserOutputHatch);
        netEnergyInventory[3] = (new NotifiableItemStackHandler(this, 1, IO.BOTH, IO.NONE)).setFilter(AdaptiveTerminalBehaviour::isNetLaserInputHatch);
    }

    private void initAdaptiveData() {
        adaptiveData[0] = new AdaptiveData(IO.OUT, 8L, 1, 0);
        adaptiveData[1] = new AdaptiveData(IO.IN, 8L, 1, 0);
        adaptiveData[2] = new AdaptiveData(IO.OUT, 8L, 256, 0);
        adaptiveData[3] = new AdaptiveData(IO.IN, 8L, 256, 0);
    }

    private void resetAdaptiveData(int i) {
        switch (i) {
            case 0:
                adaptiveData[0] = new AdaptiveData(IO.OUT, 8L, 1, 0);
                break;
            case 1:
                adaptiveData[1] = new AdaptiveData(IO.IN, 8L, 1, 0);
                break;
            case 2:
                adaptiveData[2] = new AdaptiveData(IO.OUT, 8L, 256, 0);
                break;
            case 3:
                adaptiveData[3] = new AdaptiveData(IO.IN, 8L, 256, 0);
                break;
        }
    }

    public static class AdaptiveData {

        public long maxEnergy;
        public long voltage = 8L;
        public int amps = 1;
        public int setTier = 0;
        public IO io;

        public AdaptiveData(IO io, long voltage, int amps, int setTier) {
            this.maxEnergy = getMaxCapacity(io, voltage, amps);
            this.voltage = voltage;
            this.amps = amps;
            this.setTier = setTier;
            this.io = io;
        }

        public void setData(long voltage, int amps, int setTier) {
            this.maxEnergy = getMaxCapacity(io, voltage, amps);
            this.voltage = voltage;
            this.amps = amps;
            this.setTier = setTier;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("maxEnergy", maxEnergy);
            tag.putLong("voltage", voltage);
            tag.putInt("amps", amps);
            tag.putInt("setTier", setTier);
            tag.putString("io", io.toString());
            return tag;
        }

        public static AdaptiveData fromTag(CompoundTag tag) {
            long voltage = tag.getLong("voltage");
            int amps = tag.getInt("amps");
            int setTier = tag.getInt("setTier");
            IO io = Enum.valueOf(IO.class, tag.getString("io"));
            return new AdaptiveData(io, voltage, amps, setTier);
        }
    }

    // endregion
    // =============================== IMachineLife ==================================
    /** 被挖时掉出里面的东西 */
    @Override
    public void onMachineRemoved() {
        for (int i = 0; i < 4; i++) {
            clearInventory(this.netEnergyInventory[i]);
        }
        if (!isSlave) this.adaptiveMaster.onUnloadOrRemove();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, () -> updateInventorySubscription(0)));
            serverLevel.getServer().tell(new TickTask(0, () -> updateInventorySubscription(1)));
            serverLevel.getServer().tell(new TickTask(0, () -> updateInventorySubscription(2)));
            serverLevel.getServer().tell(new TickTask(0, () -> updateInventorySubscription(3)));
            serverLevel.getServer().tell(new TickTask(0, () -> {
                // 在首个tick进行加载
                if (isSlave) adaptiveSlave.setUUIDAndFrequency(this.uuid, this.frequency);
                else adaptiveMaster.setUUIDAndFrequency(this.uuid, this.frequency);
            }));
        }
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            inventorySubs[i] = netEnergyInventory[i].addChangedListener(() -> updateInventorySubscription(finalI));
        }
        updateNetSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!isSlave) this.adaptiveMaster.onUnloadOrRemove();
    }

    private void updateNetSubscription() {
        if (frequency != 0L && this.isSlave) {
            this.updNet = this.subscribeServerTick(this.updNet, this::updateNet);
        } else if (this.updNet != null) {
            this.updNet.unsubscribe();
            this.updNet = null;
        }
    }

    private void updateNet() {
        // 在服务端每1秒更新一次
        // 此处用isRemote()的原因：在tick方法中执行时，单人存档isClient()始终为false
        if (!LDLib.isRemote() && Objects.requireNonNull(this.getServerLevel()).getServer().getTickCount() % 20 == 0) {
            if (!this.isSlave) {
                this.updNet.unsubscribe();
                this.updNet = null;
                return;
            }
            if (this.frequency != 0L && this.uuid != null) {
                // 更新状态，迁移或不迁移
                this.adaptiveSlave.updateStatus();
                // 当从端发现覆盖源端不存在时，自己成为主端
                if (NetMasterRegistry.get(NET_TYPE_ENERGY, frequency, uuid) == null) {
                    setSlave(!adaptiveMaster.setUUIDAndFrequency(this.uuid, this.frequency));
                    // 此时发现成为主端，则取消订阅从端更新方法
                    if (!this.isSlave) {
                        this.updNet.unsubscribe();
                        this.updNet = null;
                    }
                }
            }
        }
    }

    // 电网仓插槽内容物变化时，对应改变终端内电压电流等数据
    private void updateInventorySubscription(int index) {
        // 当配置被覆盖时跳过。
        if (isSlave) return;
        try {
            ItemStack itemstack = netEnergyInventory[index].storage.getStackInSlot(0);
            // TODO 魔改支持适配终端中所需数量的修改
            if (itemstack.getCount() < 64 && !AdaptiveTerminalBehaviour.isAnyTag(itemstack)) {
                resetAdaptiveData(index);
                return;
            }
            String itemName = itemstack.getItem().toString();
            String[] data = itemName.split("a_", 2)[0].split("_");
            int tier = Objects.equals(data[0], "opv") ? 13 : Objects.equals(data[0], "luv") ? 6 : Arrays.stream(GTValues.VN).toList().indexOf(data[0].toUpperCase());
            int amps = Integer.parseInt(data[1]);
            if (tier == -1) return;
            adaptiveData[index].setData(GTValues.V[tier], amps, tier);
            // TODO 此处写适配器逻辑
        } catch (Exception ignored) {}
    }

    // region 闪存读写
    /** 内存右击绑定至玩家/队伍 */
    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack is = player.getItemInHand(hand);
        if (is.isEmpty()) {
            return InteractionResult.PASS;
        } else if (is.is(AHItems.TOOL_NET_DATA_STICK.asItem())) {
            this.setUUID(TeamUtils.getTeamUUID(uuid));
            if (LDLib.isClient())
                player.displayClientMessage(Component.translatable("gtmoremachine.machine.wireless_energy_hatch.tooltip.bind", TeamUtils.getName(player)), true);
            return InteractionResult.SUCCESS;
        } else if (is.is(Items.STICK) && player.isCreative()) {
            // TODO 调试逻辑：木棍右键自动切换至已加载的终端的队伍uuid
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    /** 内存左击复制uuid与频道，以便设置适配仓 */
    @Override
    public boolean onLeftClick(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.isEmpty()) {
            return false;
        } else if (itemStack.is(AHItems.TOOL_NET_DATA_STICK.asItem())) {
            itemStack.setTag(writeConfigToTag());
            itemStack.setHoverName(Component.translatable("gtmadvancedhatch.machine.adaptive.data_stick.name"));
            player.displayClientMessage(Component.translatable("gtmadvancedhatch.machine.adaptive.import_data"), true);
            return true;
        } else if (itemStack.is(Items.STICK)) {
            // TODO 逻辑：木棍左键显示当前uuid下的全部已使用频道
            return true;
        } else {
            return false;
        }
    }

    /** 实际写入内存数据的方法 */
    protected CompoundTag writeConfigToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TagConstants.ADAPTIVE_NET_UUID, this.uuid);
        // UUID为公共UUID时传入everyone
        if (this.uuid.equals(MachinesConstants.UUID_ZERO)) tag.putString(TagConstants.ADAPTIVE_NET_NAME, "everyone");
        else tag.putString(TagConstants.ADAPTIVE_NET_NAME, AHUtil.getTeamName(this.holder.level(), this.uuid).getString());
        tag.putLong(TagConstants.ADAPTIVE_NET_FREQUENCY, this.frequency);
        return tag;
    }

    // endregion
    // =============================== IBindable ==================================
    // region 接口实现样板代码
    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public void setUUID(UUID uuid) {
        if (this.uuid.equals(uuid)) return;
        this.uuid = uuid;
        setSlave(!adaptiveMaster.setUUIDAndFrequency(this.uuid, this.frequency));
    }

    @Override
    public void setFrequency(long frequency) {
        if (this.frequency == frequency) return;
        this.frequency = frequency;
        setSlave(!adaptiveMaster.setUUIDAndFrequency(this.uuid, this.frequency));
    }

    /** 多终端同频段处理 */
    public void setSlave(boolean slave) {
        if (this.frequency == 0L || this.isSlave == slave) return;
        this.isSlave = slave;
        // 无论是否频段重复都要初始化数据
        initAdaptiveData();
        // 当频段重复时，再读取适配系统中的数据执行数据覆盖
        if (slave) {
            adaptiveMaster.onUnloadOrRemove();
            adaptiveSlave.setUUIDAndFrequency(this.uuid, this.frequency);
            updateNetSubscription();
        } else {
            // 否则从插槽中读取
            for (int i = 0; i < 4; i++) {
                updateInventorySubscription(i);
            }
        }
    }

    // endregion
    // =============================== INetEndpoint ==================================
    // region 接口实现样板代码
    @Nullable
    @Override
    public ServerLevel getServerLevel() {
        Level lvl = this.holder.getSelf().getLevel();
        return lvl instanceof ServerLevel sl ? sl : null;
    }

    @Override
    public BlockPos getBlockPos() {
        return this.holder.getSelf().getBlockPos();
    }

    @Override
    public Supplier<? extends CompoundTag> getData() {
        CompoundTag tag = new CompoundTag();
        if (isAutoRebind && frequency != 0L)
            tag.put("net_key", new NetKey(NET_TYPE_ENERGY, this.frequency, this.uuid).toTag());
        tag.put("data0", this.adaptiveData[0].toTag());
        tag.put("data1", this.adaptiveData[1].toTag());
        tag.put("data2", this.adaptiveData[2].toTag());
        tag.put("data3", this.adaptiveData[3].toTag());
        return () -> tag;
    }

    @Override
    public boolean encodeData(CompoundTag tag) {
        boolean flag = false;
        CompoundTag key = (CompoundTag) tag.get("net_key");
        if (isSlave && key != null && !key.isEmpty()) {
            this.frequency = key.getLong(TagConstants.ADAPTIVE_NET_FREQUENCY);
            this.uuid = key.getUUID(TagConstants.ADAPTIVE_NET_UUID);
            flag = true;
        }
        this.adaptiveData[0] = AdaptiveData.fromTag((CompoundTag) tag.get("data0"));
        this.adaptiveData[1] = AdaptiveData.fromTag((CompoundTag) tag.get("data1"));
        this.adaptiveData[2] = AdaptiveData.fromTag((CompoundTag) tag.get("data2"));
        this.adaptiveData[3] = AdaptiveData.fromTag((CompoundTag) tag.get("data3"));
        return flag;
    }

    @Override
    public boolean isEndpointRemoved() {
        return this.holder.getSelf().isRemoved();
    }

    // endregion
    // =============================== GUI ==================================
    public final ComponentPanelWidget custom = new ComponentPanelWidget(4, 5, components -> {}) {

        @Override
        public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
            // 需要保留原有的id为1的方法用于初始化（不然连初始化也要重写
            super.readUpdateInfo(id, buffer);
            if (id == 2) {
                lastText.clear();
                CompoundTag tag = buffer.readNbt();
                adaptiveData[0] = AdaptiveData.fromTag((CompoundTag) tag.get("data0"));
                adaptiveData[1] = AdaptiveData.fromTag((CompoundTag) tag.get("data1"));
                adaptiveData[2] = AdaptiveData.fromTag((CompoundTag) tag.get("data2"));
                adaptiveData[3] = AdaptiveData.fromTag((CompoundTag) tag.get("data3"));
                stasticNum = buffer.readVarIntArray();
                if (frequency == 0)
                    lastText.add(Component.translatable("gtmadvancedhatch.machine.adaptive.frequency")
                            .withStyle(ChatFormatting.DARK_GREEN)
                            .append(Component.translatable("gtmadvancedhatch.machine.adaptive.frequency.off")
                                    .withStyle(ChatFormatting.YELLOW)));
                else {
                    MutableComponent freq_component = Component.translatable("gtmadvancedhatch.machine.adaptive.frequency")
                            .append(String.valueOf(frequency));
                    if (isSlave)
                        freq_component.setStyle(freq_component.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("gtmadvancedhatch.machine.adaptive.fail").withStyle(ChatFormatting.BLUE))))
                                .withStyle(ChatFormatting.BLUE);
                    else
                        freq_component.withStyle(ChatFormatting.DARK_GREEN);
                    lastText.add(freq_component);
                }
                if (uuid.equals(MachinesConstants.UUID_ZERO))
                    lastText.add(Component.translatable("gtmoremachine.machine.wireless_energy_hatch.tooltip.bind", Component.translatable("gtmadvancedhatch.gui.bind_uuid.everyone"))
                            .withStyle(ChatFormatting.AQUA));
                else
                    lastText.add(Component.translatable("gtmoremachine.machine.wireless_energy_hatch.tooltip.bind", AHUtil.getTeamName(holder.level(), uuid))
                            .withStyle(ChatFormatting.AQUA));
                lastText.add(AHFormattingUtil.getFormatWidthComponent(Component.literal("动力仓：")
                        .withStyle(ChatFormatting.GREEN), Component.literal(adaptiveData[0].amps + "A  " + GTValues.VNF[adaptiveData[0].setTier] + " (" + adaptiveData[0].voltage + ")"), 220, "·"));
                lastText.add(Component.empty()
                        .append(Component.literal("  -")
                                .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("已加载："))
                        .append(Component.literal(String.valueOf(stasticNum[0]))
                                .withStyle(ChatFormatting.AQUA))
                        .append(" 个"));
                lastText.add(AHFormattingUtil.getFormatWidthComponent(Component.literal("能源仓：")
                        .withStyle(ChatFormatting.RED), Component.literal(adaptiveData[1].amps + "A  " + GTValues.VNF[adaptiveData[1].setTier] + " (" + adaptiveData[1].voltage + ")"), 220, "·"));
                lastText.add(Component.empty()
                        .append(Component.literal("  -")
                                .withStyle(ChatFormatting.RED))
                        .append(Component.literal("已加载："))
                        .append(Component.literal(String.valueOf(stasticNum[1]))
                                .withStyle(ChatFormatting.AQUA))
                        .append(" 个"));
                lastText.add(AHFormattingUtil.getFormatWidthComponent(Component.literal("激光源仓：")
                        .withStyle(ChatFormatting.DARK_GREEN), Component.literal(adaptiveData[2].amps + "A  " + GTValues.VNF[adaptiveData[2].setTier] + " (" + adaptiveData[2].voltage + ")"), 220, "·"));
                lastText.add(Component.empty()
                        .append(Component.literal("  -")
                                .withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("已加载："))
                        .append(Component.literal(String.valueOf(stasticNum[2]))
                                .withStyle(ChatFormatting.AQUA))
                        .append(" 个"));
                lastText.add(AHFormattingUtil.getFormatWidthComponent(Component.literal("激光靶仓：")
                        .withStyle(ChatFormatting.DARK_RED), Component.literal(adaptiveData[3].amps + "A  " + GTValues.VNF[adaptiveData[3].setTier] + " (" + adaptiveData[3].voltage + ")"), 220, "·"));
                lastText.add(Component.empty()
                        .append(Component.literal("  -")
                                .withStyle(ChatFormatting.DARK_RED))
                        .append(Component.literal("已加载："))
                        .append(Component.literal(String.valueOf(stasticNum[3]))
                                .withStyle(ChatFormatting.AQUA))
                        .append(" 个"));
                formatDisplayText();
                updateComponentTextSize();
            }
        }

        @Override
        public void detectAndSendChanges() {
            stasticNum[0] = ActiveAdaptiveNetStatistics.getNum(NET_TYPE_ENERGY, frequency, uuid, AdaptiveConstants.NET_ENERGY_SPECIAL_OUTPUT_HATCH);
            stasticNum[1] = ActiveAdaptiveNetStatistics.getNum(NET_TYPE_ENERGY, frequency, uuid, AdaptiveConstants.NET_ENERGY_SPECIAL_INPUT_HATCH);
            stasticNum[2] = ActiveAdaptiveNetStatistics.getNum(NET_TYPE_ENERGY, frequency, uuid, AdaptiveConstants.NET_ENERGY_SPECIAL_OUTPUT_LASER);
            stasticNum[3] = ActiveAdaptiveNetStatistics.getNum(NET_TYPE_ENERGY, frequency, uuid, AdaptiveConstants.NET_ENERGY_SPECIAL_INPUT_LASER);
            // 手动同步服务端与客户端数据（考虑到adaptiveData无法被@DescSync同步，故使用nbt
            writeUpdateInfo(2, buf -> {
                CompoundTag tag = new CompoundTag();
                tag.put("data0", adaptiveData[0].toTag());
                tag.put("data1", adaptiveData[1].toTag());
                tag.put("data2", adaptiveData[2].toTag());
                tag.put("data3", adaptiveData[3].toTag());
                buf.writeNbt(tag);
                buf.writeVarIntArray(stasticNum);
            });
        }
    };

    // region GUI样板代码
    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 220 + 8 + 8, 117 + 8);
        group.addWidget(
                new DraggableScrollableWidgetGroup(4, 4, 220 + 8, 117)
                        .setBackground(GuiTextures.DISPLAY)
                        .addWidget(custom.setMaxWidthLimit(220)))
                .setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    // endregion
    // 绑定侧页
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IFancyUIMachine.super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_WORKING_ENABLE.getSubTexture(0, 0.5, 1, 0.5),
                GuiTextures.BUTTON_WORKING_ENABLE.getSubTexture(0, 0, 1, 0.5),
                this::isAutoRebind, (clickData, pressed) -> setAutoRebind(pressed))
                .setTooltipsSupplier(pressed -> List.of(
                        Component.translatable("gtmadvancedhatch.gui.auto_rebind")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                                .append(Component.translatable(pressed ? "gtmadvancedhatch.gui.auto_rebind.yes" :
                                        "gtmadvancedhatch.gui.auto_rebind.no")),
                        Component.translatable("gtmadvancedhatch.gui.auto_rebind.info"),
                        Component.translatable("gtmadvancedhatch.gui.auto_rebind.info2"))));
        configuratorPanel.attachConfigurators(new NetHatchInvFancyConfigurator(this.netEnergyInventory[0].storage, this.netEnergyInventory[1].storage, this.netEnergyInventory[2].storage, this.netEnergyInventory[3].storage));
        configuratorPanel.attachConfigurators(new BindUUIDFancyConfigurator(this));
        configuratorPanel.attachConfigurators(new SetFrequencyFancyConfigurator(this));
    }
}
