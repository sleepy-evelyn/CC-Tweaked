// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public final class RecipeUtil {
    private RecipeUtil() {
    }

    public static NonNullList<Ingredient> readIngredients(FriendlyByteBuf buffer) {
        var count = buffer.readVarInt();
        var ingredients = NonNullList.withSize(count, Ingredient.EMPTY);
        for (var i = 0; i < ingredients.size(); i++) ingredients.set(i, Ingredient.fromNetwork(buffer));
        return ingredients;
    }

    public static void writeIngredients(FriendlyByteBuf buffer, NonNullList<Ingredient> ingredients) {
        buffer.writeCollection(ingredients, (a, b) -> b.toNetwork(a));
    }

    /**
     * Calls {@link Recipe#getResultItem(RegistryAccess)} with a {@code null} {@link RegistryAccess}.
     * <p>
     * This should only be called on specific recipe types, where the accessor is known to be safe.
     *
     * @param recipe The recipe to get the result of.
     * @return The recipe's result.
     */
    @SuppressWarnings("DataFlowIssue")
    public static ItemStack getResultUnsafe(Recipe<?> recipe) {
        return recipe.getResultItem(null);
    }

    public static <A> MapCodec<A> unsafeUnwrapMapCodec(Codec<A> codec) {
        return ((MapCodec.MapCodecCodec<A>) codec).codec();
    }

    public static <A, O extends A> RecordCodecBuilder<O, A> unsafeUnwrapRecordCodec(Codec<A> codec) {
        return unsafeUnwrapMapCodec(codec).forGetter(x -> x);
    }

}
