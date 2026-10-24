package com.sablednah.apocalypsetoolkit.gear;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

/** A shiv is all point: Sharpness on it counts one level higher (Sharpness V hits as VI). */
public final class ShivSharpness {
    private ShivSharpness() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((GetEnchantmentLevelEvent e) -> {
            if (!e.getStack().is(ModRegistry.SHIV.get())) return;
            e.getHolder(Enchantments.SHARPNESS).ifPresent(sharp -> {
                int lvl = e.getEnchantments().getLevel(sharp);
                if (lvl > 0) e.getEnchantments().set(sharp, lvl + 1);
            });
        });
    }
}
