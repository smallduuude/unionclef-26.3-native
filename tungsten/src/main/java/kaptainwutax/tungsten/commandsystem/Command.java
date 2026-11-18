package kaptainwutax.tungsten.commandsystem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import kaptainwutax.tungsten.Debug;
import kaptainwutax.tungsten.TungstenMod;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;

public abstract class Command {

	protected static final CommandBuildContext REGISTRY_ACCESS = Commands.createValidationContext(VanillaRegistries.createWorldLookup());
    protected static final int SINGLE_SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;
    protected final static SimpleCommandExceptionType INCORRECT_USE = new SimpleCommandExceptionType(Component.literal("Incorrect command use!"));
    
    private final String _name;
    private final String _description;
    protected TungstenMod _mod;
    private Runnable _onFinish = null;

    public Command(String name, String description, TungstenMod mod) {
        _name = name;
        _description = description;
        _mod = mod;
    }

    public void run(TungstenMod mod, String line, Runnable onFinish) throws CommandException {
        _onFinish = onFinish;
        try {
			CommandExecutor.dispatch(line);
		} catch (CommandSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    protected void finish() {
        if (_onFinish != null)
            //noinspection unchecked
            _onFinish.run();
    }
    
    public abstract void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder);
    
    // Helper methods to painlessly infer the CommandSource generic type argument
    protected static <T> RequiredArgumentBuilder<SharedSuggestionProvider, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected static LiteralArgumentBuilder<SharedSuggestionProvider> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }
    
    public final void registerTo(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
        register(dispatcher, _name);
    }
    
    public void register(CommandDispatcher<SharedSuggestionProvider> dispatcher, String name) {
        LiteralArgumentBuilder<SharedSuggestionProvider> builder = LiteralArgumentBuilder.literal(name);
        build(builder);
        dispatcher.register(builder);
    }

//    public String getHelpRepresentation() {
//        StringBuilder sb = new StringBuilder(_name);
//        for (ArgBase arg : REGISTRY_ACCESS.()) {
//            sb.append(" ");
//            sb.append(arg.getHelpRepresentation());
//        }
//        return sb.toString();
//    }

    protected void log(Object message) {
    	Debug.logMessage(message.toString());
    }

    protected void logError(Object message) {
    	Debug.logError(message.toString());
    }

    public String getName() {
        return _name;
    }

    public String getDescription() {
        return _description;
    }
    
    public String toString() {
    	
        return ";" + _name;
    }

    public String toString(String... args) {
        StringBuilder base = new StringBuilder(toString());
        for (String arg : args) base.append(' ').append(arg);
        return base.toString();
    }
}
