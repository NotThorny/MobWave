package thorny.grasscutters.MobWave;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.monster.MonsterData;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType;
import emu.grasscutter.game.world.Position;
import thorny.grasscutters.MobWave.commands.MobWaveCommand;

public class MobSpawner {

    public static List<Integer> commonMob = monsterLists.getCommonMobs(); // Common mobs
    public static List<Integer> eliteMob = monsterLists.getEliteMobs(); // Elite mobs
    public static List<Integer> bossMob = monsterLists.getBossMobs(); // Boss mobs
    
    public static int generatedCount = 0; // Spawned mob counter
    static int alive = 0; // Number of mobs alive
    public static int n = 0; // Wave counter
    public static List<GameEntity> activeMonsters = new ArrayList<>(); // Current mobs

    // Increase wave counter after wave is spawned
    public static void incrementWaves() {
        MobSpawner.n++;
    }// incrementWaves

    public static void resetWaves() {
        MobSpawner.n = 0;
    }// resetWaves

    // Check if the desired number of waves have occured
    public static boolean checkWave(int waves) {
        if (MobSpawner.n >= waves) {
            return false;
        } else {
            return true;
        } // else
    }// checkWave

    public static MonsterData setMonsterData(int imn) {
        Random pRandom = new Random();
        int randomMob;
    
        // Sanity check
        if (MobWaveCommand.waveType.equals(null)) {
            MobWaveCommand.waveType = "common";
        } // if
    
        // Scale elite spawns
        if (imn > 0 && imn % 4 == 0) {
            imn = 4;
        }
        if (imn > 0 && imn % 3 == 0) {
            imn = 3;
        }
    
        // Boss wave
        if (MobSpawner.n > 0 && (MobSpawner.n + 1) % 5 == 0) {
            switch (imn) {
                case 4 -> MobWaveCommand.waveType = "elite";
                case 3 -> MobWaveCommand.waveType = "elite";
                case 0 -> MobWaveCommand.waveType = "boss";
                default -> MobWaveCommand.waveType = "common";
            } // switch
        } // if
    
        // Normal wave
        else {
            switch (imn) {
                case 4 -> MobWaveCommand.waveType = "elite";
                case 3 -> MobWaveCommand.waveType = "elite";
                case 0 -> MobWaveCommand.waveType = "common";
                default -> MobWaveCommand.waveType = "common";
            } // switch
        } // else
    
        // Custom user wave type
        if(!MobWaveCommand.userWaveReq.equals("none")){
            Grasscutter.getLogger().info("Set custom wave: " + MobWaveCommand.userWaveReq);
            MobWaveCommand.waveType = MobWaveCommand.userWaveReq;
        }
    
        // Get random mob from type
        switch (MobWaveCommand.waveType) {
            case "common" ->  randomMob = commonMob.get(pRandom.nextInt(commonMob.size()));
            case "elite" -> randomMob = eliteMob.get(pRandom.nextInt(eliteMob.size()));
            case "boss" -> randomMob = bossMob.get(pRandom.nextInt(bossMob.size()));
            // Use common mob when no match
            default -> randomMob = commonMob.get(pRandom.nextInt(commonMob.size()));
        } // switch
    
        // Set mob
        MonsterData monsterData = GameData.getMonsterDataMap().get(randomMob);
        return monsterData;
    } // setMonsterData

    // Set groupId for new monsters
    public static void setMonsters(List<EntityMonster> monsters) {
        MobSpawner.activeMonsters.clear();
        MobSpawner.activeMonsters.addAll(monsters);
        for (EntityMonster monster : monsters) {
            monster.setGroupId(MobWaveCommand.mobSG.id);
        } // for
    } // setMonsters

    // Spawn the monsters
    public static void spawnMobEntity(int uid, Player targetPlayer, List<String> args, int nuMobs, int nuWaves,
        int mLevel, int step, List<Integer> paramList, int time, String waveType) {
        // Defaults
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        List<EntityMonster> newMonsters = new ArrayList<>();
        MobSpawner.generatedCount = 0;
        int goal = nuMobs;
        MobWaveCommand.isWaves = true;
    
        // Repeat once per second
        executor.scheduleAtFixedRate(() -> {
            // Get current location
            Scene scene = targetPlayer.getScene();
            Position pos = targetPlayer.getPosition();
            newMonsters.clear(); // Clean list
            exceedTime(targetPlayer); // Check for time
    
            if(!(targetPlayer.getServer().getPlayerByUid(uid, true).isOnline())){
                Grasscutter.getLogger().info("[MobWave] Player logged out so challenge ended.");
                MobWaveCommand.mobWaveChallenge.fail();
                endChallenge(executor);
                MobWaveCommand.playerExited = false;
            }
    
            // When timer runs out
            if (exceedTime(targetPlayer)) {
                CommandHandler.sendMessage(targetPlayer, "Ran out of time!");
                MobWaveCommand.mobWaveChallenge.fail();
                endChallenge(executor);
                return;
            } // if
    
            // Check to spawn
            if (MobSpawner.getAliveMonstersCount() <= 0) {
                // Check if waves are completed and shutdown if so
                if (!MobWaveCommand.isWaves || !checkWave(nuWaves)) {
                    CommandHandler.sendMessage(targetPlayer, "Challenge finished!");
                    endChallenge(executor);
                    return;
                } // if
    
                // Spawns mobs if waves remain and challenge is active
                if (MobSpawner.generatedCount < goal && MobWaveCommand.isWaves) {
                    for (int i = 0; i < goal; i++) {
                        MonsterData monsterData = setMonsterData(i);
                        EntityMonster entity = new EntityMonster(scene, monsterData, pos.nearby2d(4f), mLevel);
                        MobWaveCommand.applyCommonParameters(entity, MobWaveCommand.param);
                        scene.addEntity(entity);
                        newMonsters.add(entity);
                        MobSpawner.generatedCount++;
                    } // for
                    setMonsters(newMonsters);
                    incrementWaves();
    
                    // Alert if next wave is boss wave
                    if((MobSpawner.n+1)%5==0){
                        CommandHandler.sendMessage(targetPlayer, "Boss incoming next wave!");
                    } // if
    
                    // Stop waves if only one exists
                    if (nuWaves == 1) {
                        MobWaveCommand.isWaves = false;
                    } // if
    
                } // if
                newMonsters.clear();
    
                // Start next wave is previous finished and waves remain
                if (nuWaves > 1) {
                    if (MobWaveCommand.mobWaveChallenge.isSuccess()) {
                        MobSpawner.generatedCount = 0;
                        executor.shutdown();
                        MobWaveCommand.mobSG.setId(80085);
                        MobWaveCommand.mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), MobWaveCommand.mobSG, 180, 180, paramList, time,
                                nuMobs, MobWaveCommand.cTrigger);
                        MobWaveCommand.mobWaveChallenge.start();
                        spawnMobEntity(uid, targetPlayer, args, nuMobs, nuWaves, mLevel, step, paramList,
                                time, waveType);
                    } // if
                } // if nuWaves
            } // if getAliveMonstersCount
        }, 0, 1, TimeUnit.SECONDS); // executor
    } // spawnMobEntity

    // End challenge and clean up
    public static void endChallenge(ScheduledExecutorService executor) {
        executor.shutdown();
        MobSpawner.removeAliveMobs();
        MobWaveCommand.isWaves = false;
        resetWaves();
    }

    // Check if wave has exceeded time limit
    private static boolean exceedTime(Player targetPlayer) {
        var current = System.currentTimeMillis();
        // If time is exceeded
        if (current - MobWaveCommand.mobWaveChallenge.getStartedAt() > MobWaveCommand.mobWaveChallenge
                .getTimeLimit() * 1000L) {
            return true;
        } else {
            return false;
        } // else
    } // exceedTime

    // Remove currently alive monsters
    public static void removeAliveMobs() {
        MobWaveCommand.mobWaveChallenge.getScene().removeEntities(MobSpawner.activeMonsters, VisionType.VISION_TYPE_REMOVE);
        MobSpawner.activeMonsters.clear();
    } // removeAliveMobs

    // Get count of alive monsters
    public static int getAliveMonstersCount() {
        int count = 0;
        if (MobSpawner.activeMonsters.isEmpty()) {
            return 0;
        }
        for (GameEntity monster : MobSpawner.activeMonsters) {
            if (monster.isAlive()) {
                count++;
            } // if
        } // for
        return count;
    } // getAliveMonstersCount  
}
