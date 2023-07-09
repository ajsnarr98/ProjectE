package moze_intel.projecte.gameObjs.container.slots;

import moze_intel.projecte.gameObjs.container.inventory.ResearchInventory;
import net.minecraftforge.items.IItemHandler;

public class ResearchSlot extends InventoryContainerSlot {

    private final ResearchInventory inv;

    public ResearchSlot(ResearchInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
        this.inv = inv;
    }
}
