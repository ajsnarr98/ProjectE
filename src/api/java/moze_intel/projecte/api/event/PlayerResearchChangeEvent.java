package moze_intel.projecte.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * This event is fired serverside after a players transmutation research is changed.
 *
 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}, and has no result
 *
 * This event is fired on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}
 */
public class PlayerResearchChangeEvent extends Event {

	private final UUID playerUUID;

	public PlayerResearchChangeEvent(@NotNull Player player) {
		this(player.getUUID());
	}

	public PlayerResearchChangeEvent(@NotNull UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	/**
	 * @return The player UUID whose research changed. The associated player may or may not be logged in when this event fires.
	 */
	@NotNull
	public UUID getPlayerUUID() {
		return playerUUID;
	}
}