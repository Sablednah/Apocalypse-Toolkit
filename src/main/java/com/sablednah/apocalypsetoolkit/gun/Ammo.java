package com.sablednah.apocalypsetoolkit.gun;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Where a player's ammunition is, for reloading: loose in the inventory first, then loose in Curios slots, then
 * inside any container item that shares its contents through NeoForge's item capability (Sophisticated Backpacks,
 * and others) carried in the inventory, then worn in a Curios slot (a backpack on the back). Curios is reached by
 * reflection, so it is optional.
 */
public final class Ammo {
    private Ammo() {}

    /** How many of {@code ammo} the player can reload from. */
    public static int count(Player player, Item ammo) {
        return pass(player, ammo, Integer.MAX_VALUE, false);
    }

    /** Takes up to {@code want} of {@code ammo}; how many it got. */
    public static int take(Player player, Item ammo, int want) {
        return pass(player, ammo, want, true);
    }

    /** One walk through every place, in order, counting or taking until {@code want} is met. */
    private static int pass(Player player, Item ammo, int want, boolean take) {
        int got = 0;
        var inv = player.getInventory();
        List<Slot> curios = Curios.slots(player);
        // 1. loose in the inventory
        for (int i = 0; i < inv.getContainerSize() && got < want; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(ammo)) continue;
            int n = Math.min(want - got, s.getCount());
            if (take) s.shrink(n);
            got += n;
        }
        // 2. loose in Curios slots
        for (Slot c : curios) {
            if (got >= want) break;
            ItemStack s = c.get();
            if (!s.is(ammo)) continue;
            int n = Math.min(want - got, s.getCount());
            if (take) {
                ItemStack left = s.copy();
                left.shrink(n);
                c.set(left);
            }
            got += n;
        }
        // 3. containers carried in the inventory
        for (int i = 0; i < inv.getContainerSize() && got < want; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || s.is(ammo)) continue;
            got += from(ItemAccess.forPlayerSlot(player, i).getCapability(Capabilities.Item.ITEM), ammo, want - got, take);
        }
        // 4. containers worn in Curios slots (a backpack on the back)
        for (Slot c : curios) {
            if (got >= want) break;
            ItemStack s = c.get();
            if (s.isEmpty() || s.is(ammo)) continue;
            int n = from(ItemAccess.forStack(s).getCapability(Capabilities.Item.ITEM), ammo, want - got, take);
            if (take && n > 0) c.set(s);                    // write the stack back, in case its contents live on it
            got += n;
        }
        return got;
    }

    private static int from(@Nullable ResourceHandler<ItemResource> h, Item ammo, int want, boolean take) {
        if (h == null || want <= 0) return 0;
        if (take) {
            try (Transaction tx = Transaction.openRoot()) {
                int n = h.extract(ItemResource.of(ammo), want, tx);
                tx.commit();
                return n;
            }
        }
        long n = 0;
        for (int i = 0; i < h.size() && n < want; i++) if (h.getResource(i).is(ammo)) n += h.getAmountAsLong(i);
        return (int) Math.min(n, want);
    }

    /** One Curios slot. */
    public interface Slot {
        ItemStack get();
        void set(ItemStack stack);
    }

    /** Curios' equipped slots by reflection; none when Curios is absent or its API has moved. */
    public static final class Curios {
        private static boolean tried;
        private static @Nullable MethodHandle inventory, equipped, size, get, set;

        public static List<Slot> slots(Player player) {
            if (!tried) init();
            List<Slot> out = new ArrayList<>();
            if (inventory == null) return out;
            try {
                @SuppressWarnings("unchecked")
                Optional<Object> curios = (Optional<Object>) inventory.invoke((LivingEntity) player);
                if (curios.isEmpty()) return out;
                Object stacks = equipped.invoke(curios.get());
                if (stacks == null) return out;
                int n = (int) size.invoke(stacks);
                for (int i = 0; i < n; i++) {
                    final int slot = i;
                    out.add(new Slot() {
                        @Override public ItemStack get() { return call(() -> (ItemStack) get.invoke(stacks, slot), ItemStack.EMPTY); }
                        @Override public void set(ItemStack stack) { call(() -> { set.invoke(stacks, slot, stack); return null; }, null); }
                    });
                }
            } catch (Throwable t) {
                ApocalypseToolkit.LOGGER.warn("Couldn't read Curios slots ({})", t.toString());
            }
            return out;
        }

        private static void init() {
            tried = true;
            try {
                var lookup = MethodHandles.publicLookup();
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Class<?> handler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                Class<?> stacks = handler.getMethod("getEquippedCurios").getReturnType();
                inventory = lookup.findStatic(api, "getCuriosInventory", MethodType.methodType(Optional.class, LivingEntity.class));
                equipped = lookup.findVirtual(handler, "getEquippedCurios", MethodType.methodType(stacks));
                size = lookup.findVirtual(stacks, "getSlots", MethodType.methodType(int.class));
                get = lookup.findVirtual(stacks, "getStackInSlot", MethodType.methodType(ItemStack.class, int.class));
                set = lookup.findVirtual(stacks, "setStackInSlot", MethodType.methodType(void.class, int.class, ItemStack.class));
            } catch (ClassNotFoundException e) {
                inventory = null;                           // no Curios: fine
            } catch (ReflectiveOperationException e) {
                inventory = null;
                ApocalypseToolkit.LOGGER.warn("Curios is installed but its API isn't the shape this build knows; guns won't reload from Curios slots ({})", e.toString());
            }
        }

        private interface Call<T> { T run() throws Throwable; }

        private static <T> T call(Call<T> c, T fallback) {
            try { return c.run(); } catch (Throwable t) { return fallback; }
        }
    }
}
