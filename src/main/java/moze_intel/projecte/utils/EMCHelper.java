package moze_intel.projecte.utils;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.gameObjs.items.KleinStar;
import moze_intel.projecte.utils.text.ILangEntry;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * Helper class for EMC. Notice: Please try to keep methods tidy and alphabetically ordered. Thanks!
 */
public final class EMCHelper {

	/**
	 * Consumes EMC from fuel items or Klein Stars Any extra EMC is discarded !!! To retain remainder EMC use ItemPE.consumeFuel()
	 *
	 * @implNote Order it tries to extract from is, Curios, Offhand, main inventory
	 */
	public static long consumePlayerFuel(Player player, @Range(from = 0, to = Long.MAX_VALUE) long minFuel) {
		if (player.isCreative() || minFuel == 0) {
			return minFuel;
		}
		IItemHandler curios = PlayerHelper.getCurios(player);
		if (curios != null) {
			for (int i = 0; i < curios.getSlots(); i++) {
				long actualExtracted = tryExtract(curios.getStackInSlot(i), minFuel);
				if (actualExtracted > 0) {
					player.containerMenu.broadcastChanges();
					return actualExtracted;
				}
			}
		}

		ItemStack offhand = player.getOffhandItem();

		if (!offhand.isEmpty()) {
			long actualExtracted = tryExtract(offhand, minFuel);
			if (actualExtracted > 0) {
				player.containerMenu.broadcastChanges();
				return actualExtracted;
			}
		}

		Optional<IItemHandler> itemHandlerCap = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
		if (itemHandlerCap.isPresent()) {
			//Ensure that we have an item handler capability, because if for example the player is dead we will not
			IItemHandler inv = itemHandlerCap.get();
			Map<Integer, Integer> map = new LinkedHashMap<>();
			boolean metRequirement = false;
			long emcConsumed = 0;
			for (int i = 0; i < inv.getSlots(); i++) {
				ItemStack stack = inv.getStackInSlot(i);
				if (stack.isEmpty()) {
					continue;
				}
				long actualExtracted = tryExtract(stack, minFuel);
				if (actualExtracted > 0) {
					player.containerMenu.broadcastChanges();
					return actualExtracted;
				} else if (!metRequirement) {
					if (FuelMapper.isStackFuel(stack)) {
						long emc = getEmcValue(stack);
						int toRemove = (int) Math.ceil((double) (minFuel - emcConsumed) / emc);

						if (stack.getCount() >= toRemove) {
							map.put(i, toRemove);
							emcConsumed += emc * toRemove;
							metRequirement = true;
						} else {
							map.put(i, stack.getCount());
							emcConsumed += emc * stack.getCount();
							if (emcConsumed >= minFuel) {
								metRequirement = true;
							}
						}
					}
				}
			}
			if (metRequirement) {
				for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
					inv.extractItem(entry.getKey(), entry.getValue(), false);
				}
				player.containerMenu.broadcastChanges();
				return emcConsumed;
			}
		}
		return -1;
	}

	private static long tryExtract(@NotNull ItemStack stack, long minFuel) {
		if (stack.isEmpty()) {
			return 0;
		}
		Optional<IItemEmcHolder> holderCapability = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve();
		if (holderCapability.isPresent()) {
			IItemEmcHolder emcHolder = holderCapability.get();
			long simulatedExtraction = emcHolder.extractEmc(stack, minFuel, EmcAction.SIMULATE);
			if (simulatedExtraction == minFuel) {
				return emcHolder.extractEmc(stack, simulatedExtraction, EmcAction.EXECUTE);
			}
		}
		return 0;
	}

	public static boolean doesItemHaveEmc(ItemInfo info) {
		return getEmcValue(info) > 0;
	}

	public static boolean doesItemHaveEmc(ItemStack stack) {
		return getEmcValue(stack) > 0;
	}

	public static boolean doesItemHaveEmc(ItemLike item) {
		return getEmcValue(item) > 0;
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEmcValue(ItemLike item) {
		return item == null ? 0 : getEmcValue(ItemInfo.fromItem(item));
	}

	/**
	 * Does not consider stack size
	 */
	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEmcValue(ItemStack stack) {
		return stack.isEmpty() ? 0 : getEmcValue(ItemInfo.fromStack(stack));
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEmcValue(ItemInfo info) {
		return NBTManager.getEmcValue(info);
	}

	private static double getEmcBuyMultiplier(ItemInfo info, @Nullable Player player) {
		return ProjectEConfig.server.difficulty.maxCreationCostMultiplier.get();
	}

	private static double getEmcSellMultiplier(ItemInfo info, @Nullable Player player) {
		return ProjectEConfig.server.difficulty.minBurnEfficiency.get();
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEmcBuyValue(ItemStack stack, @Nullable Player player) {
		return getEmcBuyValue(ItemInfo.fromStack(stack), player);
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEmcBuyValue(ItemInfo info, @Nullable Player player) {
		long originalValue = getEmcValue(info);
		if (originalValue == 0) {
			return 0;
		}
		return (long) Math.floor(originalValue * getEmcBuyMultiplier(info, player));
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEmcSellValue(ItemStack stack, @Nullable Player player) {
		return stack.isEmpty() ? 0 : getEmcSellValue(ItemInfo.fromStack(stack), player);
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEmcSellValue(ItemInfo info, @Nullable Player player) {
		long originalValue = getEmcValue(info);
		if (originalValue == 0) {
			return 0;
		}
		long emc = (long) Math.floor(originalValue * getEmcSellMultiplier(info, player));
		if (emc < 1) {
			if (ProjectEConfig.server.difficulty.burnCostRounding.get()) {
				emc = 1;
			} else {
				emc = 0;
			}
		}
		return emc;
	}

	public static Component getEmcTextComponent(ItemLike item, int stackSize, @Nullable Player player) {
		return getEmcTextComponent(ItemInfo.fromItem(item), stackSize, player);
	}

	public static Component getEmcTextComponent(ItemStack item, int stackSizeUsed, @Nullable Player player) {
		return getEmcTextComponent(ItemInfo.fromStack(item), stackSizeUsed, player);
	}

	public static Component getEmcTextComponent(ItemInfo info, int stackSize, @Nullable Player player) {
		long emc = getEmcValue(info);

		boolean showExtraBuyAndSellInfo = player != null;
		if (!showExtraBuyAndSellInfo) {
			ILangEntry prefix;
			String value;
			if (stackSize > 1) {
				prefix = PELang.EMC_STACK_TOOLTIP;
				value = Constants.EMC_FORMATTER.format(BigInteger.valueOf(emc).multiply(BigInteger.valueOf(stackSize)));
			} else {
				prefix = PELang.EMC_TOOLTIP;
				value = Constants.EMC_FORMATTER.format(emc);
			}
			return prefix.translateColored(ChatFormatting.YELLOW, ChatFormatting.WHITE, value);
		}

		long emcSellValue = getEmcSellValue(info, player);
		long emcBuyValue = getEmcBuyValue(info, player);
		ILangEntry prefix;
		String value;
		String sell;
		String buy;
		if (stackSize > 1) {
			prefix = PELang.EMC_STACK_TOOLTIP_WITH_SELL;
			BigInteger bigIntStack = BigInteger.valueOf(stackSize);
			value = Constants.EMC_FORMATTER.format(BigInteger.valueOf(emc).multiply(bigIntStack));
			sell = Constants.EMC_FORMATTER.format(BigInteger.valueOf(emcSellValue).multiply(bigIntStack));
			buy = Constants.EMC_FORMATTER.format(BigInteger.valueOf(emcBuyValue).multiply(bigIntStack));
		} else {
			prefix = PELang.EMC_TOOLTIP_WITH_SELL;
			value = Constants.EMC_FORMATTER.format(emc);
			sell = Constants.EMC_FORMATTER.format(emcSellValue);
			buy = Constants.EMC_FORMATTER.format(emcBuyValue);
		}
		return prefix.translateColored(ChatFormatting.YELLOW, ChatFormatting.WHITE, value, ChatFormatting.BLUE, sell, ChatFormatting.BLUE, buy);
	}

	@Range(from = 1, to = Long.MAX_VALUE)
	public static long getKleinStarMaxEmc(ItemStack stack) {
		if (stack.getItem() instanceof KleinStar star) {
			return Constants.MAX_KLEIN_EMC[star.tier.ordinal()];
		}
		return Constants.MAX_KLEIN_EMC[0];
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	public static long getEMCPerDurability(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		} else if (stack.isDamageableItem()) {
			ItemStack stackCopy = stack.copy();
			stackCopy.setDamageValue(0);
			long emc = (long) Math.ceil(getEmcValue(stackCopy) / (double) stack.getMaxDamage());
			return Math.max(emc, 1);
		}
		return 1;
	}

	/**
	 * Adds the given amount to the amount of unprocessed EMC the stack has. The amount returned should be used for figuring out how much EMC actually gets removed. While
	 * the remaining fractional EMC will be stored in UnprocessedEMC.
	 *
	 * @param stack  The stack to set the UnprocessedEMC tag to.
	 * @param amount The partial amount of EMC to add with the current UnprocessedEMC
	 *
	 * @return The amount of non fractional EMC no longer being stored in UnprocessedEMC.
	 */
	public static long removeFractionalEMC(ItemStack stack, double amount) {
		CompoundTag nbt = stack.getOrCreateTag();
		double unprocessedEMC = nbt.getDouble(Constants.NBT_KEY_UNPROCESSED_EMC);
		unprocessedEMC += amount;
		long toRemove = (long) unprocessedEMC;
		unprocessedEMC -= toRemove;
		nbt.putDouble(Constants.NBT_KEY_UNPROCESSED_EMC, unprocessedEMC);
		return toRemove;
	}
}