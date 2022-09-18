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
@Command(label = "mobwave", aliases = "mw", usage = 
    "start/stop \n /mw create [# waves] [# mobs] [level] [wave time in sec]"+
        "\n Wave time is optional and will default to 60 seconds if not specified")
public class MobWaveCommand implements CommandHandler {

    int n = 0; // Counter
    boolean isWaves = false; // Whether waves are occuring or not
    static List<String> mobs = null; // Default null list of mobs

    public static void readFile (){         // Read file to memory
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
        int nuMobs = 5;     // Placeholder # of mobs spawned per wave
        int lvMobs = 90;    // Placeholder level of monsters spawned
        int nuWaves = 1;    // Placeholder # of waves
        long time = 60;     // Time between waves in seconds
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        if (args.size() < 1) {
            if (sender != null) {
                this.sendUsageMessage(targetPlayer);
            } // sender exists
        } // no args

        else if (args.size() > 5) {
            CommandHandler.sendMessage(targetPlayer, "Too many arguments!");
            this.sendUsageMessage(targetPlayer);
        } // exceed size

        // Stops future waves from ocurring
        else if (args.get(0).equals("stop")) {
            if (isWaves) {
                isWaves = false;
                CommandHandler.sendMessage(targetPlayer, 
                    "Stopping waves! \nPlease wait until current wave timer ends (finished message appears) to start a new custom wave.");
            } // if isWaves
            else {
                CommandHandler.sendMessage(targetPlayer, "No waves to stop!");
            } // else
        } // stop

        else if (args.get(0).equals("create")) {
            int cWaves = Integer.parseInt(args.get(1));
            int cMobs = Integer.parseInt(args.get(2));
            int cLevel = Integer.parseInt(args.get(3));
            List<String> clistMobs = mobs;
            isWaves = true;

            // Determine if time was set by user
            if(args.size() > 4){
                if(args.get(4) != null){
                    try {
                        //Set time to match user input
                        time = Long.parseLong(args.get(4));
                    } catch (NumberFormatException exception) {
                        this.sendUsageMessage(targetPlayer);
                    }
                }//if args
            }//if size
            
            executor.scheduleAtFixedRate(() -> {

                // Spawn wave
                if (isWaves) {
                    spawnWaves(sender, targetPlayer, args, cMobs, cWaves, clistMobs, cLevel);
                    incrementWaves();
                } // else

                // Check if there are waves remaining
                if (!checkWave(cWaves) || !isWaves) {
                    executor.shutdown();
                    CommandHandler.sendMessage(targetPlayer, "Custom waves finished.");
                    isWaves = false;
                    n = 0;
                    return;
                } // if

            }, 0, time, TimeUnit.SECONDS);

            // Send wave time message
            if(cWaves > 1){
            CommandHandler.sendMessage(targetPlayer,
                    "Custom waves started! You have " + time + " seconds before the next wave starts!");
            }
            
        } // create

        else if (args.get(0).equals("start")) {
            spawnWaves(sender, targetPlayer, args, nuMobs, nuWaves, mobs, lvMobs);
            CommandHandler.sendMessage(targetPlayer, "Wave started.");
        } // start

        else {
            this.sendUsageMessage(targetPlayer);
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
            args.clear();           // Clean the list
            args.add(0, randomMob); // Add mobId to command
            args.add("x1");         // Number of each mob spawned per randomly selected mob id
            args.add("lv"+Integer.toString(mLevel)); // Mob level
            spawnMob(sender, targetPlayer, args);    // Send to spawn mob
        } // nuMobs
    } // spawnWaves

    public void spawnMob(Player sender, Player targetPlayer, List<String> args) {
        SpawnCommand sMob = new SpawnCommand();     // Call SpawnCommand to make monster
        sMob.execute(sender, targetPlayer, args);   // Spawn the mob
    }// spawnMob

}
