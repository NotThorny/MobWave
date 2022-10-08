package thorny.grasscutters.MobWave.commands;

import emu.grasscutter.server.packet.send.PacketSceneEntityDisappearNotify;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.command.Command;

import emu.grasscutter.game.dungeons.challenge.trigger.*;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import static emu.grasscutter.command.CommandHelpers.*;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.FightProperty;
import emu.grasscutter.game.world.Scene;

import thorny.grasscutters.MobWave.sufferHandler;
import emu.grasscutter.data.excels.MonsterData;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.data.GameData;
import emu.grasscutter.utils.Position;
import emu.grasscutter.Grasscutter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import lombok.Setter;

import java.util.*;
import java.io.*;

// Command usage
@Command(label = "mobwave", aliases = "mw", usage = "start/stop \n /mw create [# waves] [# mobs per wave] [level] [wave time in sec]"
        + "\n Wave time is optional and will default to 300 seconds if not specified")
public class MobWaveCommand implements CommandHandler {
    // Patterns
    public static final Pattern wavesRegex = Pattern.compile("w(\\d+)");
    public static final Pattern timeRegex = Pattern.compile("s(\\d+)");
    public static final Pattern typeRegex = Pattern.compile("t(\\d+)");

    public static WorldChallenge mobWaveChallenge;

    // Challenge triggers
    KillMonsterTrigger killMob = new KillMonsterTrigger();
    InTimeTrigger timeMob = new InTimeTrigger();

    // Lists
    List<EntityMonster> monsters = new ArrayList<EntityMonster>(); // Mob entities
    ArrayList<ChallengeTrigger> cTrigger = new ArrayList<>(); // Challenge triggers
    List<EntityMonster> activeMonsters = new ArrayList<>(); // Current mobs
    SpawnParameters param = new SpawnParameters(); // Custom parameters
    static JsonArray commonMob = new JsonArray(); // Common mobs
    static JsonArray eliteMob = new JsonArray(); // Elite mobs
    static JsonArray bossMob = new JsonArray(); // Boss mobs
    public SceneGroup mobSG = new SceneGroup(); // Mobs scenegroup
    static JsonObject jsonMobs = null; // Mob object from file
    static JsonArray arrMobs = null; // Array of mobs from file
   
    // Defaults
    String userWaveReq = "none"; // Custom wave type set by user
    String waveType = "common"; // Type of wave being spawned
    boolean isWaves = false; // Whether waves are occuring
    int generatedCount = 0; // Spawned mob counter
    int alive = 0; // Number of mobs alive
    int n = 0; // Wave counter
    

    // Taken from SpawnCommand.java with edits made to match 'create'
    private static final Map<Pattern, BiConsumer<SpawnParameters, Integer>> intCommandHandlers = Map.ofEntries(
        Map.entry(lvlRegex, SpawnParameters::setLvl),
        Map.entry(amountRegex, SpawnParameters::setAmount),
        Map.entry(wavesRegex, SpawnParameters::setWaves),
        Map.entry(timeRegex, SpawnParameters::setTime),
        Map.entry(typeRegex, SpawnParameters::setType),
        Map.entry(maxHPRegex, SpawnParameters::setMaxHP),
        Map.entry(hpRegex, SpawnParameters::setHp),
        Map.entry(defRegex, SpawnParameters::setDef),
        Map.entry(atkRegex, SpawnParameters::setAtk)
    );

    // Read file to memory
    public static void readFile() throws FileNotFoundException, IOException {

        try (Reader reader = new InputStreamReader(MobWaveCommand.class.getResourceAsStream("/monsters.json"))) {
            arrMobs = new Gson().fromJson(reader, JsonArray.class);
            reader.close();

            // Add mobs to respective groups
            for(JsonElement curMob : arrMobs){
                String type = curMob.getAsJsonObject().get("Type").getAsString();
                switch (type) {
                    case "common":
                        commonMob.add(curMob);
                        break;
                    case "elite":
                        eliteMob.add(curMob);
                        break;
                    case "boss":
                        bossMob.add(curMob);
                        break;
                    default:
                        // Invalid mob types are ignored
                        break;
                }
            }
        } // try

        catch (IOException e) {
            Grasscutter.getLogger().info("Failed to load file.", e);
        } // catch
    }// readFile

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Wave and mob default settings
        param = new SpawnParameters();
        int nuMobs = 5;     // Number of mobs spawned per wave
        int lvMobs = 90;    // Level of monsters spawned
        int nuWaves = 999;  // Number of waves
        int time = 300;     // Time between waves in seconds
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

        else if (args.get(0).equals("skip")) {
            removeAliveMobs();
            mobWaveChallenge.setSuccess(true);
            CommandHandler.sendMessage(targetPlayer, "Wave skipped!");
        } // else if skip

        // Stops future waves from ocurring
        else if (args.get(0).equals("stop")) {
            try {
                if (mobWaveChallenge.inProgress()) {
                    removeAliveMobs();
                    mobWaveChallenge.fail();
                    isWaves = false;
                    CommandHandler.sendMessage(targetPlayer, "Waves stopped!");
                    return;
                } // if
            } catch (Exception e) {
            } // catch
            if (isWaves) {
                isWaves = false;
            } // if isWaves
            else {
                CommandHandler.sendMessage(targetPlayer, "No waves to stop!");
            } // else
        } // stop

        // Custom wave
        else if (args.get(0).equals("create")) {
            // Get user params
            parseIntParameters(args, param, intCommandHandlers);

            // Set wave settings
            int cWaves = param.waves;
            int cMobs = param.amount;
            int cLevel = param.lvl;
            time = param.time;

            switch (param.type) {
                case 1:
                    userWaveReq = "common";
                    break;
                case 2:
                    userWaveReq = "elite";
                    break;
                case 3:
                    userWaveReq = "boss";
                    break;
                default:
                    userWaveReq = "none";
                    break;
            }

            // Make sure valid arguments
            if (cWaves < 1) {
                cWaves = nuWaves;
            } // if
            if (cMobs < 1) {
                cMobs = nuMobs;
            } // if
            if (cLevel < 0 || cLevel > 200) {
                cLevel = lvMobs;
            } // if

            // Set triggers
            cTrigger.clear();
            cTrigger.add(killMob);
            cTrigger.add(timeMob);
            int step = 0;

            isWaves = true;

            // Set and start challenge
            List<Integer> paramList = List.of(cMobs, time);
            mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time, cMobs,
                    cTrigger);
            mobWaveChallenge.start();
            spawnMobEntity(sender, targetPlayer, args, cMobs, cWaves, arrMobs, cLevel, step, paramList, time, waveType);

        } // create

        // Challenge using wave defaults
        else if (args.get(0).equals("start")) {

            // Determine if wave type is set
            if (args.size() > 1) {
                if (args.get(1).equals("common") || args.get(1).equals("elite") || args.get(1).equals("boss")) {
                    userWaveReq = args.get(1);
                } else {
                    userWaveReq = "none";
                } // else
            } // if

            List<Integer> paramList = List.of(nuMobs, time);
            int step = 0;
            cTrigger.clear();
            cTrigger.add(killMob);
            cTrigger.add(timeMob);
            mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time, nuMobs,
                    cTrigger);
            mobWaveChallenge.start();
            spawnMobEntity(sender, targetPlayer, args, nuMobs, nuWaves, arrMobs, lvMobs, step, paramList, time, waveType);
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

    public MonsterData setMonsterData(int imn) {
        Random pRandom = new Random();
        int chance = pRandom.nextInt(100);
        JsonObject randomMob;

        // Sanity check
        if (waveType.equals(null)) {
            waveType = "common";
        } // if

        // Scale elite spawns
        if (imn > 0 && imn % 4 == 0) {
            imn = 4;
        }
        if (imn > 0 && imn % 3 == 0) {
            imn = 3;
        }

        // Boss wave
        if (n > 0 && (n+1) % 5 == 0) {
            switch (imn) {
                case 4:
                    waveType = "elite";
                    break;
                case 3:
                    waveType = "elite";
                    break;
                case 0:
                    waveType = "boss";
                    break;
                default:
                    waveType = "common";
            } // switch
        } // if
        
        // Normal wave
        else {
            switch (imn) {
                case 4:
                    waveType = "elite";
                    break;
                case 3:
                    waveType = "elite";
                    break;
                case 0:
                    waveType = "common";
                    break;
                default:
                    waveType = "common";
            } // switch
        } // else

        // Custom user wave type
        if(!userWaveReq.equals("none")){
            Grasscutter.getLogger().info("Set custom wave: " + userWaveReq);
            waveType = userWaveReq;
        }

        // Get mob from type and of required chance
        switch (waveType) {
            case "common":
                randomMob = commonMob.get(pRandom.nextInt(commonMob.size())).getAsJsonObject();
                while (randomMob.get("Chance").getAsInt() < chance) {
                    randomMob = commonMob.get(pRandom.nextInt(commonMob.size())).getAsJsonObject();
                }
                break;
            case "elite":
                randomMob = eliteMob.get(pRandom.nextInt(eliteMob.size())).getAsJsonObject();
                while (randomMob.get("Chance").getAsInt() < chance) {
                    randomMob = eliteMob.get(pRandom.nextInt(eliteMob.size())).getAsJsonObject();
                }
                break;
            case "boss":
                randomMob = bossMob.get(pRandom.nextInt(bossMob.size())).getAsJsonObject();
                while (randomMob.get("Chance").getAsInt() < chance) {
                    randomMob = bossMob.get(pRandom.nextInt(bossMob.size())).getAsJsonObject();
                }
                break;
            default:
                // Pick common if no match
                randomMob = commonMob.get(pRandom.nextInt(commonMob.size())).getAsJsonObject();
                break;
        } // switch

        // Set mob
        MonsterData monsterData = GameData.getMonsterDataMap().get((randomMob.get("id").getAsInt()));
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
        if (activeMonsters.isEmpty()) {
            return 0;
        }
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
            JsonArray arrMobs, int mLevel, int step, List<Integer> paramList, int time, String waveType) {
        // Defaults
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        List<EntityMonster> newMonsters = new ArrayList<>();
        generatedCount = 0;
        int goal = nuMobs;
        isWaves = true;

        // Repeat once per second
        executor.scheduleAtFixedRate(() -> {
            // Get current location
            Scene scene = targetPlayer.getScene();
            Position pos = targetPlayer.getPosition();
            newMonsters.clear(); // Clean list
            exceedTime(targetPlayer); // Check for time

            // When timer runs out
            if (exceedTime(targetPlayer)) {
                CommandHandler.sendMessage(targetPlayer, "Ran out of time!");
                mobWaveChallenge.fail();
                endChallenge(executor);
                return;
            } // if

            // Check to spawn
            if (getAliveMonstersCount() <= 0) {
                // Check if waves are completed and shutdown if so
                if (!isWaves || !checkWave(nuWaves)) {
                    CommandHandler.sendMessage(targetPlayer, "Challenge finished!");
                    endChallenge(executor);
                    return;
                } // if

                // Spawns mobs if waves remain and challenge is active
                if (generatedCount < goal && isWaves) {
                    for (int i = 0; i < goal; i++) {
                        MonsterData monsterData = setMonsterData(i);
                        EntityMonster entity = new EntityMonster(scene, monsterData, pos.nearby2d(4f), mLevel);
                        applyCommonParameters(entity, param);
                        scene.addEntity(entity);
                        newMonsters.add(entity);
                        generatedCount++;
                    } // for
                    setMonsters(newMonsters);
                    incrementWaves();

                    // Alert if next wave is boss wave
                    if((n+1)%5==0){
                        CommandHandler.sendMessage(targetPlayer, "Boss incoming next wave!");
                    } // if

                    // Stop waves if only one exists
                    if (nuWaves == 1) {
                        isWaves = false;
                    } // if

                } // if
                newMonsters.clear();

                // Start next wave is previous finished and waves remain
                if (nuWaves > 1) {
                    if (mobWaveChallenge.isSuccess()) {
                        generatedCount = 0;
                        executor.shutdown();
                        mobSG.setId(80085);
                        mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time,
                                nuMobs, cTrigger);
                        mobWaveChallenge.start();
                        spawnMobEntity(sender, targetPlayer, args, nuMobs, nuWaves, arrMobs, mLevel, step, paramList,
                                time, waveType);
                    } // if
                } // if nuWaves
            } // if getAliveMonstersCount
        }, 0, 1, TimeUnit.SECONDS); // executor
    } // spawnMobEntity

    // End challenge and clean up
    private void endChallenge(ScheduledExecutorService executor) {
        executor.shutdown();
        removeAliveMobs();
        isWaves = false;
        resetWaves();
    }

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

    // Taken from SpawnCommand.java
    private static class SpawnParameters {
        @Setter public int lvl = 1;
        @Setter public int time = 300;
        @Setter public int waves = 1;
        @Setter public int amount = 1;
        @Setter public int hp = -1;
        @Setter public int maxHP = -1;
        @Setter public int atk = -1;
        @Setter public int def = -1;
        @Setter public int type = -1;
    }
    // Taken from SpawnCommand.java
    private void applyCommonParameters(EntityMonster entity, SpawnParameters param) {
        if (param.maxHP != -1) {
            entity.setFightProperty(FightProperty.FIGHT_PROP_MAX_HP, param.maxHP);
            entity.setFightProperty(FightProperty.FIGHT_PROP_BASE_HP, param.maxHP);
        }
        if (param.hp != -1) {
            entity.setFightProperty(FightProperty.FIGHT_PROP_CUR_HP, param.hp == 0 ? Float.MAX_VALUE : param.hp);
        }
        if (param.atk != -1) {
            entity.setFightProperty(FightProperty.FIGHT_PROP_ATTACK, param.atk);
            entity.setFightProperty(FightProperty.FIGHT_PROP_CUR_ATTACK, param.atk);
        }
        if (param.def != -1) {
            entity.setFightProperty(FightProperty.FIGHT_PROP_DEFENSE, param.def);
            entity.setFightProperty(FightProperty.FIGHT_PROP_CUR_DEFENSE, param.def);
        }
    }
} // MobWaveCommand
