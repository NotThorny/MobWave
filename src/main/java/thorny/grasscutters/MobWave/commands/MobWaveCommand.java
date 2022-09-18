package thorny.grasscutters.MobWave.commands;

import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.command.commands.SpawnCommand;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.Grasscutter;

import java.util.List;
import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Command usage
@Command(label = "mobwave", aliases = "mw", description = "Spawn a wave of mobs", usage = "mobwave start / mobwave create waves mobs level")
public class MobWaveCommand implements CommandHandler {

    int n = 0;
    static List<String> mobs = null; // Default null list of mobs
 
    public static void readFile (){ // Read file to memory
    try(
    InputStream resource = MobWaveCommand.class.getResourceAsStream("/monsters.txt"))
    {
        mobs = new BufferedReader(new InputStreamReader(resource,
                StandardCharsets.UTF_8)).lines().collect(Collectors.toList());}
    catch(IOException e){
        Grasscutter.getLogger().info("Failed to load file.", e);
    } // catch
    }// readFile

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Defaults for simple start
        int nuMobs = 5;  // Placeholder # of mobs spawned per wave
        int lvMobs = 90; // Placeholder level of monsters spawned
        int nuWaves = 1; // Placeholder # of waves
        long time = 5;   // Time between waves in seconds

        if (args.size() < 1) {
            if (sender != null) {
                CommandHandler.sendMessage(targetPlayer, "/mobwave start|stop or /mw create [waves] [# mobs] [level]");
            }
        } // no args

        else if (args.size() > 4) {
            CommandHandler.sendMessage(targetPlayer,
                    "Too many args, /mobwave start|stop or /mw create [waves] [# mobs] [level]");
        } // exceed size

        else if (args.get(0).equals("create")) {
            int cWaves = Integer.parseInt(args.get(1));
            int cMobs = Integer.parseInt(args.get(2));
            int cLevel = Integer.parseInt(args.get(3));
            List<String> clistMobs = mobs;
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> {
                spawnWaves(sender, targetPlayer, args, cMobs, cWaves, clistMobs, cLevel);
                incrementWaves();
                if (!checkWave(cWaves)) {
                    executor.shutdown();
                    CommandHandler.sendMessage(targetPlayer, "Custom waves finished.");
                    n = 0;
                    return;
                } 
                else {}
            }, 0, time, TimeUnit.SECONDS);

            if(cWaves > 1){
            CommandHandler.sendMessage(targetPlayer,
                    "Custom waves started! You have " + time + " seconds before the next wave starts!");
            }
            
        } // create
        // Does nothing other than send message, needs implementation
        else if (args.get(0).equals("stop")) {
            CommandHandler.sendMessage(targetPlayer, "Waves stopped.");
        } // stop

        else if (args.get(0).equals("start")) {
            spawnWaves(sender, targetPlayer, args, nuMobs, nuWaves, mobs, lvMobs);
            CommandHandler.sendMessage(targetPlayer, "Wave started.");
        } // start

        else {
            CommandHandler.sendMessage(targetPlayer, "/mobwave start|stop or /mw create [waves] [# mobs] [level]");
        } // else

        return;
    }

    // Increase wave counter after wave is spawned
    private void incrementWaves() {
        n++;
    }// incrementWaves

    // Check if the desired number of waves have occured
    private boolean checkWave(int waves) {
        if (n >= waves) {
            return false;
        } else {
            return true;
        }
    }// checkWave

    // Uses old var name refs but works as intended
    public void spawnWaves(Player sender, Player targetPlayer, List<String> args, int nuMobs, int nuWaves,
            List<String> mobs, int mLevel) {
        Random pRandom = new Random();
        for (int i = 0; nuMobs > i; i++) {
            String randomMob = mobs.get(pRandom.nextInt(mobs.size()));
            args.clear(); // Clean the list
            args.add(0, randomMob); // Add mobId to command
            args.add("x1"); // Number of each mob spawned per randomly selected mob id
            args.add("lv"+Integer.toString(mLevel)); // Level of mobs | Change to var for user input
            spawnMob(sender, targetPlayer, args);
        } // nuMobs
    } // spawnWaves

    public void spawnMob(Player sender, Player targetPlayer, List<String> args) {
        SpawnCommand sMob = new SpawnCommand(); // Call SpawnCommand to make monster
        sMob.execute(sender, targetPlayer, args); // Spawn the mob
    }// spawnMob

}
