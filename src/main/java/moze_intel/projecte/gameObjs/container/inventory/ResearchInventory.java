package moze_intel.projecte.gameObjs.container.inventory;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.gameObjs.container.ResearchContainer;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.to_client.ResearchResultPKT;
import moze_intel.projecte.network.packets.to_server.ConsumeResearchItemsPKT;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class ResearchInventory extends ItemStackHandler {

    /**
     * How many times temporary labels in a gui will be drawn before not
     * being drawn again.
     */
    private static final int GUI_MESSAGE_TIME = 300;

    public final Player player;
    public final IKnowledgeProvider provider;

    private Component itemResearchFragments;
    private Component itemBaseEMC;
    private Component itemSellEMC;
    private Component itemBuyEMC;
    private Component itemResearchResultText;
    public int researchResultFlag = 0;

    public ResearchInventory(Player player) {
        super(1);
        this.player = player;
        this.provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElseThrow(NullPointerException::new);

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

    public Component getResearchResults() { return itemResearchResultText; }

    /**
     * Call on client only.
     *
     * Call when the user clicks the consume button in the research table.
     */
    public void onClickConsume() {
        PacketHandler.sendToServer(new ConsumeResearchItemsPKT());
    }

    /**
     * Call on client only.
     */
    public void onReceivedResearchResults(int newFragmentsReceived, boolean stoppedBecauseHitMaxFragments) {
        researchResultFlag = GUI_MESSAGE_TIME;
        if (newFragmentsReceived > 0) {
            itemResearchResultText = PELang.RESEARCH_RESULT.translateColored(ChatFormatting.BLACK, ChatFormatting.DARK_PURPLE, newFragmentsReceived);
        } else if (stoppedBecauseHitMaxFragments) {
            itemResearchResultText = PELang.RESEARCH_RESULT_MAX.translateColored(ChatFormatting.RED);
        } else {
            itemResearchResultText = PELang.RESEARCH_RESULT_FAILURE.translateColored(ChatFormatting.BLACK, ChatFormatting.DARK_PURPLE, newFragmentsReceived);
        }

        updateClientTargets();
    }

    /**
     * Call on server only.
     */
    public void tryConsumeResearchItems() {
        ItemStack stack = getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        EMCHelper.ResearchResult researchResult = EMCHelper.getResearchResults(stack, provider);

        // send result to client to display feedback
        PacketHandler.sendTo(new ResearchResultPKT(researchResult), (ServerPlayer) player);

        if (researchResult.isEmpty()) {
            return;
        }

        // update stack
        ItemStack newStack = stack.copy();
        newStack.setCount(stack.getCount() - researchResult.itemsConsumed());
        setStackInSlot(0, newStack);

        // update researchFragments
        if (researchResult.researchFragmentsReceived() > 0) {
            ItemInfo item = ItemInfo.fromStack(stack);
            int oldFragments = provider.getResearchFragments(item);
            provider.setResearchFragments(item, oldFragments + researchResult.researchFragmentsReceived());

            provider.syncResearchFragmentChange((ServerPlayer) player, NBTManager.getPersistentInfo(item));
        }
    }

    /**
     * Call on server only.
     */
    public void onCloseContainer() {
        // give items back to the player
        ItemHandlerHelper.giveItemToPlayer(player, getStackInSlot(0));
    }

    public void updateClientTargets() {
        if (isServer()) {
            return;
        }

        ItemStack stack = getStackInSlot(0);
        if (!stack.isEmpty()) {
            ItemInfo curItem = ItemInfo.fromStack(stack);
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
        updateClientTargets();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return EMCHelper.doesItemHaveEmc(stack);
    }
}
