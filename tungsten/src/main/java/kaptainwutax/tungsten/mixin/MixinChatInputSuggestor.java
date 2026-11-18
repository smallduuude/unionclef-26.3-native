package kaptainwutax.tungsten.mixin;

import static kaptainwutax.tungsten.commandsystem.suggestionsapi.Filtering.FilteringMode.LOOSE;
import static kaptainwutax.tungsten.commandsystem.suggestionsapi.Filtering.FilteringMode.SLIGHTLY_LOOSE;
import static kaptainwutax.tungsten.commandsystem.suggestionsapi.Filtering.FilteringMode.STRICT;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.commandsystem.CommandExecutor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * Tungsten's own {@code |}-prefixed command suggestions.
 *
 * <p>26.3 port: the old inject used {@code LocalCapture.CAPTURE_FAILHARD} on
 * {@code updateCommandInfo}'s locals (fragile — the method body changed) and called a no-arg
 * {@code updateUsageInfo()} shadow that no longer exists (the method now takes
 * {@code (ParseResults, Suggestions)}). Rewritten as a HEAD inject that reads the input directly,
 * so it resolves without any local capture; the usage box is recomputed by vanilla afterwards.
 */
@Mixin(CommandSuggestions.class)
public abstract class MixinChatInputSuggestor {
    @Shadow private ParseResults<SharedSuggestionProvider> currentParse;
    @Shadow boolean keepSuggestions;
    @Shadow private CommandSuggestions.SuggestionsList suggestions;
    @Shadow private static int getLastWordIndex(String input) {
        throw new AssertionError();
    }
    @Shadow @Final EditBox input;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    @Final
    private boolean commandsOnly;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "sortSuggestions", at = @At("HEAD"), cancellable = true)
    private List<Suggestion> sortSuggestions(Suggestions suggestions, CallbackInfoReturnable<List<Suggestion>> ci) {
        String command = input.getValue().substring(0, input.getCursorPosition());

        ci.cancel();
        // To make sorting command literals work
        if (command.startsWith("/") || command.startsWith(TungstenMod.getCommandPrefix()))
            command = command.substring(1);
        int startOfCurrentWord = getLastWordIndex(command);
        String remaining = command.substring(startOfCurrentWord);
        // To make sorting tags work
        if (remaining.startsWith("#"))
            remaining = remaining.substring(1);
        List<Suggestion> strictList = Lists.newArrayList();
        List<Suggestion> slightlyLooseList = Lists.newArrayList();
        List<Suggestion> looseList = Lists.newArrayList();
        List<Suggestion> veryLooseList = Lists.newArrayList();

        if (remaining.contains(":"))
            remaining = remaining.substring(remaining.indexOf(':') + 1);
        remaining = remaining.toLowerCase(Locale.ROOT);

        for (Suggestion suggestion : suggestions.getList()) {
            String suggestionText = suggestion.getText();
            if (suggestionText.contains(":"))
                suggestionText = suggestionText.substring(suggestionText.indexOf(':') + 1);
            suggestionText = suggestionText.toLowerCase(Locale.ROOT);

            if (STRICT.test(remaining, suggestionText))
                strictList.add(suggestion);
            else if (SLIGHTLY_LOOSE.test(remaining, suggestionText))
                slightlyLooseList.add(suggestion);
            else if (LOOSE.test(remaining, suggestionText))
                looseList.add(suggestion);
            else
                veryLooseList.add(suggestion);
        }

        strictList.addAll(slightlyLooseList);
        strictList.addAll(looseList);
        strictList.addAll(veryLooseList);
        ci.setReturnValue(strictList);
        return strictList;
    }

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void tungsten$updateCommandInfo(CallbackInfo ci) {
        if (TungstenMod.mc == null) {
            return; // tungsten not initialized yet
        }
        String prefix = TungstenMod.getCommandPrefix();
        String message = input.getValue();
        int cursor = input.getCursorPosition();
        if (cursor < prefix.length() || !message.startsWith(prefix)) {
            return; // not a tungsten command, let vanilla handle it
        }

        if (TungstenMod.mc.getConnection() == null) {
            return; // no world: no suggestion provider to parse against
        }

        // Chained commands: ";" separates them, so suggest for the last '|'/separator segment.
        String segment = message.contains("|") ? message.substring(message.lastIndexOf('|') + 1) : message;

        // ⛔ RE-PARSE EVERY TIME. The parse used to be cached (`if (currentParse == null)`) and never
        // cleared, so once the input changed, getCompletionSuggestions was handed the NEW cursor
        // against the OLD, shorter parse — "Range [0, 2) out of bounds for length 1" — which threw
        // out of updateCommandInfo on every keystroke / history recall, leaving the chat input
        // half-updated. The user pressed Enter and the command ran a mangled name (e.g. "ned"
        // instead of the item they typed). The parse must always describe the text the cursor is a
        // position in, and the cursor is the segment's own length.
        this.currentParse = CommandExecutor.DISPATCHER.parse(
                new StringReader(segment), TungstenMod.mc.getConnection().getSuggestionsProvider());

        if (this.suggestions == null || !this.keepSuggestions) {
            this.pendingSuggestions = CommandExecutor.DISPATCHER.getCompletionSuggestions(
                    this.currentParse, segment.length());
        }

        ci.cancel();
    }
}
