package com.sablednah.apocalypsetoolkit.medical;

import java.util.List;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;

/**
 * Field medicine, all of it vanilla's consumable component (use time, animation, sound, effects), so nothing here
 * needs code of its own:
 * <ul>
 *   <li>Bandage: two seconds to wrap, then Regeneration II for 5 s, about two hearts back over time.</li>
 *   <li>Medkit: four seconds, Instant Health II, Regeneration I for 10 s, and poison, wither and hunger cleared.</li>
 *   <li>Adrenaline syringe: half a second, Speed II and Strength I for 30 s.</li>
 *   <li>Antidote syringe: half a second, poison, wither, nausea, hunger and weakness cleared.</li>
 * </ul>
 */
public final class Medical {
    private Medical() {}

    public static Item.Properties bandage(Item.Properties p) {
        return p.stacksTo(16).component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(2F)
                .animation(ItemUseAnimation.BRUSH).sound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WOOL_PLACE)).hasConsumeParticles(false)
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1)))
                .build());
    }

    public static Item.Properties medkit(Item.Properties p) {
        return p.stacksTo(4).component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(4F)
                .animation(ItemUseAnimation.BRUSH).sound(SoundEvents.ARMOR_EQUIP_LEATHER).hasConsumeParticles(false)
                .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1),
                        new MobEffectInstance(MobEffects.REGENERATION, 200, 0))))
                .onConsume(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(MobEffects.POISON, MobEffects.WITHER, MobEffects.HUNGER)))
                .build());
    }

    public static Item.Properties adrenaline(Item.Properties p) {
        return p.stacksTo(8).component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(0.5F)
                .animation(ItemUseAnimation.NONE).sound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BOTTLE_EMPTY)).hasConsumeParticles(false)
                .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.SPEED, 600, 1),
                        new MobEffectInstance(MobEffects.STRENGTH, 600, 0))))
                .build());
    }

    public static Item.Properties antidote(Item.Properties p) {
        return p.stacksTo(8).component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(0.5F)
                .animation(ItemUseAnimation.NONE).sound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BOTTLE_EMPTY)).hasConsumeParticles(false)
                .onConsume(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(MobEffects.POISON, MobEffects.WITHER,
                        MobEffects.NAUSEA, MobEffects.HUNGER, MobEffects.WEAKNESS)))
                .build());
    }
}
