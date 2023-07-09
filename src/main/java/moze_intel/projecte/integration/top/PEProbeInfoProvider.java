package moze_intel.projecte.integration.top;

import java.util.function.Function;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import moze_intel.projecte.PECore;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//Registered via IMC
@SuppressWarnings("unused")
public class PEProbeInfoProvider implements IProbeInfoProvider, Function<ITheOneProbe, Void> {

	@Override
	public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level, BlockState blockState, IProbeHitData data) {
		if (ProjectEConfig.server.misc.hwylaTOPDisplay.get()) {
			ItemStack item = new ItemStack(blockState.getBlock());
			long value = EMCHelper.getEmcValue(item);
			if (value > 0) {
				probeInfo.mcText(EMCHelper.getEmcTextComponent(item, 1, null));
			}
		}
	}

	@Override
	public ResourceLocation getID() {
		return PECore.rl("emc");
	}

	@Override
	public Void apply(ITheOneProbe iTheOneProbe) {
		iTheOneProbe.registerProvider(this);
		return null;
	}
}