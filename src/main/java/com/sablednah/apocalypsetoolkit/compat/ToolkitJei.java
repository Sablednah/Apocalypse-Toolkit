package com.sablednah.apocalypsetoolkit.compat;

import java.util.ArrayList;
import java.util.List;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.sprung.Sprung;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * JEI: the sprung recipe is a special one (any door, trapdoor or gate plus the material), which JEI can't show, so this
 * lists it as one ordinary crafting recipe per door, trapdoor and gate, built from the two tags a client has (so a pack
 * that swaps the material, as ZARP does to scrap, shows its own). Only loaded when JEI is.
 */
@JeiPlugin
public class ToolkitJei implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var material = BuiltInRegistries.ITEM.get(Sprung.MATERIAL);
        var gates = BuiltInRegistries.ITEM.get(Sprung.SPRUNGABLE_ITEMS);
        if (material.isEmpty() || gates.isEmpty() || material.get().size() == 0) return;
        Ingredient with = Ingredient.of(material.get());
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        for (Holder<Item> gate : gates.get()) {
            ItemStackTemplate result = new ItemStackTemplate(gate, 1,
                    DataComponentPatch.builder().set(Sprung.SPRUNG.get(), Unit.INSTANCE).build());
            ShapelessRecipe recipe = new ShapelessRecipe(new Recipe.CommonInfo(true),
                    new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "sprung"), result,
                    List.of(Ingredient.of(gate.value()), with));
            Identifier item = BuiltInRegistries.ITEM.getKey(gate.value());
            recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "sprung/" + item.getNamespace() + "/" + item.getPath())), recipe));
        }
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }
}
