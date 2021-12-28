package moze_intel.projecte;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.item.IAlchBagItem;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.config.PEModConfig;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.EMCReloadListener;
import moze_intel.projecte.emc.json.NSSSerializer;
import moze_intel.projecte.emc.mappers.recipe.CraftingMapper;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.gameObjs.customRecipes.FullKleinStarIngredient;
import moze_intel.projecte.gameObjs.customRecipes.FullKleinStarsCondition;
import moze_intel.projecte.gameObjs.customRecipes.TomeEnabledCondition;
import moze_intel.projecte.gameObjs.items.rings.Arcana;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.gameObjs.registries.PERecipeSerializers;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.handlers.CommonInternalAbilities;
import moze_intel.projecte.handlers.InternalAbilities;
import moze_intel.projecte.handlers.InternalTimers;
import moze_intel.projecte.impl.IMCHandler;
import moze_intel.projecte.impl.TransmutationOffline;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.ThreadCheckUUID;
import moze_intel.projecte.network.ThreadCheckUpdate;
import moze_intel.projecte.network.commands.ClearKnowledgeCMD;
import moze_intel.projecte.network.commands.DumpMissingEmc;
import moze_intel.projecte.network.commands.RemoveEmcCMD;
import moze_intel.projecte.network.commands.ResetEmcCMD;
import moze_intel.projecte.network.commands.SetEmcCMD;
import moze_intel.projecte.network.commands.ShowBagCMD;
import moze_intel.projecte.network.commands.argument.ColorArgument;
import moze_intel.projecte.network.commands.argument.NSSItemArgument;
import moze_intel.projecte.network.commands.argument.UUIDArgument;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.WorldTransmutations;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PECore.MODID)
@Mod.EventBusSubscriber(modid = PECore.MODID)
public class PECore {

	public static final String MODID = ProjectEAPI.PROJECTE_MODID;
	public static final String MODNAME = "ProjectE";
	public static final GameProfile FAKEPLAYER_GAMEPROFILE = new GameProfile(UUID.fromString("590e39c7-9fb6-471b-a4c2-c0e539b2423d"), "[" + MODNAME + "]");
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final List<String> uuids = new ArrayList<>();

	public static ModContainer MOD_CONTAINER;

	public static void debugLog(String msg, Object... args) {
		if (!FMLEnvironment.production || ProjectEConfig.common.debugLogging.get()) {
			LOGGER.info(msg, args);
		} else {
			LOGGER.debug(msg, args);
		}
	}

	public static ResourceLocation rl(String path) {
		return new ResourceLocation(MODID, path);
	}

	public PECore() {
		MOD_CONTAINER = ModLoadingContext.get().getActiveContainer();

		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::imcQueue);
		modEventBus.addListener(this::imcHandle);
		modEventBus.addListener(this::onConfigLoad);
		modEventBus.addListener(this::registerCapabilities);
		modEventBus.addGenericListener(RecipeSerializer.class, this::registerRecipeSerializers);
		PEBlocks.BLOCKS.register(modEventBus);
		PEContainerTypes.CONTAINER_TYPES.register(modEventBus);
		PEEntityTypes.ENTITY_TYPES.register(modEventBus);
		PEItems.ITEMS.register(modEventBus);
		PERecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
		PESoundEvents.SOUND_EVENTS.register(modEventBus);
		PEBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::addReloadListenersLowest);
		MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
		MinecraftForge.EVENT_BUS.addListener(this::serverQuit);

		//Register our config files
		ProjectEConfig.register();
	}

	private void registerRecipeSerializers(RegistryEvent.Register<RecipeSerializer<?>> event) {
		//Add our condition serializers
		CraftingHelper.register(TomeEnabledCondition.SERIALIZER);
		CraftingHelper.register(FullKleinStarsCondition.SERIALIZER);
		//Add our ingredients
		CraftingHelper.register(rl("full_klein_star"), FullKleinStarIngredient.SERIALIZER);
	}

	private void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.register(IAlchBagProvider.class);
		event.register(IKnowledgeProvider.class);
		event.register(InternalTimers.class);
		event.register(InternalAbilities.class);
		event.register(CommonInternalAbilities.class);
		event.register(IAlchBagItem.class);
		event.register(IAlchChestItem.class);
		event.register(IExtraFunction.class);
		event.register(IItemCharge.class);
		event.register(IItemEmcHolder.class);
		event.register(IModeChanger.class);
		event.register(IPedestalItem.class);
		event.register(IProjectileShooter.class);
		event.register(IEmcStorage.class);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		new ThreadCheckUpdate().start();

		EMCMappingHandler.loadMappers();
		CraftingMapper.loadMappers();
		NBTManager.loadProcessors();

		event.enqueueWork(() -> {
			PacketHandler.register();
			//Dispenser Behavior
			registerDispenseBehavior(new ShearsDispenseItemBehavior(), PEItems.DARK_MATTER_SHEARS, PEItems.RED_MATTER_SHEARS, PEItems.RED_MATTER_KATAR);
			DispenserBlock.registerBehavior(PEBlocks.NOVA_CATALYST, PEBlocks.NOVA_CATALYST.getBlock().createDispenseItemBehavior());
			DispenserBlock.registerBehavior(PEBlocks.NOVA_CATACLYSM, PEBlocks.NOVA_CATACLYSM.getBlock().createDispenseItemBehavior());
			registerDispenseBehavior(new OptionalDispenseItemBehavior() {
				@Nonnull
				@Override
				protected ItemStack execute(@Nonnull BlockSource source, @Nonnull ItemStack stack) {
					//Based off the flint and steel dispense behavior
					if (stack.getItem() instanceof Arcana item) {
						if (item.getMode(stack) != 1) {
							//Only allow using the arcana ring to ignite things when on ignition mode
							setSuccess(false);
							return super.execute(source, stack);
						}
					}
					Level world = source.getLevel();
					setSuccess(true);
					Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
					BlockPos pos = source.getPos().relative(direction);
					BlockState state = world.getBlockState(pos);
					if (BaseFireBlock.canBePlacedAt(world, pos, direction)) {
						world.setBlockAndUpdate(pos, BaseFireBlock.getState(world, pos));
					} else if (CampfireBlock.canLight(state)) {
						world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, true));
					} else if (state.isFlammable(world, pos, direction.getOpposite())) {
						state.onCaughtFire(world, pos, direction.getOpposite(), null);
						if (state.getBlock() instanceof TntBlock) {
							world.removeBlock(pos, false);
						}
					} else {
						setSuccess(false);
					}
					return stack;
				}
			}, PEItems.IGNITION_RING, PEItems.ARCANA_RING);
			DispenserBlock.registerBehavior(PEItems.EVERTIDE_AMULET, new DefaultDispenseItemBehavior() {
				@Nonnull
				@Override
				public ItemStack execute(@Nonnull BlockSource source, @Nonnull ItemStack stack) {
					//Based off of vanilla's bucket dispense behaviors
					// Note: We only do evertide, not volcanite, as placing lava requires EMC
					Level world = source.getLevel();
					Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
					BlockPos pos = source.getPos().relative(direction);
					BlockEntity tile = WorldHelper.getTileEntity(world, pos);
					Direction sideHit = direction.getOpposite();
					if (tile != null) {
						Optional<IFluidHandler> capability = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, sideHit).resolve();
						if (capability.isPresent()) {
							capability.get().fill(new FluidStack(Fluids.WATER, FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
							return stack;
						}
					}
					BlockState state = world.getBlockState(pos);
					if (state.getBlock() == Blocks.CAULDRON) {
						//TODO - 1.18: Re-evaluate I think this should end up becoming a CauldronInteraction
						/*int waterLevel = state.getValue(CauldronBlock.LEVEL);
						if (waterLevel < 3) {
							((CauldronBlock) state.getBlock()).setWaterLevel(world, pos, state, waterLevel + 1);
							return stack;
						}*/
					} else {
						WorldHelper.placeFluid(null, world, pos, Fluids.WATER, !ProjectEConfig.server.items.opEvertide.get());
						world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), PESoundEvents.WATER_MAGIC.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
						return stack;
					}
					return super.execute(source, stack);
				}
			});

			// internals unsafe
			ArgumentTypes.register(MODID + ":uuid", UUIDArgument.class, new EmptyArgumentSerializer<>(UUIDArgument::new));
			ArgumentTypes.register(MODID + ":color", ColorArgument.class, new EmptyArgumentSerializer<>(ColorArgument::new));
			ArgumentTypes.register(MODID + ":nss", NSSItemArgument.class, new EmptyArgumentSerializer<>(NSSItemArgument::new));
		});
	}

	private static void registerDispenseBehavior(DispenseItemBehavior behavior, ItemLike... items) {
		for (ItemLike item : items) {
			DispenserBlock.registerBehavior(item, behavior);
		}
	}

	private void imcQueue(InterModEnqueueEvent event) {
		WorldTransmutations.init();
		NSSSerializer.init();
		IntegrationHelper.sendIMCMessages(event);
	}

	private void imcHandle(InterModProcessEvent event) {
		IMCHandler.handleMessages();
	}

	private void onConfigLoad(ModConfigEvent configEvent) {
		//Note: We listen to both the initial load and the reload, so as to make sure that we fix any accidentally
		// cached values from calls before the initial loading
		ModConfig config = configEvent.getConfig();
		//Make sure it is for the same modid as us
		if (config.getModId().equals(MODID) && config instanceof PEModConfig peConfig) {
			peConfig.clearCache();
		}
	}

	private void addReloadListenersLowest(AddReloadListenerEvent event) {
		//Note: We register our listener for this event on lowest priority so that if other mods register custom NSSTags
		// or other things that need to be sync'd/reloaded they have a chance to go before we do
		event.addListener(new EMCReloadListener(event.getDataPackRegistries()));
	}

	private void registerCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("projecte")
				.then(ClearKnowledgeCMD.register())
				.then(DumpMissingEmc.register())
				.then(RemoveEmcCMD.register())
				.then(ResetEmcCMD.register())
				.then(SetEmcCMD.register())
				.then(ShowBagCMD.register());
		event.getDispatcher().register(root);
	}

	private void serverStarting(ServerStartingEvent event) {
		if (!ThreadCheckUUID.hasRunServer()) {
			new ThreadCheckUUID(true).start();
		}
	}

	private void serverQuit(ServerStoppedEvent event) {
		//Ensure we save any changes to the custom emc file
		CustomEMCParser.flush();
		TransmutationOffline.cleanAll();
		EMCMappingHandler.clearEmcMap();
	}
}