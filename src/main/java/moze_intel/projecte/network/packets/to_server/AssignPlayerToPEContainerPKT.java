package moze_intel.projecte.network.packets.to_server;

import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.container.ResearchContainer;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public record AssignPlayerToPEContainerPKT(boolean isPlayerAssigned) implements IPEPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.containerMenu instanceof CondenserContainer container) {
            container.getBlockEntitySafe().setAssociatedPlayerServer(player, isPlayerAssigned);
            container.broadcastFullState();
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isPlayerAssigned);
    }

    public static AssignPlayerToPEContainerPKT decode(FriendlyByteBuf buf) {
        return new AssignPlayerToPEContainerPKT(buf.readBoolean());
    }
}
