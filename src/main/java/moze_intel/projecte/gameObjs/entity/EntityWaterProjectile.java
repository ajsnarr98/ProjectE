package moze_intel.projecte.gameObjs.entity;

import javax.annotation.Nonnull;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.network.protocol.Packet;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.network.NetworkHooks;

public class EntityWaterProjectile extends ThrowableProjectile {

	public EntityWaterProjectile(EntityType<EntityWaterProjectile> type, Level world) {
		super(type, world);
	}

	public EntityWaterProjectile(Player entity, Level world) {
		super(PEEntityTypes.WATER_PROJECTILE.get(), entity, world);
	}

	@Override
	protected void defineSynchedData() {
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.getCommandSenderWorld().isClientSide) {
			if (tickCount > 400 || !getCommandSenderWorld().isLoaded(blockPosition())) {
				discard();
				return;
			}
			Entity thrower = getOwner();
			if (thrower instanceof ServerPlayer player) {
				BlockPos.betweenClosedStream(blockPosition().offset(-3, -3, -3), blockPosition().offset(3, 3, 3)).forEach(pos -> {
					BlockState state = level.getBlockState(pos);
					FluidState fluidState = state.getFluidState();
					if (fluidState.is(FluidTags.LAVA)) {
						pos = pos.immutable();
						if (state.getBlock() instanceof LiquidBlock) {
							//If it is a source block convert it
							Block block = fluidState.isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
							//Like: ForgeEventFactory#fireFluidPlaceBlockEvent except checks if it was cancelled
							BlockEvent.FluidPlaceBlockEvent event = new BlockEvent.FluidPlaceBlockEvent(level, pos, pos, block.defaultBlockState());
							if (!MinecraftForge.EVENT_BUS.post(event)) {
								PlayerHelper.checkedPlaceBlock(player, pos, event.getNewState());
							}
						} else {
							//Otherwise if it is lava logged, "void" the lava as we can't place a block in that spot
							WorldHelper.drainFluid(level, pos, state, Fluids.LAVA);
						}
						playSound(SoundEvents.GENERIC_BURN, 0.5F, 2.6F + (getCommandSenderWorld().random.nextFloat() - getCommandSenderWorld().random.nextFloat()) * 0.8F);
					}
				});
			}
			if (isInWater()) {
				discard();
			}
			if (getY() > 128) {
				LevelData worldInfo = this.getCommandSenderWorld().getLevelData();
				worldInfo.setRaining(true);
				discard();
			}
		}
	}

	@Override
	public float getGravity() {
		return 0;
	}

	@Override
	protected void onHit(@Nonnull HitResult mop) {
		if (level.isClientSide) {
			return;
		}
		Entity thrower = getOwner();
		if (!(thrower instanceof Player)) {
			discard();
			return;
		}
		if (mop instanceof BlockHitResult result) {
			WorldHelper.placeFluid((ServerPlayer) thrower, level, result.getBlockPos(), result.getDirection(), Fluids.WATER, !ProjectEConfig.server.items.opEvertide.get());
		} else if (mop instanceof EntityHitResult result) {
			Entity ent = result.getEntity();
			if (ent.isOnFire()) {
				ent.clearFire();
			}
			ent.push(this.getDeltaMovement().x() * 2, this.getDeltaMovement().y() * 2, this.getDeltaMovement().z() * 2);
		}
		discard();
	}

	@Nonnull
	@Override
	public Packet<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public boolean ignoreExplosion() {
		return true;
	}
}