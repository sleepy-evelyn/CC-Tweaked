// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The template for {@linkplain ShapedRecipe shaped recipes}. This largely exists for parsing shaped recipes from JSON.
 *
 * @param width       The width of the recipe, see {@link ShapedRecipe#getWidth()}.
 * @param height      The height of the recipe, see {@link ShapedRecipe#getHeight()}.
 * @param ingredients The ingredients in the recipe, see {@link ShapedRecipe#getIngredients()}
 */
public record ShapedTemplate(int width, int height, NonNullList<Ingredient> ingredients) {
    private static final Codec<List<String>> PATTERN_CODEC = ExtraCodecs.validate(Codec.STRING.listOf(), patterns -> {
        if (patterns.size() > 3) return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
        if (patterns.isEmpty()) return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");

        var width = patterns.get(0).length();

        for (var p : patterns) {
            if (p.length() > 3) return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
            if (width != p.length()) return DataResult.error(() -> "Invalid pattern: each row must be the same width");
        }

        return DataResult.success(patterns);
    });

    private static final Codec<String> SINGLE_CHARACTER_STRING_CODEC = ExtraCodecs.validate(Codec.STRING, string -> {
        if (string.length() != 1) {
            return DataResult.error(() -> "Invalid key entry: '" + string + "' is an invalid symbol (must be 1 character only).");
        }
        if (" ".equals(string)) return DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.");

        return DataResult.success(string);
    });

    private static final MapCodec<Pair<Map<String, Ingredient>, List<String>>> RAW_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ExtraCodecs.strictUnboundedMap(SINGLE_CHARACTER_STRING_CODEC, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(Pair::getFirst),
        PATTERN_CODEC.fieldOf("pattern").forGetter(Pair::getSecond)
    ).apply(instance, Pair::of));

    public static final MapCodec<ShapedTemplate> CODEC = RAW_CODEC.flatXmap(pair -> {
        var key = pair.getFirst();
        var pattern = pair.getSecond();

        var width = pattern.get(0).length();
        var height = pattern.size();
        var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

        Set<String> notSeen = new HashSet<>(key.keySet());
        for (var y = 0; y < pattern.size(); ++y) {
            var row = pattern.get(y);

            for (var x = 0; x < row.length(); ++x) {
                var lookup = row.substring(x, x + 1);
                notSeen.remove(lookup);

                var ingredient = lookup.equals(" ") ? Ingredient.EMPTY : key.get(lookup);
                if (ingredient == null) {
                    return DataResult.error(() -> "Pattern references symbol '" + lookup + "' but it's not defined in the key");
                }

                ingredients.set(x + width * y, ingredient);
            }
        }

        if (!notSeen.isEmpty()) {
            return DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + notSeen);
        }

        return DataResult.success(new ShapedTemplate(width, height, ingredients));
    }, recipe -> DataResult.error(() -> "Serialisation not supported"));

    public static ShapedTemplate of(ShapedRecipe recipe) {
        return new ShapedTemplate(recipe.getWidth(), recipe.getHeight(), recipe.getIngredients());
    }

    public static ShapedTemplate fromNetwork(FriendlyByteBuf buffer) {
        var width = buffer.readVarInt();
        var height = buffer.readVarInt();
        var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (var i = 0; i < ingredients.size(); ++i) ingredients.set(i, Ingredient.fromNetwork(buffer));
        return new ShapedTemplate(width, height, ingredients);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(width());
        buffer.writeVarInt(height());
        for (var ingredient : ingredients) ingredient.toNetwork(buffer);
    }

}
