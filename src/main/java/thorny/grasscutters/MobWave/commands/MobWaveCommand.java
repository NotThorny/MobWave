package thorny.grasscutters.MobWave.commands;

import emu.grasscutter.server.packet.send.PacketSceneEntityDisappearNotify;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.command.Command;

import emu.grasscutter.game.dungeons.challenge.trigger.KillMonsterTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.ChallengeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.InTimeTrigger;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Scene;

import emu.grasscutter.data.excels.MonsterData;
import emu.grasscutter.data.GameData;

import thorny.grasscutters.MobWave.sufferHandler;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.utils.Position;
import emu.grasscutter.Grasscutter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.*;
import java.io.*;

// Command usage
@Command(label = "mobwave", aliases = "mw", usage = "start/stop \n /mw create [# waves] [# mobs] [level] [wave time in sec]"
        + "\n Wave time is optional and will default to 180 seconds if not specified")
public class MobWaveCommand implements CommandHandler {
    public static WorldChallenge mobWaveChallenge;

    // Challenge triggers
    KillMonsterTrigger killMob = new KillMonsterTrigger();
    InTimeTrigger timeMob = new InTimeTrigger();

    // Lists
    List<EntityMonster> monsters = new ArrayList<EntityMonster>();
    ArrayList<ChallengeTrigger> cTrigger = new ArrayList<>();
    List<EntityMonster> activeMonsters = new ArrayList<>();
    public SceneGroup mobSG = new SceneGroup();

    // Defaults
    static List<String> mobs = null; // List of mobs to read file
    boolean isWaves = false; // Whether waves are occuring or not
    int generatedCount = 0; // Spawned mob counter
    int alive = 0; // Number of mobs alive
    int n = 0; // Wave counter

    // Read file to memory
    public static void readFile() {
        try (
                InputStream resource = MobWaveCommand.class.getResourceAsStream("/monsters.txt")) {
            mobs = new BufferedReader(new InputStreamReader(resource,
                    StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
        } // try

        catch (IOException e) {
            Grasscutter.getLogger().info("Failed to load file.", e);
        } // catch
    }// readFile

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Wave and mob default settings
        int nuMobs = 5;     // Number of mobs spawned per wave
        int lvMobs = 90;    // Level of monsters spawned
        int nuWaves = 1;    // Number of waves
        int time = 180;     // Time between waves in seconds;
        mobSG.id = 80085;   // Scenegroup id

        if (args.size() < 1) {
            if (sender != null) {
                this.sendUsageMessage(targetPlayer);
            } // sender exists
        } // no args

        else if (args.size() > 5) {
            CommandHandler.sendMessage(targetPlayer, "Too many arguments!");
            this.sendUsageMessage(targetPlayer);
        } // exceed size

        else if (args.get(0).equals("suffer")) {
            sufferHandler sufferNow = new sufferHandler();
            sufferNow.sufferExecutor(targetPlayer, targetPlayer, args);
        } // else if suffer

        // Stops future waves from ocurring
        else if (args.get(0).equals("stop")) {
            try {
                if (mobWaveChallenge.inProgress()) {
                    removeAliveMobs();
                    mobWaveChallenge.fail();
                    CommandHandler.sendMessage(targetPlayer, "Challenge failed!");
                    return;
                } // if
            } catch (Exception e) {
            } // catch
            if (isWaves) {
                isWaves = false;
            } // if isWaves
            else {
                CommandHandler.sendMessage(targetPlayer, "No queued waves to stop!");
            } // else
        } // stop

        // Custom wave
        else if (args.get(0).equals("create")) {
            // Set wave settings
            int cWaves = Integer.parseInt(args.get(1));
            int cMobs = Integer.parseInt(args.get(2));
            int cLevel = Integer.parseInt(args.get(3));

            // Make sure valid arguments
            if (cWaves < 1) {
                cWaves = nuWaves;
            } // if
            if(cMobs < 1){
                cMobs = nuMobs;
            } // if
            if(cLevel < 0 || cLevel > 200){
                cLevel = lvMobs;
            } // if

            // Set triggers
            cTrigger.clear();
            cTrigger.add(killMob);
            cTrigger.add(timeMob);
            int step = 0;

            isWaves = true;
            // Determine if time was set by user
            if (args.size() > 4) {
                if (args.get(4) != null) {
                    try {
                        // Set time to match user input
                        time = Integer.valueOf(args.get(4));
                    } catch (NumberFormatException exception) {
                        this.sendUsageMessage(targetPlayer);
                    }
                } // if args
            } // if size

            // Set and start challenge
            List<Integer> paramList = List.of(cMobs, time);
            mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time, cMobs,
                    cTrigger);
            mobWaveChallenge.start();
            spawnMobEntity(sender, targetPlayer, args, cMobs, cWaves, mobs, cLevel, step, paramList, time);

        } // create

        // Challenge using wave defaults
        else if (args.get(0).equals("start")) {
            List<Integer> paramList = List.of(nuMobs, time);
            int step = 0;
            cTrigger.clear();
            cTrigger.add(killMob);
            cTrigger.add(timeMob);
            mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time, nuMobs,
                    cTrigger);
            mobWaveChallenge.start();
            spawnMobEntity(sender, targetPlayer, args, nuMobs, nuWaves, mobs, lvMobs, step, paramList, time);
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

    private void resetWaves() {
        n = 0;
    }// resetWaves

    // Check if the desired number of waves have occured
    private boolean checkWave(int waves) {
        if (n >= waves) {
            return false;
        } else {
            return true;
        } // else
    }// checkWave

    public MonsterData setMonsterData() {
        Random pRandom = new Random();
        String randomMob = mobs.get(pRandom.nextInt(mobs.size()));
        MonsterData monsterData = GameData.getMonsterDataMap().get(Integer.parseInt(randomMob));
        return monsterData;
    } // setMonsterData

    // Return current challenge
    public WorldChallenge getChallenge() {
        return mobWaveChallenge;
    } // getChallenge

    // Set groupId for new monsters
    public void setMonsters(List<EntityMonster> monsters) {
        activeMonsters.clear();
        activeMonsters.addAll(monsters);
        for (EntityMonster monster : monsters) {
            monster.setGroupId(mobSG.id);
        } // for
    } // setMonsters

    // Remove currently alive monsters
    public void removeAliveMobs() {
        for (EntityMonster monster : activeMonsters) {
            mobWaveChallenge.getScene().removeEntity(monster, VisionType.VISION_TYPE_REMOVE);
            mobWaveChallenge.getScene()
                    .broadcastPacket(new PacketSceneEntityDisappearNotify(monster, VisionType.VISION_TYPE_REMOVE));
        } // for
        activeMonsters.clear();
    } // removeAliveMobs

    // Get count of alive monsters
    public int getAliveMonstersCount() {
        int count = 0;
        for (EntityMonster monster : activeMonsters) {
            if (monster.isAlive()) {
                count++;
            } // if
        } // for
        return count;
    } // getAliveMonstersCount

    // Return for event listener
    public static int getMobSceneGroup() {
        return 80085;
    } // getMobSceneGroup

    // Spawn the monsters
    public void spawnMobEntity(Player sender, Player targetPlayer, List<String> args, int nuMobs, int nuWaves,
            List<String> mobs, int mLevel, int step, List<Integer> paramList, int time) {
        // Defaults
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        List<EntityMonster> newMonsters = new ArrayList<>();
        generatedCount = 0;
        int goal = nuMobs;
        isWaves = true;
        
        // Repeat once per second
        executor.scheduleAtFixedRate(() -> {
            Scene scene = targetPlayer.getScene();
            Position pos = targetPlayer.getPosition();
            newMonsters.clear();
            exceedTime(targetPlayer);

            // When timer runs out
            if(exceedTime(targetPlayer)){
                CommandHandler.sendMessage(targetPlayer, "Ran out of time!");
                executor.shutdown();
                removeAliveMobs();
                mobWaveChallenge.fail();
                isWaves = false;
                resetWaves();
                return;
            } // if

            // Check to spawn
            if (getAliveMonstersCount() <= 0) {
                // Check if waves are completed and shutdown if so
                if (!isWaves || !checkWave(nuWaves)) {
                    CommandHandler.sendMessage(targetPlayer, "Challenge finished!");
                    executor.shutdown();
                    activeMonsters.clear();
                    isWaves = false;
                    resetWaves();
                    return;
                } // if

                // Spawns mobs if waves remain and challenge is active
                if (generatedCount < goal && isWaves) {
                    for (int i = 0; i < goal; i++) {
                        MonsterData monsterData = setMonsterData();
                        EntityMonster entity = new EntityMonster(scene, monsterData, pos.nearby2d(4f), mLevel);
                        scene.addEntity(entity);
                        newMonsters.add(entity);
                        generatedCount++;
                    } // for
                    setMonsters(newMonsters);
                    incrementWaves();

                    if(nuWaves == 1){
                        isWaves = false;
                    }
                    
                } // if
                newMonsters.clear();

                // Check if there are waves remaining and spawn if last wave finished
                if (nuWaves > 1) {
                    if (mobWaveChallenge.isSuccess()) {
                        generatedCount = 0;
                        executor.shutdown();
                        mobSG.setId(80085);
                        mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time,
                                nuMobs, cTrigger);
                        mobWaveChallenge.start();
                        spawnMobEntity(sender, targetPlayer, args, nuMobs, nuWaves, mobs, mLevel, step, paramList,
                                time);
                    } // if
                } // if nuWaves
            } // if getAliveMonstersCount
        }, 0, 1, TimeUnit.SECONDS); // executor
    } // spawnMobEntity

    // Check if wave has exceeded time limit
    private boolean exceedTime(Player targetPlayer) {
        var current = System.currentTimeMillis();
        // If time is exceeded
        if (current - mobWaveChallenge.getStartedAt() > mobWaveChallenge
                .getTimeLimit() * 1000L) {
            return true;
        } else {
            return false;
        } // else
    } // exceedTime
} // MobWaveCommand
