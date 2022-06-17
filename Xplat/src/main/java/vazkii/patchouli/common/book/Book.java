package vazkii.patchouli.common.book;

import com.google.gson.JsonObject;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.base.ClientAdvancements;
import vazkii.patchouli.client.book.*;
import vazkii.patchouli.common.base.PatchouliConfig;
import vazkii.patchouli.common.base.PatchouliSounds;
import vazkii.patchouli.common.item.ItemModBook;
import vazkii.patchouli.common.util.ItemStackUtil;
import vazkii.patchouli.common.util.SerializationUtil;
import vazkii.patchouli.xplat.XplatModContainer;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Book {

	private static final String[] ORDINAL_SUFFIXES = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
	private static final ResourceLocation DEFAULT_MODEL = new ResourceLocation(PatchouliAPI.MOD_ID, "book_brown");
	private static final ResourceLocation DEFAULT_BOOK_TEXTURE = new ResourceLocation(PatchouliAPI.MOD_ID, "textures/gui/book_brown.png");
	private static final ResourceLocation DEFAULT_FILLER_TEXTURE = new ResourceLocation(PatchouliAPI.MOD_ID, "textures/gui/page_filler.png");
	private static final ResourceLocation DEFAULT_CRAFTING_TEXTURE = new ResourceLocation(PatchouliAPI.MOD_ID, "textures/gui/crafting.png");
	private static final ResourceLocation UNICODE_FONT_ID = new ResourceLocation(PatchouliAPI.MOD_ID, "unicode_font");

	private static final Map<String, String> DEFAULT_MACROS = Util.make(() -> {
		Map<String, String> ret = new HashMap<>();
		ret.put("$(list", "$(li"); //  The lack of ) is intended
		ret.put("/$", "$()");
		ret.put("<br>", "$(br)");

		ret.put("$(item)", "$(#b0b)");
		ret.put("$(thing)", "$(#490)");
		return ret;
	});

	private BookContents contents;

	private boolean wasUpdated = false;

	public final XplatModContainer owner;
	public final ResourceLocation id;
	private final ItemStack bookItem;

	public final int textColor, headerColor, nameplateColor, linkColor, linkHoverColor, progressBarColor, progressBarBackground;

	public final boolean isExtension;
	public final List<Book> extensions = new LinkedList<>();
	@Nullable public Book extensionTarget;

	public final boolean isExternal;

	// JSON Loaded properties

	public final String name;
	public final String landingText;

	public final ResourceLocation bookTexture, fillerTexture, craftingTexture;

	public final ResourceLocation model;

	public final boolean useBlockyFont;

	public final ResourceLocation openSound, flipSound;

	public final boolean showProgress;

	public final String indexIconRaw;

	public final String version;
	public final String subtitle;

	public final String creativeTab;

	@Nullable public final ResourceLocation advancementsTab;

	public final boolean noBook;

	public final boolean showToasts;

	@Nullable public final ResourceLocation extensionTargetID;

	public final boolean allowExtensions;

	public final boolean pauseGame;

	public final boolean useResourcePack;

	public final boolean isPamphlet;

	public final boolean i18n;
	@Nullable public final PatchouliConfig.TextOverflowMode overflowMode;

	public final Map<String, String> macros = new HashMap<>();

	private static int parseColor(JsonObject root, String key, String defaultColor, boolean isExtension) {
		if (isExtension) {
			return 0;
		}

		return 0xFF000000 | Integer.parseInt(GsonHelper.getAsString(root, key, defaultColor), 16);
	}

	public Book(JsonObject root, XplatModContainer owner, ResourceLocation id, boolean external) {
		this.name = GsonHelper.getAsString(root, "name");
		this.landingText = GsonHelper.getAsString(root, "landing_text", "patchouli.gui.lexicon.landing_info");
		this.bookTexture = SerializationUtil.getAsResourceLocation(root, "book_texture", DEFAULT_BOOK_TEXTURE);
		this.fillerTexture = SerializationUtil.getAsResourceLocation(root, "filler_texture", DEFAULT_FILLER_TEXTURE);
		this.craftingTexture = SerializationUtil.getAsResourceLocation(root, "crafting_texture", DEFAULT_CRAFTING_TEXTURE);
		this.model = SerializationUtil.getAsResourceLocation(root, "model", DEFAULT_MODEL);
		this.useBlockyFont = GsonHelper.getAsBoolean(root, "use_blocky_font", false);

		this.owner = owner;
		this.id = id;
		this.isExternal = external;
		this.extensionTargetID = SerializationUtil.getAsResourceLocation(root, "extend", null);
		this.isExtension = extensionTargetID != null;
		this.textColor = parseColor(root, "text_color", "000000", isExtension);
		this.headerColor = parseColor(root, "header_color", "333333", isExtension);
		this.nameplateColor = parseColor(root, "nameplate_color", "FFDD00", isExtension);
		this.linkColor = parseColor(root, "link_color", "0000EE", isExtension);
		this.linkHoverColor = parseColor(root, "link_hover_color", "8800EE", isExtension);
		this.progressBarColor = parseColor(root, "progress_bar_color", "FFFF55", isExtension);
		this.progressBarBackground = parseColor(root, "progress_bar_background", "DDDDDD", isExtension);
		this.openSound = SerializationUtil.getAsResourceLocation(root, "open_sound", PatchouliSounds.BOOK_OPEN.getLocation());
		this.flipSound = SerializationUtil.getAsResourceLocation(root, "flip_sound", PatchouliSounds.BOOK_FLIP.getLocation());
		this.showProgress = GsonHelper.getAsBoolean(root, "show_progress", true);
		this.indexIconRaw = GsonHelper.getAsString(root, "index_icon", "");
		this.version = GsonHelper.getAsString(root, "version", "0");
		this.subtitle = GsonHelper.getAsString(root, "subtitle", "");
		this.creativeTab = GsonHelper.getAsString(root, "creative_tab", "misc");
		this.advancementsTab = SerializationUtil.getAsResourceLocation(root, "advancements_tab", null);
		this.noBook = GsonHelper.getAsBoolean(root, "dont_generate_book", false);
		this.showToasts = GsonHelper.getAsBoolean(root, "show_toasts", true);
		this.allowExtensions = GsonHelper.getAsBoolean(root, "allow_extensions", true);
		this.pauseGame = GsonHelper.getAsBoolean(root, "pause_game", false);
		this.useResourcePack = GsonHelper.getAsBoolean(root, "use_resource_pack", false);
		this.isPamphlet = GsonHelper.getAsBoolean(root, "pamphlet", false);
		this.i18n = GsonHelper.getAsBoolean(root, "i18n", false);
		this.overflowMode = SerializationUtil.getAsEnum(root, "text_overflow_mode", PatchouliConfig.TextOverflowMode.class, null);

		var customBookItem = GsonHelper.getAsString(root, "custom_book_item", "");
		if (noBook) {
			bookItem = ItemStackUtil.loadStackFromString(customBookItem);
		} else {
			bookItem = ItemModBook.forBook(this.id);
		}

		if (!isExtension) {
			macros.putAll(DEFAULT_MACROS);
			for (var e : GsonHelper.getAsJsonObject(root, "macros", new JsonObject()).entrySet()) {
				macros.put(e.getKey(), GsonHelper.convertToString(e.getValue(), "macro value"));
			}
		}
	}

	public String getModNamespace() {
		return id.getNamespace();
	}

	public ItemStack getBookItem() {
		return bookItem;
	}

	public void markUpdated() {
		wasUpdated = true;
	}

	public boolean popUpdated() {
		boolean updated = wasUpdated;
		wasUpdated = false;
		return updated;
	}

	/** Must only be called on client */
	@Deprecated
	public void reloadContents() {
		reloadContents(false);
	}

	/**
	 * Must only be called on client
	 * 
	 * @param singleBook Hint that the book was reloaded through the button on the main page
	 */
	public void reloadContents(boolean singleBook) {
		if (!isExtension) {
			BookContentsBuilder builder = new BookContentsBuilder(singleBook);
			try {
				builder.loadFrom(this);
			} catch (Exception e) {
				PatchouliAPI.LOGGER.error("Error loading book {}, using empty contents and ignoring extensions", id, e);
				contents = BookContents.empty(this, e);
			}

			for (Book extension : extensions) {
				try {
					builder.loadFrom(extension);
				} catch (Exception e) {
					PatchouliAPI.LOGGER.error("Error loading extending book {} with addon book {}, skipping", id, extension.id, e);
				}
			}

			try {
				contents = builder.build(this);
			} catch (Exception e) {
				PatchouliAPI.LOGGER.error("Error compiling book {}, using empty contents", id, e);
				contents = BookContents.empty(this, e);
			}
		}
	}

	public final boolean advancementsEnabled() {
		return !PatchouliConfig.get().disableAdvancementLocking().get()
				&& !PatchouliConfig.get().noAdvancementBooks().get().contains(id.toString());
	}

	public void reloadLocks(boolean suppressToasts) {
		getContents().entries.values().forEach(BookEntry::updateLockStatus);
		getContents().categories.values().forEach(c -> c.updateLockStatus(true));

		boolean updated = popUpdated();
		if (updated && !suppressToasts && advancementsEnabled() && showToasts) {
			ClientAdvancements.sendBookToast(this);
		}
	}

	public String getOwnerName() {
		return owner.getName();
	}

	public Style getFontStyle() {
		if (useBlockyFont) {
			return Style.EMPTY;
		} else {
			return Style.EMPTY.withFont(UNICODE_FONT_ID);
		}
	}

	public MutableComponent getSubtitle() {
		Component editionStr;

		try {
			int ver = Integer.parseInt(version);
			if (ver == 0) {
				return new TranslatableComponent(subtitle);
			}

			editionStr = new TextComponent(numberToOrdinal(ver));
		} catch (NumberFormatException e) {
			editionStr = new TranslatableComponent("patchouli.gui.lexicon.dev_edition");
		}

		return new TranslatableComponent("patchouli.gui.lexicon.edition_str", editionStr);
	}

	public BookIcon getIcon() {
		if (indexIconRaw == null || indexIconRaw.isEmpty()) {
			return new BookIcon.StackIcon(getBookItem());
		} else {
			return BookIcon.from(indexIconRaw);
		}
	}

	private static String numberToOrdinal(int i) {
		return i % 100 == 11 || i % 100 == 12 || i % 100 == 13 ? i + "th" : i + ORDINAL_SUFFIXES[i % 10];
	}

	public BookContents getContents() {
		if (isExtension) {
			return extensionTarget.getContents();
		} else {
			return contents != null ? contents : BookContents.empty(this, null);
		}
	}
}
