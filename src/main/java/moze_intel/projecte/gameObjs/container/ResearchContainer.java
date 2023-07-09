package moze_intel.projecte.gameObjs.container;

import moze_intel.projecte.gameObjs.container.inventory.ResearchInventory;
import moze_intel.projecte.gameObjs.container.slots.ResearchSlot;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class ResearchContainer extends PEContainer {

    public final ResearchInventory researchInventory;

    public static ResearchContainer fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        return new ResearchContainer(windowId, playerInv);
    }

    public ResearchContainer(int windowId, Inventory playerInv) {
        super(PEContainerTypes.RESEARCH_CONTAINER,  windowId, playerInv);
        this.researchInventory = new ResearchInventory(this.playerInv.player);
        initSlots();
    }

    private void initSlots() {
        this.addSlot(new ResearchSlot(researchInventory, 0, 30, 27));
        addPlayerInventory(35, 117);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
