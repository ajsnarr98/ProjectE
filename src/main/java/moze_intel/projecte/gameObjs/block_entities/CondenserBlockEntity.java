package moze_intel.projecte.gameObjs.block_entities;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.event.PlayerAttemptCondenserSetEvent;
import moze_intel.projecte.api.event.PlayerResearchChangeEvent;
import moze_intel.projecte.api.proxy.ITransmutationProxy;
import moze_intel.projecte.capability.managing.BasicCapabilityResolver;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.events.PlayerEvents;
import moze_intel.projecte.gameObjs.container.CondenserContainer;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CondenserBlockEntity extends EmcChestBlockEntity implements PlayerEvents.PlayerResearchChangeListener {

	protected final ItemStackHandler inputInventory = createInput();
	private final ItemStackHandler outputInventory = createOutput();
	@Nullable private UUID associatedPlayer = null;
	private String associatedPlayerName = "";
	@Nullable private IKnowledgeProvider knowledgeProvider = null;
	@Nullable
	private ItemInfo lockInfo;
	private boolean isAcceptingEmc;
	public long displayEmc;
	public long requiredEmc;
	//Start at one less than actual just to ensure we run initially after loading
	private int loadIndex = EMCMappingHandler.getLoadIndex() - 1;

	private List<Component> tooltipComponents = new ArrayList<>(4);

	private final ITransmutationProxy transmutationProxy = ProjectEAPI.getTransmutationProxy();

	public CondenserBlockEntity(BlockPos pos, BlockState state) {
		this(PEBlockEntityTypes.CONDENSER, pos, state);
	}

	public List<Component> getTooltipComponents() {
		return tooltipComponents;
	}

	protected CondenserBlockEntity(BlockEntityTypeRegistryObject<? extends CondenserBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		itemHandlerResolver = BasicCapabilityResolver.getBasicItemHandlerResolver(this::createAutomationInventory);
		onAssociatedPlayerChange();
		PlayerEvents.addWeakReferencePlayerResearchChangeListener(this);
	}

	/**
	 * Call when the player changes, or when the associated player connects.
	 */
	private synchronized void onAssociatedPlayerChange() {
		if (associatedPlayer != null) {
			knowledgeProvider = transmutationProxy.getKnowledgeProviderFor(associatedPlayer);
		} else {
			knowledgeProvider = null;
		}
		if (level != null && !level.isClientSide) {
			checkLockAndUpdate(true);
			markDirty(false);
		} else {
			updateClientTargets();
		}
	}

	/**
	 * Call from client only.
	 */
	public void setAssociatedPlayerClient(@Nullable LocalPlayer player) {
		if (player != null) {
			this.associatedPlayer = player.getUUID();
			this.associatedPlayerName = player.getDisplayName().getString();
		} else {
			this.associatedPlayer = null;
			this.associatedPlayerName = "";
		}
		onAssociatedPlayerChange();
	}

	/**
	 * Call from the server only.
	 */
	public void setAssociatedPlayerServer(@NotNull ServerPlayer player, boolean isNowAssociated) {
		if (isNowAssociated) {
			this.associatedPlayer = player.getUUID();
			this.associatedPlayerName = player.getDisplayName().getString();
		} else {
			this.associatedPlayer = null;
			this.associatedPlayerName = "";
		}
		setChanged();
		onAssociatedPlayerChange();
	}

	private void updateClientTargets() {
		tooltipComponents.clear();
		if (associatedPlayer == null) {
			tooltipComponents.add(PELang.CONDENSER_USE_PLAYER_RESEARCH_TOOLTIP.translate("false"));
		} else {
			tooltipComponents.add(PELang.CONDENSER_USING_PLAYER_RESEARCH_TOOLTIP.translate());
			MutableComponent playerNameLine = new TextComponent(associatedPlayerName);
			playerNameLine.setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
			tooltipComponents.add(playerNameLine);
		}
	}

	@Override
	protected boolean canAcceptEmc() {
		return isAcceptingEmc;
	}

	@Override
	protected boolean canProvideEmc() {
		return false;
	}

	public boolean hasAssignedPlayerForResearch() {
		return associatedPlayer != null;
	}

	@Nullable
	public final ItemInfo getLockInfo() {
		if (requiredEmc == 0 && (level == null || !level.isClientSide)) {
			//If the lock doesn't have EMC don't tell the client it is there
			return null;
		}
		return lockInfo;
	}

	public ItemStackHandler getInput() {
		return inputInventory;
	}

	public ItemStackHandler getOutput() {
		return outputInventory;
	}

	protected ItemStackHandler createInput() {
		return new StackHandler(91);
	}

	protected ItemStackHandler createOutput() {
		return inputInventory;
	}

	@NotNull
	protected IItemHandler createAutomationInventory() {
		return new WrappedItemHandler(inputInventory, WrappedItemHandler.WriteMode.IN_OUT) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return SlotPredicates.HAS_EMC.test(stack) && !isStackEqualToLock(stack) ? super.insertItem(slot, stack, simulate) : stack;
			}

			@NotNull
			@Override
			public ItemStack extractItem(int slot, int max, boolean simulate) {
				if (!getStackInSlot(slot).isEmpty() && isStackEqualToLock(getStackInSlot(slot))) {
					return super.extractItem(slot, max, simulate);
				}
				return ItemStack.EMPTY;
			}
		};
	}

	public static void tickServer(Level level, BlockPos pos, BlockState state, CondenserBlockEntity condenser) {
		condenser.checkLockAndUpdate(false);
		condenser.displayEmc = condenser.getStoredEmc();
		if (condenser.getLockInfo() != null) {
			condenser.condense();
		}
		condenser.updateComparators();
	}

	@Override
	public void onResearchChangeEvent(PlayerResearchChangeEvent event) {
		if (associatedPlayer != null && associatedPlayer.equals(event.getPlayerUUID())) {
			onAssociatedPlayerChange();
		}
	}

	private void checkLockAndUpdate(boolean force) {
		if (!force && loadIndex == EMCMappingHandler.getLoadIndex()) {
			//Only update if we are forcing it or are on a different load index
			return;
		}
		loadIndex = EMCMappingHandler.getLoadIndex();
		if (lockInfo != null) {
			long lockEmc = EMCHelper.getEmcBuyValue(lockInfo, knowledgeProvider);
			if (lockEmc > 0) {
				if (requiredEmc != lockEmc) {
					requiredEmc = lockEmc;
					this.isAcceptingEmc = true;
				}
				return;
			}
			//Don't reset the lockInfo just because it has no EMC, as if a reload makes it have EMC again
			// then we want to allow it to happen again
		}
		displayEmc = 0;
		requiredEmc = 0;
		this.isAcceptingEmc = false;
	}

	protected void condense() {
		for (int i = 0; i < inputInventory.getSlots(); i++) {
			ItemStack stack = inputInventory.getStackInSlot(i);
			if (!stack.isEmpty() && !isStackEqualToLock(stack)) {
				inputInventory.extractItem(i, 1, false);
				forceInsertEmc(EMCHelper.getEmcSellValue(stack, null), EmcAction.EXECUTE);
				break;
			}
		}
		if (this.getStoredEmc() >= requiredEmc && this.hasSpace()) {
			forceExtractEmc(requiredEmc, EmcAction.EXECUTE);
			pushStack();
		}
	}

	protected final void pushStack() {
		ItemInfo lockInfo = getLockInfo();
		if (lockInfo != null) {
			ItemHandlerHelper.insertItemStacked(outputInventory, lockInfo.createStack(), false);
		}
	}

	protected boolean hasSpace() {
		for (int i = 0; i < outputInventory.getSlots(); i++) {
			ItemStack stack = outputInventory.getStackInSlot(i);
			if (stack.isEmpty() || (isStackEqualToLock(stack) && stack.getCount() < stack.getMaxStackSize())) {
				return true;
			}
		}
		return false;
	}

	public boolean isStackEqualToLock(ItemStack stack) {
		ItemInfo lockInfo = getLockInfo();
		if (lockInfo == null || stack.isEmpty()) {
			return false;
		}
		//Compare our lock to the persistent info that the stack would have
		return lockInfo.equals(NBTManager.getPersistentInfo(ItemInfo.fromStack(stack)));
	}

	public void setLockInfoFromPacket(@Nullable ItemInfo lockInfo) {
		this.lockInfo = lockInfo;
	}

	public boolean attemptCondenserSet(Player player) {
		if (level == null || level.isClientSide) {
			return false;
		}
		if (getLockInfo() == null) {
			ItemStack stack = player.containerMenu.getCarried();
			if (!stack.isEmpty()) {
				ItemInfo sourceInfo = ItemInfo.fromStack(stack);
				ItemInfo reducedInfo = NBTManager.getPersistentInfo(sourceInfo);
				if (!MinecraftForge.EVENT_BUS.post(new PlayerAttemptCondenserSetEvent(player, sourceInfo, reducedInfo))) {
					lockInfo = reducedInfo;
					checkLockAndUpdate(true);
					markDirty(false);
					return true;
				}
				return false;
			}
			//If the lock info is actually null and the player didn't carry anything don't do anything
			// otherwise just fall through as we need to update it to actually being empty
			if (lockInfo == null) {
				return false;
			}
		}
		lockInfo = null;
		checkLockAndUpdate(true);
		markDirty(false);
		return true;
	}

	@Override
	public void load(@NotNull CompoundTag nbt) {
		super.load(nbt);
		inputInventory.deserializeNBT(nbt.getCompound("Input"));
		lockInfo = ItemInfo.read(nbt.getCompound("LockInfo"));
		if (nbt.hasUUID("AssociatedPlayer")) {
			associatedPlayer = nbt.getUUID("AssociatedPlayer");
		}
		associatedPlayerName = nbt.getString("AssociatedPlayerName");
		onAssociatedPlayerChange();
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("Input", inputInventory.serializeNBT());
		if (lockInfo != null) {
			tag.put("LockInfo", lockInfo.write(new CompoundTag()));
		}
		if (associatedPlayer != null) {
			tag.putUUID("AssociatedPlayer", associatedPlayer);
		}
		tag.putString("AssociatedPlayerName", associatedPlayerName);
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player playerIn) {
		return new CondenserContainer(windowId, playerInventory, this);
	}

	@NotNull
	@Override
	public Component getDisplayName() {
		return TextComponentUtil.build(PEBlocks.CONDENSER);
	}
}