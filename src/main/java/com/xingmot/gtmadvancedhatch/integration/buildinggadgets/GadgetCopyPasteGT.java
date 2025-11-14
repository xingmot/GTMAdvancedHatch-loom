package com.xingmot.gtmadvancedhatch.integration.buildinggadgets;

import com.xingmot.gtmadvancedhatch.GTMAdvancedHatch;
import com.xingmot.gtmadvancedhatch.config.AHConfig;
import com.xingmot.gtmadvancedhatch.integration.buildinggadgets.util.AdjusteTagUtil;
import com.xingmot.gtmadvancedhatch.integration.buildinggadgets.util.BuildingUtilsGT;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

import javax.annotation.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.direwolf20.buildinggadgets2.api.gadgets.GadgetTarget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.util.*;
import com.direwolf20.buildinggadgets2.util.context.ItemActionContext;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.datatypes.TagPos;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.direwolf20.buildinggadgets2.util.modes.Copy;
import com.direwolf20.buildinggadgets2.util.modes.Paste;

public class GadgetCopyPasteGT extends GadgetCopyPaste {

    public GadgetCopyPasteGT() {
        super();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Minecraft mc = Minecraft.getInstance();
        if (level != null && mc.player != null) {
            tooltip.add(Component.translatable("item.buildinggadgets2.partern_copy_tooltip").withStyle(ChatFormatting.GOLD));
            if (GadgetNBT.getPasteReplace(stack)) {
                tooltip.add(Component.translatable("buildinggadgets2.voidwarning").withStyle(ChatFormatting.RED));
            }

            String templateName = GadgetNBT.getTemplateName(stack);
            if (!templateName.isEmpty()) {
                tooltip.add(Component.translatable("buildinggadgets2.templatename", new Object[] { templateName }).withStyle(ChatFormatting.AQUA));
            }

            boolean sneakPressed = Screen.hasShiftDown();
            if (sneakPressed) {}

        }
    }

    @Override
    public int getEnergyMax() {
        return AHConfig.INSTANCE.buildingGadgetMaxPower;
    }

    InteractionResultHolder<ItemStack> onAction(ItemActionContext context) {
        var gadget = context.stack();

        var mode = GadgetNBT.getMode(gadget);
        if (mode.getId().getPath().equals("copy")) {
            GadgetNBT.setCopyStartPos(gadget, context.pos());
            /* 关键在于此处：复制方块时需要把GT配置，电网绑定等信息拿到 */
            buildAndStore(context, gadget);
        } else if (mode.getId().getPath().equals("paste")) {
            UUID uuid = GadgetNBT.getUUID(gadget);
            BG2Data bg2Data = BG2Data.get(Objects.requireNonNull(context.player().level().getServer()).overworld());
            ArrayList<StatePos> buildList = bg2Data.getCopyPasteList(uuid, false); // Don't remove the data just yet
            ArrayList<TagPos> tagList = bg2Data.peekTEMap(uuid);
            BuildingUtilsGT.buildWithTileData(context.level(), context.player(), buildList, getHitPos(context).above().offset(GadgetNBT.getRelativePaste(gadget)), tagList, gadget, true, false);
            return InteractionResultHolder.success(gadget);
        } else {
            return InteractionResultHolder.pass(gadget);
        }

        return InteractionResultHolder.success(gadget);
    }

    InteractionResultHolder<ItemStack> onShiftAction(ItemActionContext context) {
        ItemStack gadget = context.stack();
        BaseMode mode = GadgetNBT.getMode(gadget);
        if (mode.getId().getPath().equals("copy")) {
            GadgetNBT.setCopyEndPos(gadget, context.pos());
            this.buildAndStore(context, gadget);
        } else if (!mode.equals(new Paste())) {
            return InteractionResultHolder.pass(gadget);
        }

        return InteractionResultHolder.success(gadget);
    }

    /** 复制方法：这里需要根据不同方块进行定制 */
    public void buildAndStore(ItemActionContext context, ItemStack gadget) {
        ArrayList<StatePos> buildList = (new Copy()).collect(context.hitResult().getDirection(), context.player(), context.pos(), Blocks.AIR.defaultBlockState());
        UUID uuid = GadgetNBT.getUUID(gadget);
        GadgetNBT.setCopyUUID(gadget);
        BG2Data bg2Data = BG2Data.get(((MinecraftServer) Objects.requireNonNull(context.player().level().getServer())).overworld());
        bg2Data.addToCopyPaste(uuid, buildList);
        ArrayList<TagPos> teData = new ArrayList<>();
        Level level = context.level();
        for (StatePos statePos : buildList) {
            BlockPos blockPos = statePos.pos.offset(GadgetNBT.getCopyStartPos(gadget));
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (AdjusteTagUtil.isModBlackListBlock(statePos.state) || AdjusteTagUtil.isModBlackListTag(blockEntity)) {
                statePos.state = Blocks.AIR.defaultBlockState();
                continue;
            }
            if (blockEntity != null) {
                CompoundTag blockTag = blockEntity.saveWithFullMetadata();
                GTMAdvancedHatch.LOGGER.info("blockTag: {}", blockTag);
                if (!AdjusteTagUtil.isModBlackListTag(blockEntity)) {
                    if (blockEntity instanceof MetaMachineBlockEntity || statePos.state.getBlock() instanceof MetaMachineBlock)
                        blockTag = AdjusteTagUtil.gtEmptyTagContent(blockTag);
                    blockTag = AdjusteTagUtil.getEmptyStorageTag(blockTag);
                    GTMAdvancedHatch.LOGGER.info("after blockTag: {}", blockTag);
                    TagPos tagPos = new TagPos(blockTag, blockPos.subtract(GadgetNBT.getCopyStartPos(gadget)));
                    teData.add(tagPos);
                }

            }
        }
        bg2Data.addToTEMap(uuid, teData);
        context.player().displayClientMessage(Component.translatable("buildinggadgets2.messages.copyblocks", new Object[] { buildList.size() }), true);
    }

    public GadgetTarget gadgetTarget() {
        return GadgetTarget.COPYPASTE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gadget = player.getItemInHand(hand);

        if (level.isClientSide()) // No client
            return InteractionResultHolder.success(gadget);

        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, gadget);
        if (level.getBlockState(lookingAt.getBlockPos()).isAir() && GadgetNBT.getAnchorPos(gadget).equals(GadgetNBT.nullPos))
            return InteractionResultHolder.success(gadget);
        ItemActionContext context = new ItemActionContext(lookingAt.getBlockPos(), lookingAt, player, level, hand, gadget);

        if (player.isShiftKeyDown()) {
            if (GadgetNBT.getSetting(gadget, "bind")) {
                ItemStack offhandItem = player.getOffhandItem();
                if (offhandItem.is(AEItems.BLANK_PATTERN.asItem()) || offhandItem.is(AEItems.PROCESSING_PATTERN.asItem()) || offhandItem.is(AEItems.CRAFTING_PATTERN.asItem()) || offhandItem.is(AEItems.SMITHING_TABLE_PATTERN.asItem()) || offhandItem.is(AEItems.STONECUTTING_PATTERN.asItem())) {
                    if (offhandItem.getCount() != 1) {
                        if (LDLib.isClient())
                            player.sendSystemMessage(Component.translatable("item.buildinggadgets2.partern_copy_message"));
                        return InteractionResultHolder.fail(gadget);
                    }
                    GenericStack[] materialList = getMaterialList(level, player, gadget);
                    // GTMAdvancedHatch.LOGGER.info(String.format("material list: %s", Arrays.toString(materialList)));

                    if (materialList.length > 0) {
                        if (offhandItem.hasTag()) {
                            // GTMAdvancedHatch.LOGGER.info(String.format("offhandItem: %s",
                            // offhandItem.getTag().toString()));
                        } else GTMAdvancedHatch.LOGGER.info(offhandItem.toString());
                        GenericStack out = Arrays.stream(materialList)
                                .filter(a -> a.what() instanceof AEItemKey aeItemKey && aeItemKey.getItem() instanceof MetaMachineItem mitem && mitem.getDefinition() instanceof MultiblockMachineDefinition)
                                .findAny().orElse(null);
                        if (out == null) {
                            out = Arrays.stream(materialList)
                                    .filter(a -> a.what() instanceof AEItemKey)
                                    .findAny()
                                    .orElse(null);
                            if (out == null)
                                out = GenericStack.fromItemStack(new ItemStack(gadget.getItem()).setHoverName(Component.translatable("item.buildinggadgets2.partern.0")));
                            else out = GenericStack.fromItemStack(((AEItemKey) out.what()).toStack()
                                    .setHoverName(Component.translatable("item.buildinggadgets2.partern.1")));
                        } else {
                            ItemStack itemStack = ((AEItemKey) out.what()).toStack()
                                    .setHoverName(Component.empty().append(out.what().getDisplayName()).append(Component.literal("x %d ".formatted(out.amount()))).append(Component.translatable("item.buildinggadgets2.partern.2")));
                            out = new GenericStack(GenericStack.fromItemStack(itemStack).what(), out.amount());
                        }
                        ItemStack is = AEItems.PROCESSING_PATTERN.asItem()
                                .encode(materialList, new GenericStack[] { out });
                        is.getOrCreateTag().putString("encodePlayer", player.getName().getString());
                        player.setItemInHand(InteractionHand.OFF_HAND, is);
                    }
                    return InteractionResultHolder.success(gadget);
                } else {
                    if (bindToInventory(level, player, gadget, lookingAt)) {
                        GadgetNBT.toggleSetting(gadget, "bind"); // Turn off bind
                        return InteractionResultHolder.success(gadget);
                    } else {
                        return InteractionResultHolder.fail(gadget);
                    }
                }
            }
            return this.onShiftAction(context);
        }

        return this.onAction(context);
    }

    public GenericStack[] getMaterialList(Level level, Player player, ItemStack gadget) {
        BG2Data bg2Data = BG2Data.get(Objects.requireNonNull(level.getServer()).overworld());
        UUID uuid = GadgetNBT.getUUID(gadget);
        ArrayList<StatePos> list = bg2Data.getCopyPasteList(uuid, false);
        HashMap<Item, GenericStack> itemList = new HashMap<>();

        if (list != null && !list.isEmpty()) {
            for (StatePos statePos : list) {
                ItemStack is = GadgetUtils.getItemForBlock(statePos.state, level, BlockPos.ZERO, player);
                if (is == null || is.isEmpty()) continue;
                if (!itemList.containsKey(is.getItem())) {
                    itemList.put(is.getItem(), GenericStack.fromItemStack(is));
                } else {
                    itemList.put(is.getItem(), GenericStack.sum(itemList.get(is.getItem()), Objects.requireNonNull(GenericStack.fromItemStack(is))));
                }
            }
            return itemList.values().toArray(new GenericStack[0]);
        } else {
            return itemList.values().toArray(new GenericStack[0]);
        }
    }
}
