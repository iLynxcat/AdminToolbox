package org.modernbeta.admintoolbox.command;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.BrigadierCommand;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

@SuppressWarnings("UnstableApiUsage")
public class YellCommand implements BrigadierCommand {

	private static final String YELL_COMMAND_PERMISSION = "admintoolbox.yell";

	private static final String MESSAGE_FEEDBACK = "Yelled at <target>: <message>";
	private static final String MESSAGE_NOTIFICATION = "<actor> yelled at <target>: <message>";

	private static final TextColor YELL_TITLE_DEFAULT_COLOR = NamedTextColor.RED;
	private static final TextColor FEEDBACK_COLOR = NamedTextColor.GOLD;

	private final MiniMessage mini = MiniMessage.miniMessage();
	private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacy('&');

	@Override
	public String description() {
		return "Display a title message on a player’s screen.";
	}

	@Override
	public Set<String> aliases() {
		return Set.of("y", "adminsay");
	}

	@Override
	public LiteralCommandNode<CommandSourceStack> buildNode() {
		var node = Commands.literal("yell")
			.requires((stack) -> stack.getSender().hasPermission(YELL_COMMAND_PERMISSION))
			.then(Commands.argument("target", ArgumentTypes.player())
				.then(Commands.argument("message", StringArgumentType.greedyString())
					.suggests(this::suggestSeparatorCharIfAbsent)
					.executes(this::runYell)));

		return node.build();
	}

	private CompletableFuture<Suggestions> suggestSeparatorCharIfAbsent(CommandContext<CommandSourceStack> context,
																		SuggestionsBuilder builder) {
		String input = builder.getRemaining();
		if (!input.contains("|") && input.endsWith(" ")) {
			SuggestionsBuilder offsetBuilder = builder.createOffset(builder.getInput().length());
			offsetBuilder.suggest("|");
			return offsetBuilder.buildFuture();
		}

		return builder.buildFuture();
	}

	private int runYell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSender sender = context.getSource().getSender();
		Player target;
		{
			PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
			Collection<Player> players = resolver.resolve(context.getSource());
			target = players.iterator().next();
		}
		if (target == null) {
			// TODO: error message
			return 0;
		}

		String titleInput;
		String subtitleInput;
		{
			String messageInput = StringArgumentType.getString(context, "message");
			String[] pieces = messageInput.split("\\|", 2);
			titleInput = pieces[0].trim();
			subtitleInput = pieces.length > 1
				? pieces[1].trim()
				: null;
		}

		Component titleComponent = Component.empty().color(YELL_TITLE_DEFAULT_COLOR);
		Component subtitleComponent = Component.empty().color(YELL_TITLE_DEFAULT_COLOR);
		{
			titleComponent = titleComponent.append(legacy.deserialize(titleInput));

			if (subtitleInput != null)
				subtitleComponent = subtitleComponent.append(legacy.deserialize(subtitleInput));
		}

		Title title = Title.title(titleComponent, subtitleComponent);
		target.showTitle(title);

		{
			Component feedbackMessage = titleComponent;

			if (!subtitleComponent.children().isEmpty())
				feedbackMessage = feedbackMessage.appendNewline()
					.appendSpace() // indent the subtitle line slightly
					.append(subtitleComponent);

			Component notification = mini.deserialize(
				MESSAGE_NOTIFICATION,
				Placeholder.component("actor", sender.name()),
				Placeholder.component("target", target.name()),
				Placeholder.component("message", feedbackMessage)
			).color(FEEDBACK_COLOR);

			AdminToolboxPlugin.getInstance().getAdminAudience()
				.excluding(sender, target)
				.sendMessage(notification);

			sender.sendMessage(mini.deserialize(
				MESSAGE_FEEDBACK,
				Placeholder.component("target", target.name()),
				Placeholder.component("message", feedbackMessage)
			).color(FEEDBACK_COLOR));
		}

		return 1;
	}

}
