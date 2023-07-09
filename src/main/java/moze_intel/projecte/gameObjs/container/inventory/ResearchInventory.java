package moze_intel.projecte.gameObjs.container.inventory;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class ResearchInventory extends ItemStackHandler {

    public final Player player;
    public final IKnowledgeProvider provider;

    private ItemInfo curItem;
    private Component itemResearchFragments;
    private Component itemBaseEMC;
    private Component itemSellEMC;
    private Component itemBuyEMC;

    public ResearchInventory(Player player) {
        super(1);
        this.player = player;
        this.provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElseThrow(NullPointerException::new);
        curItem = ItemInfo.fromStack(getStackInSlot(0));

        updateClientTargets();
    }

    public boolean isServer() {
        return !player.getCommandSenderWorld().isClientSide;
    }

    /**
     * @apiNote Call on client only
     */
    public void itemResearchUpdated() {
        updateClientTargets();
    }

    public boolean hasItemInfoText() {
        return itemResearchFragments != null
            && itemBaseEMC != null
            && itemSellEMC != null
            && itemBuyEMC != null;
    }

    public Component getItemResearchFragments() {
        return itemResearchFragments;
    }

    public Component getItemBaseEMC() {
        return itemBaseEMC;
    }

    public Component getItemSellEMC() {
        return itemSellEMC;
    }

    public Component getItemBuyEMC() {
        return itemBuyEMC;
    }

    public void updateClientTargets() {
        if (isServer()) {
            return;
        }

        if (curItem.getItem() != Items.AIR) {
            int fragments = provider.getResearchFragments(curItem);
            int targetFragments = ProjectEConfig.server.difficulty.researchFragmentsPerItem.get();
            long baseEmc = EMCHelper.getEmcValue(curItem);
            long sellEmc = EMCHelper.getEmcSellValue(curItem, provider);
            long buyEmc = EMCHelper.getEmcBuyValue(curItem, provider);

            itemResearchFragments = PELang.RESEARCH_RESEARCH_FRAGMENTS.translateColored(
                ChatFormatting.BLACK,
                ChatFormatting.DARK_PURPLE, PELang.RESEARCH_FRACTION.translate(fragments, targetFragments)
            );
            itemBaseEMC = PELang.RESEARCH_BASE_EMC.translateColored(ChatFormatting.BLACK, ChatFormatting.BLUE, baseEmc);
            itemSellEMC = PELang.RESEARCH_SELL_EMC.translateColored(ChatFormatting.BLACK, ChatFormatting.BLUE, sellEmc);
            itemBuyEMC = PELang.RESEARCH_BUY_EMC.translateColored(ChatFormatting.BLACK, ChatFormatting.BLUE, buyEmc);
        } else {
            itemResearchFragments = null;
            itemBaseEMC = null;
            itemSellEMC = null;
            itemBuyEMC = null;
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        curItem = ItemInfo.fromStack(getStackInSlot(0));
        updateClientTargets();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return EMCHelper.doesItemHaveEmc(stack);
    }
}
