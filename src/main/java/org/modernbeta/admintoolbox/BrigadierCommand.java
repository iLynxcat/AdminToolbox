package org.modernbeta.admintoolbox;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Set;

public interface BrigadierCommand {
	LiteralCommandNode<CommandSourceStack> buildNode();
	String description();

	default Set<String> aliases() {
		return Set.of();
	}

	default void register(Commands registry) {
		registry.register(this.buildNode(), this.description(), this.aliases());
	}
}
