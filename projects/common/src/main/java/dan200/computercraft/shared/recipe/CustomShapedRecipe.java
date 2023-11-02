// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.function.Function;

/**
 * A custom version of {@link ShapedRecipe}, which can be converted to and from a {@link ShapedRecipeSpec}.
 * <p>
 * This recipe may both be used as a normal recipe (behaving mostly the same as {@link ShapedRecipe}, with
 * {@linkplain MoreCodecs#ITEM_STACK_WITH_NBT support for putting nbt on the result}), or subclassed to
 * customise the crafting behaviour.
 */
public class CustomShapedRecipe extends ShapedRecipe {
    private final ItemStack result;

    public CustomShapedRecipe(ShapedRecipeSpec recipe) {
        super(
            recipe.properties().group(), recipe.properties().category(),
            recipe.template().width(), recipe.template().height(), recipe.template().ingredients(),
            recipe.result()
        );
        this.result = recipe.result();
    }

    public final ShapedRecipeSpec toSpec() {
        return new ShapedRecipeSpec(RecipeProperties.of(this), ShapedTemplate.of(this), result);
    }

    @Override
    public RecipeSerializer<? extends CustomShapedRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.SHAPED.get();
    }

    public static <T extends CustomShapedRecipe> RecipeSerializer<T> serialiser(Function<ShapedRecipeSpec, T> factory) {
        return new Serialiser<>(r -> DataResult.success(factory.apply(r)));
    }

    public static <T extends CustomShapedRecipe> RecipeSerializer<T> validatingSerialiser(Function<ShapedRecipeSpec, DataResult<T>> factory) {
        return new Serialiser<>(factory);
    }

    private record Serialiser<T extends CustomShapedRecipe>(
        Function<ShapedRecipeSpec, DataResult<T>> factory
    ) implements RecipeSerializer<T> {
        private Serialiser(Function<ShapedRecipeSpec, DataResult<T>> factory) {
            this.factory = r -> factory.apply(r).flatMap(x -> {
                if (x.getSerializer() != this) {
                    return DataResult.error(() -> "Expected serialiser to be " + this + ", but was " + x.getSerializer());
                }
                return DataResult.success(x);
            });
        }

        @Override
        public Codec<T> codec() {
            return ShapedRecipeSpec.CODEC.codec().flatXmap(factory, x -> DataResult.success(x.toSpec()));
        }

        @Override
        public T fromNetwork(FriendlyByteBuf buffer) {
            return Util.getOrThrow(factory.apply(ShapedRecipeSpec.fromNetwork(buffer)), IllegalStateException::new);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, T recipe) {
            recipe.toSpec().toNetwork(buffer);
        }
    }
}
