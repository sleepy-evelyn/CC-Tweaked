// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A description of a {@link ShapedRecipe}.
 * <p>
 * This is meant to be used in conjunction with {@link CustomShapedRecipe} for more reusable serialisation and
 * deserialisation of {@link ShapedRecipe}-like recipes.
 *
 * @param properties The common properties of this recipe.
 * @param template   The shaped template of the recipe.
 * @param result     The result of the recipe.
 */
public record ShapedRecipeSpec(RecipeProperties properties, ShapedTemplate template, ItemStack result) {
    public static final MapCodec<ShapedRecipeSpec> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeProperties.CODEC.forGetter(ShapedRecipeSpec::properties),
        ShapedTemplate.CODEC.forGetter(ShapedRecipeSpec::template),
        MoreCodecs.ITEM_STACK_WITH_NBT.fieldOf("result").forGetter(ShapedRecipeSpec::result)
    ).apply(instance, ShapedRecipeSpec::new));

    public static ShapedRecipeSpec fromNetwork(FriendlyByteBuf buffer) {
        var properties = RecipeProperties.fromNetwork(buffer);
        var template = ShapedTemplate.fromNetwork(buffer);
        var result = buffer.readItem();
        return new ShapedRecipeSpec(properties, template, result);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        properties().toNetwork(buffer);
        template().toNetwork(buffer);
        buffer.writeItem(result());
    }
}
