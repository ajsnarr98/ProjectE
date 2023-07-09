package moze_intel.projecte.network.packets.to_client.knowledge;

import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.container.ResearchContainer;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record KnowledgeSyncResearchChangePKT(ItemInfo change, int numFragments) implements IPEPacket {

	@Override
	public void handle(NetworkEvent.Context context) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).ifPresent(cap -> {
				boolean researchFragmentsChanged = cap.setResearchFragments(change, numFragments);
				if (researchFragmentsChanged && player.containerMenu instanceof TransmutationContainer container) {
					container.transmutationInventory.itemResearchUpdated();
				} else if (researchFragmentsChanged && player.containerMenu instanceof ResearchContainer container) {
					container.researchInventory.itemResearchUpdated();
				}
			});
		}
		PECore.debugLog("** RECEIVED TRANSMUTATION KNOWLEDGE CHANGE DATA CLIENTSIDE **");
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeRegistryId(change.getItem());
		buffer.writeNbt(change.getNBT());
		buffer.writeInt(numFragments);
	}

	public static KnowledgeSyncResearchChangePKT decode(FriendlyByteBuf buffer) {
		return new KnowledgeSyncResearchChangePKT(ItemInfo.fromItem(buffer.readRegistryId(), buffer.readNbt()), buffer.readInt());
	}
}