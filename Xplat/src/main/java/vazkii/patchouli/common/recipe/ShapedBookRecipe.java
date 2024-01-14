package vazkii.patchouli.common.recipe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import org.apache.commons.lang3.NotImplementedException;

import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.common.item.PatchouliItems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recipe type for shaped book recipes.
 * The format is the same as vanilla shaped recipes, but the
 * "result" object is replaced by a "book" string for the book ID.
 */
public class ShapedBookRecipe extends ShapedRecipe {
	public static final RecipeSerializer<ShapedBookRecipe> SERIALIZER = new Serializer();

	private final ItemStack result;

	@Nullable private final ResourceLocation outputBook;

	public ShapedBookRecipe(
			String group,
			int width, int height,
			NonNullList<Ingredient> recipePattern, ItemStack result, @Nullable ResourceLocation outputBook) {
		super(group, CraftingBookCategory.MISC, width, height, recipePattern, getOutputBook(result, outputBook));
		this.result = result;
		this.outputBook = outputBook;
	}

	private static ItemStack getOutputBook(ItemStack result, @Nullable ResourceLocation outputBook) {
		if (outputBook != null) {
			return PatchouliAPI.get().getBookStack(outputBook);
		}
		return result;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	public static class Serializer implements RecipeSerializer<ShapedBookRecipe> {
		static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().flatXmap($$0 -> {
			if ($$0.size() > 3) {
				return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
			} else if ($$0.isEmpty()) {
				return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
			} else {
				int $$1 = $$0.get(0).length();

				for (String $$2 : $$0) {
					if ($$2.length() > 3) {
						return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
					}

					if ($$1 != $$2.length()) {
						return DataResult.error(() -> "Invalid pattern: each row must be the same width");
					}
				}

				return DataResult.success($$0);
			}
		}, DataResult::success);
		static final Codec<String> SINGLE_CHARACTER_STRING_CODEC = Codec.STRING.flatXmap($$0 -> {
			if ($$0.length() != 1) {
				return DataResult.error(() -> "Invalid key entry: '" + $$0 + "' is an invalid symbol (must be 1 character only).");
			} else {
				return " ".equals($$0) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success($$0);
			}
		}, DataResult::success);
		private static final Codec<ShapedBookRecipe> CODEC = ShapedBookRecipe.Serializer.RawShapedRecipe.CODEC.flatXmap($$0 -> {
			String[] $$1 = ShapedBookRecipe.shrink($$0.pattern);
			int $$2 = $$1[0].length();
			int $$3 = $$1.length;
			NonNullList<Ingredient> $$4 = NonNullList.withSize($$2 * $$3, Ingredient.EMPTY);
			Set<String> $$5 = Sets.newHashSet($$0.key.keySet());

			for (int $$6 = 0; $$6 < $$1.length; ++$$6) {
				String $$7 = $$1[$$6];

				for (int $$8 = 0; $$8 < $$7.length(); ++$$8) {
					String $$9 = $$7.substring($$8, $$8 + 1);
					Ingredient $$10 = $$9.equals(" ") ? Ingredient.EMPTY : $$0.key.get($$9);
					if ($$10 == null) {
						return DataResult.error(() -> "Pattern references symbol '" + $$9 + "' but it's not defined in the key");
					}

					$$5.remove($$9);
					$$4.set($$8 + $$2 * $$6, $$10);
				}
			}

			if (!$$5.isEmpty()) {
				return DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + $$5);
			} else {
				ShapedBookRecipe $$11 = new ShapedBookRecipe($$0.group, $$2, $$3, $$4, $$0.result, $$0.outputBook);
				return DataResult.success($$11);
			}
		}, $$0 -> {
			throw new NotImplementedException("Serializing ShapedRecipe is not implemented yet.");
		});

		@Override
		public Codec<ShapedBookRecipe> codec() {
			return CODEC;
		}

		@NotNull
		@Override
		public ShapedBookRecipe fromNetwork(FriendlyByteBuf buf) {
			int width = buf.readVarInt();
			int height = buf.readVarInt();
			String group = buf.readUtf();
			NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

			ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buf));

			ItemStack result = buf.readItem();
			final var hasOutputBook = buf.readBoolean();
			ResourceLocation outputBook = null;
			if (hasOutputBook) {
				outputBook = buf.readResourceLocation();
			}
			return new ShapedBookRecipe(group, width, height, ingredients, result, outputBook);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, ShapedBookRecipe recipe) {
			buf.writeVarInt(recipe.getWidth());
			buf.writeVarInt(recipe.getHeight());
			buf.writeUtf(recipe.getGroup());

			for (Ingredient ingredient : recipe.getIngredients()) {
				ingredient.toNetwork(buf);
			}

			buf.writeItem(recipe.result);
			final var outputBook = recipe.outputBook;
			buf.writeBoolean(outputBook != null);
			if (outputBook != null) {
				buf.writeResourceLocation(outputBook);
			}
		}

		record RawShapedRecipe(
				String group, Map<String, Ingredient> key, List<String> pattern, ItemStack result,
				@Nullable ResourceLocation outputBook) {
			public static final Codec<ShapedBookRecipe.Serializer.RawShapedRecipe> CODEC = RecordCodecBuilder.create(
					$$0 -> $$0.group(
							ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter($$0x -> $$0x.group),
							ExtraCodecs.strictUnboundedMap(ShapedBookRecipe.Serializer.SINGLE_CHARACTER_STRING_CODEC, Ingredient.CODEC_NONEMPTY)
									.fieldOf("key")
									.forGetter($$0x -> $$0x.key),
							ShapedBookRecipe.Serializer.PATTERN_CODEC.fieldOf("pattern").forGetter($$0x -> $$0x.pattern),
							ExtraCodecs.strictOptionalField(CraftingRecipeCodecs.ITEMSTACK_OBJECT_CODEC, "result", new ItemStack(PatchouliItems.BOOK)).forGetter($$0x -> $$0x.result),
							ExtraCodecs.strictOptionalField(ResourceLocation.CODEC, "book", null).forGetter(bookRecipe -> bookRecipe.outputBook)
					)
							.apply($$0, ShapedBookRecipe.Serializer.RawShapedRecipe::new)
			);
		}
	}

	private static int firstNonSpace(String $$0) {
		int $$1 = 0;

		while ($$1 < $$0.length() && $$0.charAt($$1) == ' ') {
			++$$1;
		}

		return $$1;
	}

	private static int lastNonSpace(String $$0) {
		int $$1 = $$0.length() - 1;

		while ($$1 >= 0 && $$0.charAt($$1) == ' ') {
			--$$1;
		}

		return $$1;
	}

	@VisibleForTesting
	static String[] shrink(List<String> $$0) {
		int $$1 = Integer.MAX_VALUE;
		int $$2 = 0;
		int $$3 = 0;
		int $$4 = 0;

		for (int $$5 = 0; $$5 < $$0.size(); ++$$5) {
			String $$6 = $$0.get($$5);
			$$1 = Math.min($$1, firstNonSpace($$6));
			int $$7 = lastNonSpace($$6);
			$$2 = Math.max($$2, $$7);
			if ($$7 < 0) {
				if ($$3 == $$5) {
					++$$3;
				}

				++$$4;
			} else {
				$$4 = 0;
			}
		}

		if ($$0.size() == $$4) {
			return new String[0];
		} else {
			String[] $$8 = new String[$$0.size() - $$4 - $$3];

			for (int $$9 = 0; $$9 < $$8.length; ++$$9) {
				$$8[$$9] = $$0.get($$9 + $$3).substring($$1, $$2 + 1);
			}

			return $$8;
		}
	}
}
