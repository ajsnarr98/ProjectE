package moze_intel.projecte.network.packets.to_client;

import moze_intel.projecte.gameObjs.container.ResearchContainer;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public record ResearchResultPKT(int newFragmentsReceived, boolean stoppedBecauseHitMaxFragments) implements IPEPacket {

    public ResearchResultPKT(EMCHelper.ResearchResult result) {
        this(result.researchFragmentsReceived(), result.stoppedBecauseMaxFragments());
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ResearchContainer container) {
            container.researchInventory.onReceivedResearchResults(newFragmentsReceived, stoppedBecauseHitMaxFragments);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(newFragmentsReceived);
        buffer.writeBoolean(stoppedBecauseHitMaxFragments);
    }

    public static ResearchResultPKT decode(FriendlyByteBuf buf) {
        int fragments = buf.readInt();
        boolean stoppedBecauseHitMaxFragments = buf.readBoolean();
        return new ResearchResultPKT(fragments, stoppedBecauseHitMaxFragments);
    }
}
