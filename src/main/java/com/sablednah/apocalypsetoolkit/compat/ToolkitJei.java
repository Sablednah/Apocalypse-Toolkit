package com.sablednah.apocalypsetoolkit.compat;

import java.util.ArrayList;
import java.util.List;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.sprung.Sprung;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI: sprung doors, trapdoors and fence gates as three recipes (not one per wood), in a "Sprung" category: the
 * door (or trapdoor, or gate) plus the material, giving the same one sprung. The input and output cycle together,
 * so oak gives sprung oak. Built from the tags a client has, so a pack's material (ZARP: scrap) is the one shown.
 * Only loaded when JEI is.
 */
@JeiPlugin
public class ToolkitJei implements IModPlugin {
    /** One of the three: what it's called, the blocks it covers (sprungable ones only), and the material. */
    public record Sprung3(String kind, List<ItemStack> plain, List<ItemStack> material) {}

    static final IRecipeType<Sprung3> TYPE = IRecipeType.create(ApocalypseToolkit.MOD_ID, "sprung", Sprung3.class);

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(TYPE, Items.CRAFTING_TABLE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> material = stacks(Sprung.MATERIAL, null);
        if (material.isEmpty()) return;
        List<Sprung3> recipes = new ArrayList<>();
        for (String kind : List.of("doors", "trapdoors", "fence_gates")) {
            List<ItemStack> plain = stacks(TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(kind)), Sprung.SPRUNGABLE_ITEMS);
            if (!plain.isEmpty()) recipes.add(new Sprung3(kind, plain, material));
        }
        registration.addRecipes(TYPE, recipes);
    }

    /** The items of {@code tag}, kept to those also in {@code within} when given. */
    private static List<ItemStack> stacks(TagKey<Item> tag, TagKey<Item> within) {
        List<ItemStack> out = new ArrayList<>();
        BuiltInRegistries.ITEM.get(tag).ifPresent(set -> {
            for (Holder<Item> h : set) {
                ItemStack s = new ItemStack(h.value());
                if (within == null || s.is(within)) out.add(s);
            }
        });
        return out;
    }

    static final class Category implements IRecipeCategory<Sprung3> {
        private final IDrawable icon, arrow;

        Category(IGuiHelper gui) {
            this.icon = gui.createDrawableItemStack(Sprung.sprung(Items.OAK_FENCE_GATE));
            this.arrow = gui.getRecipeArrow();
        }

        @Override public IRecipeType<Sprung3> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("jei.apocalypse_toolkit.sprung"); }
        @Override public int getWidth() { return 112; }
        @Override public int getHeight() { return 26; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Sprung3 recipe, IFocusGroup focuses) {
            List<ItemStack> sprung = recipe.plain().stream().map(s -> Sprung.sprung(s.getItem())).toList();
            var in = builder.addInputSlot(1, 5).setStandardSlotBackground().addItemStacks(recipe.plain());
            builder.addInputSlot(23, 5).setStandardSlotBackground().addItemStacks(recipe.material());
            var out = builder.addOutputSlot(91, 5).setOutputSlotBackground().addItemStacks(sprung);
            builder.createFocusLink(in, out);                     // oak in, sprung oak out
        }

        @Override
        public void draw(Sprung3 recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
            arrow.draw(g, 56, 5);
        }
    }
}
