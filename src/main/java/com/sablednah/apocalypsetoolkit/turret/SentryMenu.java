package com.sablednah.apocalypsetoolkit.turret;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The turret's screen: ammunition and repair slots, wear and mode (synced), and button 0 to cycle the mode. */
public class SentryMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public SentryMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(2), new SimpleContainerData(3), BlockPos.ZERO);
    }

    public SentryMenu(int id, Inventory inv, Container container, ContainerData data, BlockPos pos) {
        super(ModRegistry.SENTRY_MENU.get(), id);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(container, SentryBlockEntity.AMMO, 44, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return SentryBlockEntity.isAmmo(stack); }
        });
        addSlot(new Slot(container, SentryBlockEntity.IRON, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.IRON_INGOT); }
        });
        addStandardInventorySlots(inv, 8, 84);
        addDataSlots(data);
    }

    public int health() { return data.get(0); }
    public int maxHealth() { return Math.max(1, data.get(1)); }
    public SentryBlockEntity.Mode mode() { return SentryBlockEntity.Mode.values()[Math.floorMod(data.get(2), 4)]; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0) return false;
        if (container instanceof SentryBlockEntity be) be.cycleMode();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (SentryBlockEntity.isAmmo(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.is(Items.IRON_INGOT)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }
}
