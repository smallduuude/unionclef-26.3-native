package adris.altoclef.mixins;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.commandsystem.StringReader;
import adris.altoclef.commandsystem.TabCompleter;
import adris.altoclef.commandsystem.args.Arg;
import adris.altoclef.commandsystem.args.ListArg;
import adris.altoclef.commandsystem.exception.BadCommandSyntaxException;
import adris.altoclef.commandsystem.exception.CommandException;
import adris.altoclef.commandsystem.exception.CommandNotFinishedException;
import adris.altoclef.commandsystem.exception.RuntimeCommandException;
import adris.altoclef.util.Pair;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Highlighting + tab-completion for UnionClef's own {@code @commands} in the chat input.
 *
 * <p>26.3 port: CommandSuggestions no longer exposes {@code refresh()} / {@code provideRenderText()}.
 * Highlighting is now installed as an {@link EditBox.TextFormatter} (via {@code input.addFormatter});
 * the completion injection moved to the {@code updateCommandInfo} HEAD (the 26.3 entry point for
 * recomputing suggestions).
 */
@Mixin(CommandSuggestions.class)
public abstract class ChatInputSuggestorMixin {

    @Unique
    private static final Style SEMICOLOMN_STYLE = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
    @Shadow
    @Final
    private static Style UNPARSED_STYLE;
    @Shadow
    @Final
    private static Style LITERAL_STYLE;
    @Shadow
    @Final
    private static List<Style> ARGUMENT_STYLES;
    @Shadow
    @Final
    private EditBox input;
    @Shadow
    @Final
    private List<FormattedCharSequence> commandUsage;
    @Shadow
    @Final
    private Screen screen;
    @Shadow
    @Final
    private Font font;
    @Shadow
    private ParseResults<ClientSuggestionProvider> currentParse;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;
    @Shadow
    boolean keepSuggestions;

    @Unique
    private boolean altoclef$formatterInstalled = false;

    @Unique
    private final HashMap<Pair<String, Integer>, Pair<MutableComponent, Optional<Pair<MutableComponent, Integer>>>> parseCache = new HashMap<>();

    /** Install the highlighter once, then try altoclef's own tab-completion for our commands. */
    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    public void altoclef$onUpdateCommandInfo(CallbackInfo ci) {
        // ⛔ THE CUSTOM TYPING HIGHLIGHTER IS DISABLED. It is cosmetic (colouring the command as you
        // type), and an EditBox uses its formatter's output for cursor and width maths — so a
        // slicing bug inside it can corrupt the text being typed, which is how "@get <item>" came to
        // run a mangled name ("ned"). Tab-completion below is the useful part and is unaffected.
        // To re-enable the highlighter once its slicing is proven, restore this line:
        //   this.input.addFormatter(this::altoclef$format);
        altoclef$formatterInstalled = true;

        parseCache.clear();

        CommandExecutor executor = AltoClef.getCommandExecutor();
        if (executor == null) {
            return;
        }
        String value = this.input.getValue();
        String prefix = value.substring(0, Math.min(value.length(), this.input.getCursorPosition()));
        if (!executor.isClientCommand(prefix)) {
            return;
        }

        String[] completions = TabCompleter.complete(prefix);
        if (completions == null) {
            return;
        }

        ci.cancel();
        this.currentParse = null;

        if (this.keepSuggestions) {
            return;
        }

        this.input.setSuggestion(null);
        this.suggestions = null;
        this.commandUsage.clear();

        if (completions.length == 0) {
            this.pendingSuggestions = Suggestions.empty();
        } else {
            StringRange range = StringRange.between(prefix.lastIndexOf(" ") + 1, prefix.length());
            List<Suggestion> suggestionList = Stream.of(completions)
                    .map(s -> new Suggestion(range, s))
                    .collect(Collectors.toList());
            Suggestions suggestions = new Suggestions(range, suggestionList);
            this.pendingSuggestions = new CompletableFuture<>();
            this.pendingSuggestions.complete(suggestions);
        }
        ((CommandSuggestions) (Object) this).showSuggestions(true);
    }

    /** EditBox text formatter: highlights an altoclef command; leaves everything else untouched. */
    @Unique
    private FormattedCharSequence altoclef$format(String original, int firstCharacterIndex) {
        CommandExecutor executor = AltoClef.getCommandExecutor();
        if (executor == null || !this.input.getValue().startsWith(executor.getCommandPrefix())) {
            return FormattedCharSequence.forward(original, Style.EMPTY);
        }

        Pair<MutableComponent, Optional<Pair<MutableComponent, Integer>>> result =
                highlightText(original, firstCharacterIndex, this.input.getValue());
        if (result == null) {
            return FormattedCharSequence.forward(original, Style.EMPTY);
        }

        this.commandUsage.clear();
        if (result.getRight().isPresent()) {
            MutableComponent text = result.getRight().get().getLeft();
            this.commandUsage.add(text.getVisualOrderText());
        }
        return result.getLeft().getVisualOrderText();
    }

    @Unique
    private static String addStyledText(List<Pair<String, Style>> styledText, String original, String currentStr, Style style, StringReader reader) throws CommandException {
        if (!reader.hasNext()) {
            styledText.add(new Pair<>(currentStr, style));
            return "";
        }

        int diff = original.length() - currentStr.length();
        int index = reader.getIndex() - diff;
        String processed = currentStr.substring(0, index);

        styledText.add(new Pair<>(processed, style));

        return currentStr.substring(index);
    }

    @Unique
    private Pair<MutableComponent, Optional<Pair<MutableComponent, Integer>>> highlightText(String original, int firstCharacterIndex, String full) {
        MutableComponent text = Component.empty();
        MutableComponent errorMsg = null;

        Style splitColor = SEMICOLOMN_STYLE;
        int errorSeverity = 0;
        try {
            List<Pair<String, Style>> styledText = new ArrayList<>();

            String[] split = full.split(";", -1);
            int index = 0;
            for (int i = 0; i < split.length; i++) {
                String command = split[i];
                if (i > 0) index += 1; // the ";" that separated this segment from the last
                index += command.length();

                if (command.endsWith(" ") && (i + 1) < split.length) {
                    errorSeverity = 2;
                    errorMsg = buildErrorMessage("Unexpected argument", full, index);
                } else if (command.isBlank()) {
                    errorSeverity = Math.max(errorSeverity, 1);
                }

                if (errorSeverity > 0) {
                    splitColor = this.UNPARSED_STYLE;

                    styledText.add(new Pair<>(command, this.UNPARSED_STYLE));
                    if (i + 1 < split.length) {
                        styledText.add(new Pair<>(";", splitColor));
                    }

                    continue;
                }

                Pair<List<Pair<String, Style>>, Pair<Integer, MutableComponent>> part = getText(
                        command.stripLeading(), original.length() + firstCharacterIndex, i + 1 < split.length
                );

                errorSeverity = part.getRight().getLeft();
                if (errorSeverity > 0) {
                    splitColor = this.UNPARSED_STYLE;
                    errorMsg = part.getRight().getRight();
                }

                List<Pair<String, Style>> styled = new ArrayList<>(part.getLeft());
                String leadingSpace = command.substring(0, command.length() - command.stripLeading().length());

                String first = styled.getFirst().getLeft();
                styled.set(0, new Pair<>(leadingSpace + first, styled.getFirst().getRight()));

                styledText.addAll(styled);

                if (i + 1 < split.length) {
                    styledText.add(new Pair<>(";", splitColor));
                }
            }

            int maxLen = firstCharacterIndex + original.length();
            int length = 0;

            for (Pair<String, Style> pair : styledText) {
                String str = pair.getLeft();
                int nextLength = length + str.length();

                if (nextLength <= firstCharacterIndex) {
                    length = nextLength;
                    continue;
                }

                int start = Math.max(firstCharacterIndex - length, 0);
                int end = Math.min(str.length(), maxLen - length + start);

                String segment = str.substring(start, end);
                text.append(Component.literal(segment).setStyle(pair.getRight()));

                if (length + end >= maxLen) break;

                length = nextLength;
            }

        } catch (CommandException e) {
            return null;
        }
        if (errorMsg != null) {
            return new Pair<>(text, Optional.of(new Pair<>(errorMsg, errorSeverity)));
        }
        return new Pair<>(text, Optional.empty());
    }

    @Unique
    private Pair<List<Pair<String, Style>>, Pair<Integer, MutableComponent>> getText(String s, int maxLen, boolean showUnfinishedErrors) throws CommandException {
        CommandExecutor executor = AltoClef.getCommandExecutor();
        StringReader reader = new StringReader(s);
        String original = s;
        MutableComponent errorMsg = null;

        if (s.isBlank() || reader.peek().isEmpty())
            return new Pair<>(List.of(new Pair<>(s, this.LITERAL_STYLE)), new Pair<>(0, null));

        String cmd = reader.next();
        boolean hasPrefix = false;
        if (cmd.startsWith(executor.getCommandPrefix())) {
            hasPrefix = true;
            cmd = cmd.substring(executor.getCommandPrefix().length());
        }

        Command command = executor.get(cmd);

        if (command == null) {
            MutableComponent error = Component.literal("Unknown command '" + cmd + "'");

            ArrayList<Pair<String, Style>> res = new ArrayList<>();
            if (hasPrefix) {
                res.add(new Pair<>(executor.getCommandPrefix(), this.LITERAL_STYLE));
                res.add(new Pair<>(s.substring(executor.getCommandPrefix().length()), this.UNPARSED_STYLE));
            } else {
                res.add(new Pair<>(s, this.UNPARSED_STYLE));
            }

            return new Pair<>(res, new Pair<>(2, error));
        }

        List<Pair<String, Style>> styledText = new ArrayList<>();

        s = addStyledText(styledText, original, s, this.LITERAL_STYLE, reader);

        Arg<?>[] args = command.getArgs();
        int styleIndex = 0;
        int errorSeverity = 0;

        for (int i = 0; i < args.length; i++) {
            Arg<?> arg = args[i];
            if (!reader.hasNext()) {
                if (!arg.hasDefault) {
                    errorMsg = buildErrorMessage("Expected " + arg.getTypeName(), original + " ", original.length() + 1);
                    errorSeverity = 2;
                }
                break;
            }
            Arg.ParseResult result = arg.consumeIfSupplied(reader);

            if (result == Arg.ParseResult.CONSUMED) {
                s = addStyledText(styledText, original, s, this.ARGUMENT_STYLES.get(styleIndex), reader);

                styleIndex++;

                if (styleIndex >= this.ARGUMENT_STYLES.size()) {
                    styleIndex = 0;
                }

                continue;
            }

            StringReader copy = reader.copy();
            try {
                arg.parseArg(copy);
            } catch (CommandNotFinishedException e) {
                if (showUnfinishedErrors) {
                    errorMsg = buildErrorMessage(e.getMessage(), original, copy.getIndex());
                    errorSeverity = 2;
                } else if ((original.endsWith(" ") || original.endsWith("[") || original.endsWith(",") || original.endsWith(";") || arg instanceof ListArg<?>)
                        && original.length() == maxLen
                ) {
                    String str = command.getHelpRepresentation(cmd, i);
                    errorMsg = Component.literal(str).setStyle(this.LITERAL_STYLE);

                    errorSeverity = 1;
                }

            } catch (BadCommandSyntaxException e) {
                errorMsg = buildErrorMessage(e.getMessage(), original, copy.getIndex());
                errorSeverity = 2;
            }

            styledText.add(new Pair<>(s, this.UNPARSED_STYLE));
            s = "";
            break;
        }

        if (!s.isEmpty() || original.endsWith(" ")) {
            styledText.add(new Pair<>(s, this.UNPARSED_STYLE));

            if (errorMsg == null) {
                errorMsg = buildErrorMessage("Unexpected argument", original, reader.getIndex());
            }
            errorSeverity = 2;
        }

        return new Pair<>(styledText, new Pair<>(errorSeverity, errorMsg));
    }

    @Unique
    private MutableComponent buildErrorMessage(String message, String original, int index) {
        String substr = original.substring(0, index);

        int ind = substr.lastIndexOf(" ");
        if (ind == -1) {
            ind = substr.length();
        }
        substr = substr.substring(0, ind);

        int maxLen = 15;
        if (substr.length() > maxLen) {
            substr = "..." + substr.substring(substr.length() - (maxLen - 3));
        }

        return Component.literal(message)
                .append(Component.literal(" at position " + index + ": " + substr + " <--[HERE]"));
    }
}
