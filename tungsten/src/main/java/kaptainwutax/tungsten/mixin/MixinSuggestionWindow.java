package kaptainwutax.tungsten.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.suggestion.Suggestion;

import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.helpers.StringProcessorHelper;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;

/**
 * Handles applying a suggestion when the input chains commands with {@code |}.
 *
 * <p>26.3 port: the outer {@code CommandSuggestions} reference was the Yarn-named synthetic field
 * {@code field_21615}; in mojmap (unobfuscated) the synthetic outer field is literally
 * {@code this$0}, which is what we shadow now.
 */
@Mixin(CommandSuggestions.SuggestionsList.class)
public class MixinSuggestionWindow {
	@Shadow
	@Final
	CommandSuggestions this$0;
	@Shadow
	private int current;
	@Shadow
	@Final
	private List<Suggestion> suggestionList;

	@Inject(method = "useSuggestion", at = @At("HEAD"), cancellable = true)
	private void overwriteComplete(CallbackInfo ci) {
		AccessorChatInputSuggestor inputSuggestor = (AccessorChatInputSuggestor) this.this$0;
		if (inputSuggestor == null) return;
		EditBox textFieldWidget = inputSuggestor.getInput();
		Suggestion suggestion = this.suggestionList.get(this.current);
		int just = suggestion.getRange().getStart() + suggestion.getText().length();
		if (textFieldWidget.getValue().contains("|")) {
			int closestIDXOfDivider = StringProcessorHelper.findClosestCharIndex(textFieldWidget.getValue(), '|', textFieldWidget.getCursorPosition() - 1);
			if (TungstenMod.getCommandExecutor().allCommands().stream().anyMatch(cmd -> cmd.getName().equals(suggestion.getText()))) {
				textFieldWidget.deleteChars(closestIDXOfDivider + 1 - textFieldWidget.getCursorPosition());
			} else {
				closestIDXOfDivider = StringProcessorHelper.findClosestCharIndex(textFieldWidget.getValue(), ' ', textFieldWidget.getCursorPosition() - 1);
				textFieldWidget.deleteChars(closestIDXOfDivider + 1 - textFieldWidget.getCursorPosition());
			}
			textFieldWidget.setHighlightPos(textFieldWidget.getValue().length());
			textFieldWidget.insertText(suggestion.getText());
			ci.cancel();
		}
	}
}
