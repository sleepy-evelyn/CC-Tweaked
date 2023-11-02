// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.JsonObject;
import dan200.computercraft.annotations.ForgeOverride;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Adapter for recipes which overrides the serializer and adds custom item NBT.
 */
final class RecipeWrapper implements RecipeOutput {
    private final RecipeOutput output;
    private final RecipeSerializer<?> serializer;
    private final List<Consumer<JsonObject>> extend = new ArrayList<>(0);

    RecipeWrapper(RecipeOutput output, RecipeSerializer<?> serializer) {
        this.output = output;
        this.serializer = serializer;
    }

    public static RecipeWrapper wrap(RecipeSerializer<?> serializer, RecipeOutput original) {
        return new RecipeWrapper(original, serializer);
    }

    public RecipeWrapper withExtraData(Consumer<JsonObject> extra) {
        extend.add(extra);
        return this;
    }

    public RecipeWrapper withResultTag(@Nullable CompoundTag resultTag) {
        if (resultTag == null) return this;

        extend.add(json -> {
            var object = GsonHelper.getAsJsonObject(json, "result");
            object.addProperty("nbt", resultTag.toString());
        });
        return this;
    }

    public RecipeWrapper withResultTag(Consumer<CompoundTag> resultTag) {
        var tag = new CompoundTag();
        resultTag.accept(tag);
        return withResultTag(tag);
    }

    @Override
    public void accept(FinishedRecipe finishedRecipe) {
        output.accept(new RecipeImpl(finishedRecipe, serializer, extend));
    }

    @Override
    public Advancement.Builder advancement() {
        return output.advancement();
    }

    @ForgeOverride
    public HolderLookup.Provider provider() {
        return HolderLookup.Provider.create(Stream.empty());
    }

    private record RecipeImpl(
        FinishedRecipe recipe, RecipeSerializer<?> type, List<Consumer<JsonObject>> extend
    ) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject jsonObject) {
            recipe.serializeRecipeData(jsonObject);
            for (var extender : extend) extender.accept(jsonObject);
        }

        @Override
        public ResourceLocation id() {
            return recipe.id();
        }

        @Nullable
        @Override
        public AdvancementHolder advancement() {
            return recipe.advancement();
        }
    }
}
