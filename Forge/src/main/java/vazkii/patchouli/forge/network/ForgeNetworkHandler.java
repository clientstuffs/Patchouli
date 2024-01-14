package vazkii.patchouli.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

import vazkii.patchouli.api.PatchouliAPI;

public class ForgeNetworkHandler {
	private static final int PROTOCOL_VERSION = 1;
	public static final SimpleChannel CHANNEL = ChannelBuilder
			.named(new ResourceLocation(PatchouliAPI.MOD_ID, "main"))
			.networkProtocolVersion(PROTOCOL_VERSION)
			.clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
			.serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
			.simpleChannel();

	public static void registerMessages() {
		// ForgeMessageOpenBookGui::encode, ,
		CHANNEL
				.messageBuilder(ForgeMessageOpenBookGui.class)
				.encoder(ForgeMessageOpenBookGui::encode)
				.decoder(ForgeMessageOpenBookGui::decode)
				.consumerMainThread((book, context) -> book.handle(() -> context));
		CHANNEL
				.messageBuilder(ForgeMessageReloadBookContents.class)
				.encoder(ForgeMessageReloadBookContents::encode)
				.decoder(ForgeMessageReloadBookContents::decode)
				.consumerMainThread((contents, context) -> contents.handle(() -> context));
	}
}
