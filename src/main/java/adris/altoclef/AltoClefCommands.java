package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.commands.multiplayer.*;
import adris.altoclef.commands.random.ScanCommand;
import adris.altoclef.commands.random.DummyTaskCommand;
import adris.altoclef.commandsystem.exception.CommandException;

/**
 * Initializes altoclef's built in commands.
 */
public class AltoClefCommands {

    public static void init() throws CommandException {
        // List commands here.
        //
        // `hidden()` = still registered, still callable by name (scripts, Py4j levers, MCP tools),
        // but not printed by @help. Everything a player needs is visible; the debug hooks, the
        // agent bridge and the multiplayer gimmicks are not.
        //
        // REMOVED: @marvion — a beat-the-game variant of @gamer whose own description read
        // "Unsupported leaving it here in case anyone uses it". One beat-the-game command, one
        // pipeline.
        AltoClef.getCommandExecutor().registerNewCommand(
                new HelpCommand(),
                new GetCommand(),
                new ListCommand(),
                new EquipCommand(),
                new DepositCommand(),
                new StashCommand(),
                new GotoCommand(),
                new IdleCommand(),
                new WanderCommand(),
                new AliveCommand(),
                new MirrorCommand(),
                new HeroCommand(),
                new CoordsCommand(),
                new StatusCommand(),
                new InventoryCommand(),
                new LocateStructureCommand(),
                new StopCommand(),
                new PauseCommand(),
                new UnPauseCommand(),
                new SetGammaCommand().hidden(),
                new TestCommand().hidden(),
                new FoodCommand(),
                new MeatCommand(),
                new ReloadSettingsCommand(),
                new GamerCommand(),
                new DummyTaskCommand().hidden(),
                new FollowCommand(),
                new ScanCommand().hidden(),
                new GiveCommand(),
                new PunkCommand(),
                new SelfCareCommand(),
                // Agent / custom tasks / settings
                new AgentCommand().hidden(),
                new CustomCommand().hidden(),
                new SetSettingsCommand(),
                new TimeoutCommand(),
                new TimeoutSpecificCommand(),
                new CoverWithBlocksCommand(),
                new CoverWithSandCommand(),
                // Multiplayer commands
                new AvoidCommand(),
                new CombatCommand(),
                new PursueCommand(),
                new GestureCommand().hidden(),
                new ShiftCommand().hidden(),
                new ShootCommand(),
                new ConnectCommand(),
                new SignCommand().hidden(),
                new GraveCommand().hidden(),
                new GameCommand().hidden(),
                new NickCommand().hidden(),
                new CheckPlayerCommand().hidden(),
                new PearlCommand().hidden(),
                new ForgetCommand(),
                new BuildCommand(),
                new PlaceCommand(),
                new MineBlockCommand(),
                new CheckBlockCommand().hidden()
        );
    }
}
