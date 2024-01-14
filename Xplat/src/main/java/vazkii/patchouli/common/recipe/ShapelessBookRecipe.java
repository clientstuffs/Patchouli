package vazkii.patchouli.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.common.item.PatchouliItems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Recipe type for shapeless book recipes.
 * The format is the same as vanilla shapeless recipes, but the
 * "result" object is replaced by a "book" string for the book ID.
 */
public class ShapelessBookRecipe extends ShapelessRecipe {
	public static final RecipeSerializer<ShapelessBookRecipe> SERIALIZER = new Serializer();

	private final ItemStack result;
	@Nullable private final ResourceLocation outputBook;

	public ShapelessBookRecipe(
			String group,
			ItemStack result, @Nullable ResourceLocation outputBook,
			NonNullList<Ingredient> ingredients) {
		super(group, CraftingBookCategory.MISC, getOutputBook(result, outputBook), ingredients);
		this.result = result;
		this.outputBook = outputBook;
	}

	private static ItemStack getOutputBook(ItemStack result, @Nullable ResourceLocation outputBook) {
		if (outputBook != null) {
			return PatchouliAPI.get().getBookStack(outputBook);
		}
		// The vanilla ShapelessRecipe implementation never uses the passed RegistryAccess, so this is ok.
		return result;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	public static class Serializer implements RecipeSerializer<ShapelessBookRecipe> {
		private static final Codec<ShapelessBookRecipe> CODEC = RecordCodecBuilder.create(
				$$0 -> $$0.group(
						ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(ShapelessRecipe::getGroup),
						ExtraCodecs.strictOptionalField(CraftingRecipeCodecs.ITEMSTACK_OBJECT_CODEC, "result", new ItemStack(PatchouliItems.BOOK)).forGetter($$0x -> $$0x.result),
						ExtraCodecs.strictOptionalField(ResourceLocation.CODEC, "book", null).forGetter(bookRecipe -> bookRecipe.outputBook),
						Ingredient.CODEC_NONEMPTY
								.listOf()
								.fieldOf("ingredients")
								.flatXmap(
										$$0x -> {
											Ingredient[] $$1 = $$0x.stream().filter($$0xx -> !$$0xx.isEmpty()).toArray(Ingredient[]::new);
											if ($$1.length == 0) {
												return DataResult.error(() -> "No ingredients for shapeless recipe");
											} else {
												return $$1.length > 9
														? DataResult.error(() -> "Too many ingredients for shapeless recipe")
														: DataResult.success(NonNullList.of(Ingredient.EMPTY, $$1));
											}
										},
										DataResult::success
								)
								.forGetter(ShapelessRecipe::getIngredients)
				)
						.apply($$0, ShapelessBookRecipe::new)
		);

		@Override
		public Codec<ShapelessBookRecipe> codec() {
			return CODEC;
		}

		@NotNull
		@Override
		public ShapelessBookRecipe fromNetwork(FriendlyByteBuf buf) {
			String group = buf.readUtf();
			int $$3 = buf.readVarInt();
			NonNullList<Ingredient> ingredients = NonNullList.withSize($$3, Ingredient.EMPTY);

			ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buf));

			ItemStack result = buf.readItem();
			final var hasOutputBook = buf.readBoolean();
			ResourceLocation outputBook = null;
			if (hasOutputBook) {
				outputBook = buf.readResourceLocation();
			}
			return new ShapelessBookRecipe(group, result, outputBook, ingredients);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, ShapelessBookRecipe recipe) {
			buf.writeUtf(recipe.getGroup());
			buf.writeVarInt(recipe.getIngredients().size());

			for (Ingredient $$2 : recipe.getIngredients()) {
				$$2.toNetwork(buf);
			}

			buf.writeItem(recipe.result);
			final var outputBook = recipe.outputBook;
			buf.writeBoolean(outputBook != null);
			if (outputBook != null) {
				buf.writeResourceLocation(outputBook);
			}
		}
	}
}
