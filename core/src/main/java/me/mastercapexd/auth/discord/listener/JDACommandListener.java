package me.mastercapexd.auth.discord.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import me.mastercapexd.auth.discord.command.actor.BaseJDAButtonActor;
import me.mastercapexd.auth.link.LinkCommandActorWrapper;
import me.mastercapexd.auth.link.discord.DiscordLinkType;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.Command.Type;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import revxrsal.commands.command.ArgumentStack;
import revxrsal.commands.command.CommandCategory;
import revxrsal.commands.command.CommandParameter;
import revxrsal.commands.command.ExecutableCommand;
import revxrsal.commands.core.CommandPath;
import revxrsal.commands.jda.JDAActor;
import revxrsal.commands.jda.JDACommandHandler;
import revxrsal.commands.jda.annotation.OptionData;
import revxrsal.commands.jda.core.actor.BaseJDASlashCommandActor;

public class JDACommandListener implements EventListener {
    private static final DiscordLinkType DISCORD_LINK_TYPE = DiscordLinkType.getInstance();
    private final JDACommandHandler handler;
    private final Function<JDAActor, LinkCommandActorWrapper> wrapper;

    public JDACommandListener(JDACommandHandler handler, Function<JDAActor, LinkCommandActorWrapper> wrapper) {
        this.handler = handler;
        this.wrapper = wrapper;
    }

    private void dispatch(JDAActor actor, ArgumentStack arguments) {
        LinkCommandActorWrapper actorWrapper = wrapper.apply(actor);
        try {
            handler.dispatch(actorWrapper, arguments);
        } catch(Throwable t) {
            handler.getExceptionHandler().handleException(t, actorWrapper);
        }
    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof SlashCommandInteractionEvent)
            onSlashCommandEvent((SlashCommandInteractionEvent) genericEvent);
        if (genericEvent instanceof ButtonInteractionEvent)
            onButtonEvent((ButtonInteractionEvent) genericEvent);
        if (genericEvent instanceof CommandAutoCompleteInteractionEvent)
            onAutocompleteEvent((CommandAutoCompleteInteractionEvent) genericEvent);
    }

    private void onAutocompleteEvent(CommandAutoCompleteInteractionEvent event) {
        Optional<ExecutableCommand> commandOptional = findExecutableCommand(event);
        if (!commandOptional.isPresent())
            return;
        ExecutableCommand command = commandOptional.get();
        AutoCompleteQuery focusedOption = event.getFocusedOption();
        Optional<CommandParameter> foundParameter = command.getValueParameters()
                .values()
                .stream()
                .filter(parameter -> getParameterName(parameter).equals(focusedOption.getName()))
                .findFirst();
        if (!foundParameter.isPresent())
            return;
        CommandParameter parameter = foundParameter.get();
        try {
            Collection<String> suggestions = parameter.getSuggestionProvider()
                    .getSuggestions(event.getOptions().stream().map(OptionMapping::getAsString).collect(Collectors.toList()), JDAActor.wrap(event, handler),
                            command);
            event.replyChoices(suggestions.stream().map(suggestion -> {
                if (focusedOption.getType() == OptionType.NUMBER)
                    return new Choice(suggestion, Double.parseDouble(suggestion));
                if (focusedOption.getType() == OptionType.INTEGER)
                    return new Choice(suggestion, Long.parseLong(suggestion));
                return new Choice(suggestion, suggestion);
            }).collect(Collectors.toList())).queue();
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    private void onButtonEvent(ButtonInteractionEvent event) {
        String content = event.getComponentId();
        if (content.isEmpty())
            return;

        event.deferReply(true).queue();
        JDAActor actor = new BaseJDAButtonActor(event, handler);
        try {
            ArgumentStack arguments = ArgumentStack.parse(content);
            dispatch(actor, arguments);
        } catch(Throwable t) {
            handler.getExceptionHandler().handleException(t, actor);
        }
    }

    private void onSlashCommandEvent(SlashCommandInteractionEvent event) {
        parseSlashCommandEvent(event).ifPresent(arguments -> {
            JDAActor actor = new BaseJDASlashCommandActor(event, handler);
            event.deferReply(true).queue();
            dispatch(actor, arguments);
        });
    }

    private Optional<ExecutableCommand> findExecutableCommand(CommandInteractionPayload event) {
        CommandPath commandPath = CommandPath.get(
                Stream.of(event.getName(), event.getSubcommandGroup(), event.getSubcommandName()).filter(Objects::nonNull).collect(Collectors.toList()));
        ExecutableCommand command = handler.getCommand(commandPath);
        if (command != null)
            return Optional.of(command);

        CommandCategory category = handler.getCategory(commandPath);
        if (category == null)
            return Optional.empty();
        return Optional.ofNullable(category.getDefaultAction());
    }

    private String getParameterName(CommandParameter parameter) {
        if (parameter.hasAnnotation(OptionData.class)) {
            OptionData optionData = parameter.getAnnotation(OptionData.class);
            return optionData.name().isEmpty() ? parameter.getName() : optionData.name();
        }
        return parameter.getName();
    }

    /**
     * Parses a SlashCommandInteractionEvent and converts event to an {@link ArgumentStack}.
     *
     * @param event The SlashCommandInteractionEvent to parse.
     * @return An Optional containing the {@link ArgumentStack}.
     */
    private Optional<ArgumentStack> parseSlashCommandEvent(SlashCommandInteractionEvent event) {
        if (event.getCommandType() != Type.SLASH)
            return Optional.of(ArgumentStack.copyExact(event.getName()));
        return findExecutableCommand(event).map(foundCommand -> {
            List<String> arguments = new ArrayList<>(foundCommand.getPath().toList());

            Map<Integer, CommandParameter> valueParameters = foundCommand.getValueParameters();
            for (int i = 0; i < valueParameters.size(); i++) {
                CommandParameter parameter = valueParameters.get(i);
                OptionMapping optionMapping = event.getOption(getParameterName(parameter));
                if (optionMapping == null) {
                    arguments.addAll(parameter.getDefaultValue());
                    continue;
                }
                if (parameter.isFlag())
                    arguments.add("-" + parameter.getFlagName());
                if (parameter.isSwitch() && optionMapping.getType() == OptionType.BOOLEAN && optionMapping.getAsBoolean()) {
                    arguments.add("-" + parameter.getSwitchName());
                    continue;
                }
                appendOptionMapping(arguments, optionMapping);
            }

            return ArgumentStack.copyExact(arguments);
        });
    }

    private void appendOptionMapping(Collection<String> arguments, OptionMapping optionMapping) {
        switch (optionMapping.getType()) {
            case CHANNEL:
                arguments.add(optionMapping.getAsChannel().getName());
                break;
            case USER:
                arguments.add(optionMapping.getAsUser().getName());
                break;
            case ROLE:
                arguments.add(optionMapping.getAsRole().getName());
                break;
            case MENTIONABLE:
                arguments.add("<@" + optionMapping.getAsMentionable().getIdLong() + ">");
                break;
            default:
                arguments.add(optionMapping.getAsString());
        }
    }
}

