package moze_intel.projecte.network.packets.to_server;

import moze_intel.projecte.gameObjs.container.ResearchContainer;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public record ConsumeResearchItemsPKT() implements IPEPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        Player player = context.getSender();
        if (player != null && player.containerMenu instanceof ResearchContainer container) {
            container.researchInventory.tryConsumeResearchItems();
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {}

    public static ConsumeResearchItemsPKT decode(FriendlyByteBuf buf) {
        return new ConsumeResearchItemsPKT();
    }
}
