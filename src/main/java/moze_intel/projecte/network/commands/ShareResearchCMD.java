package moze_intel.projecte.network.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeClearPKT;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

public class ShareResearchCMD {

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("shareknowledgefragments")
				.requires(cs -> cs.hasPermission(2))
				.then(Commands.argument("targets", EntityArgument.players())
						.executes(cs -> execute(cs, EntityArgument.getPlayers(cs, "targets"))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
		CommandSourceStack source = ctx.getSource();
		Player sourcePlayer;
		if (source.getEntity() instanceof Player) {
			sourcePlayer = (Player) source.getEntity();
		} else {
			source.sendFailure(PELang.SHARE_RESEARCH_SOURCE_WAS_NOT_PLAYER.translate());
			return 0;
		}
		IKnowledgeProvider sourceKnowledge = sourcePlayer.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
		if (sourceKnowledge == null) {
			source.sendFailure(PELang.SHARE_RESEARCH_SENDER_KNOWLEDGE_MISSING.translate());
			return 0;
		}

		int succeeded = 0;
		for (ServerPlayer player : targets) {
			IKnowledgeProvider knowledge = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve().orElse(null);
			if (knowledge == null) {
				// skip current player
				source.sendFailure(PELang.SHARE_RESEARCH_RECEIVER_KNOWLEDGE_MISSING.translate(player.getDisplayName().getString()));
				continue;
			}
			if (knowledge.updateResearchFragmentsUsing(sourceKnowledge)) {
				knowledge.sync(player);
				source.sendSuccess(PELang.SHARE_RESEARCH_SENDER_SUCCESS.translate(player.getDisplayName()), true);
				player.sendMessage(PELang.SHARE_RESEARCH_RECEIVER_SUCCESS.translateColored(ChatFormatting.RED, source.getDisplayName()), Util.NIL_UUID);
				succeeded++;
			} else {
				source.sendFailure(PELang.SHARE_RESEARCH_SENDER_FAILED_TO_ADD_NEW_RESEARCH.translate(player.getDisplayName()));
			}
		}
		return succeeded;
	}
}