package com.sablednah.apocalypsetoolkit.sprung;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * One door, trapdoor or fence gate plus one material, shapeless, gives the same block sprung ({@link Sprung#SPRUNG}).
 * Custom because the result is whichever block went in, so one recipe covers every wood and every mod's doors.
 * Data: {@code data/apocalypse_toolkit/recipe/sprung.json} (a pack swaps the material, ZARP for scrap). Being special,
 * it is not in the recipe book.
 */
public class SprungRecipe extends CustomRecipe {
    public static final MapCodec<SprungRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("item").forGetter(r -> r.gate),
            Ingredient.CODEC.fieldOf("material").forGetter(r -> r.material)).apply(i, SprungRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SprungRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.gate,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.material,
            SprungRecipe::new);
    public static final RecipeSerializer<SprungRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient gate, material;

    public SprungRecipe(Ingredient gate, Ingredient material) {
        this.gate = gate;
        this.material = material;
    }

    private ItemStack theGate(CraftingInput input) {
        ItemStack found = ItemStack.EMPTY;
        int materials = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack s = input.getItem(slot);
            if (s.isEmpty()) continue;
            if (gate.test(s) && !s.has(Sprung.SPRUNG.get()) && found.isEmpty()) found = s;
            else if (material.test(s)) materials++;
            else return ItemStack.EMPTY;
        }
        return materials == 1 ? found : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !theGate(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack out = theGate(input).copyWithCount(1);
        if (!out.isEmpty()) out.set(Sprung.SPRUNG.get(), Unit.INSTANCE);
        return out;
    }

    @Override
    public RecipeSerializer<SprungRecipe> getSerializer() {
        return SERIALIZER;
    }
}
