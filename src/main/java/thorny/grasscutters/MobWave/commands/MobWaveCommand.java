package thorny.grasscutters.MobWave.commands;

import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.command.Command.TargetRequirement;
import emu.grasscutter.command.Command;

import emu.grasscutter.game.dungeons.challenge.trigger.*;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import static emu.grasscutter.command.CommandHelpers.*;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.FightProperty;
import thorny.grasscutters.MobWave.MobSpawner;
import thorny.grasscutters.MobWave.sufferHandler;
import emu.grasscutter.scripts.data.SceneGroup;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import lombok.Setter;

import java.util.*;

// Command usage
@Command(label = "mobwave", aliases = "mw", usage = "start|stop \n\n /mw create w[# waves] x[# mobs per wave] lv[level] s[wave time in sec] t[type of mobs]"
        + "\n Types: 1 (common), 2 (elite), 3 (boss). Timer will default to 300 seconds"
        + "\n Start can specify type of mobs to spawn (only that type will spawn): start common|elite|boss", targetRequirement = TargetRequirement.PLAYER)
public class MobWaveCommand implements CommandHandler {
    // Patterns
    public static final Pattern wavesRegex = Pattern.compile("w(\\d+)");
    public static final Pattern timeRegex = Pattern.compile("s(\\d+)");
    public static final Pattern typeRegex = Pattern.compile("t(\\d+)");
    public static final Pattern timesHPRegex = Pattern.compile("hx(\\d+)");

    public static WorldChallenge mobWaveChallenge;

    // Challenge triggers
    KillMonsterTrigger killMob = new KillMonsterTrigger(0);
    InTimeTrigger timeMob = new InTimeTrigger();

    // Lists
    List<EntityMonster> monsters = new ArrayList<EntityMonster>(); // Mob entities
    public static ArrayList<ChallengeTrigger> cTrigger = new ArrayList<>(); // Challenge triggers
    public static SpawnParameters param = new SpawnParameters(); // Custom parameters
    public static SceneGroup mobSG = new SceneGroup(); // Mobs scenegroup
   
    // Defaults
    public static String userWaveReq = "none"; // Custom wave type set by user
    public static String waveType = "common"; // Type of wave being spawned
    public static boolean isWaves = false; // Whether waves are occuring
    public static boolean playerExited = false;

    // Taken from SpawnCommand.java with edits made to match 'create'
    private static final Map<Pattern, BiConsumer<SpawnParameters, Integer>> intCommandHandlers = Map.ofEntries(
        Map.entry(lvlRegex, SpawnParameters::setLvl),
        Map.entry(amountRegex, SpawnParameters::setAmount),
        Map.entry(wavesRegex, SpawnParameters::setWaves),
        Map.entry(timeRegex, SpawnParameters::setTime),
        Map.entry(typeRegex, SpawnParameters::setType),
        Map.entry(maxHPRegex, SpawnParameters::setMaxHP),
        Map.entry(hpRegex, SpawnParameters::setHp),
        Map.entry(timesHPRegex, SpawnParameters::setTiHP), // Times hp
        Map.entry(defRegex, SpawnParameters::setDef),
        Map.entry(atkRegex, SpawnParameters::setAtk)
    );

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        // Wave and mob default settings
        param = new SpawnParameters();
        int uid = targetPlayer.getUid();
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

        else if (args.get(0).equals("suffer")) {
            sufferHandler sufferNow = new sufferHandler();
            sufferNow.sufferExecutor(targetPlayer, targetPlayer, args);
        } // else if suffer

        else if (args.get(0).equals("skip")) {
            MobSpawner.removeAliveMobs();
            mobWaveChallenge.setSuccess(true);
            CommandHandler.sendMessage(targetPlayer, "Wave skipped!");
        } // else if skip

        // Stops future waves from ocurring
        else if (args.get(0).equals("stop")) {
            try {
                if (mobWaveChallenge.inProgress()) {
                    MobSpawner.removeAliveMobs();
                    mobWaveChallenge.fail();
                    isWaves = false;
                    CommandHandler.sendMessage(targetPlayer, "Waves stopped!");
                    return;
                } // if
            } catch (Exception e) {
                // Attempt to stop waves anyways
                isWaves = false;
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

            try{
                if(mobWaveChallenge.inProgress()){
                CommandHandler.sendMessage(targetPlayer, 
                    "Another challenge is currently in progress, please wait for the other challenge to finish!");
                return;
                }
            }catch(Exception e){}
            
            // Get user params
            parseIntParameters(args, param, intCommandHandlers);

            // Set wave settings
            int cWaves = param.waves;
            int cMobs = param.amount;
            int cLevel = param.lvl;
            time = param.time;

            switch (param.type) {
                case 1 -> userWaveReq = "common";
                case 2 -> userWaveReq = "elite";
                case 3 -> userWaveReq = "boss";
                default -> userWaveReq = "none";
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
            MobSpawner.spawnMobEntity(uid, targetPlayer, args, cMobs, cWaves, cLevel, step, paramList, time, waveType);

        } // create

        // Challenge using wave defaults
        else if (args.get(0).equals("start")) {

            try{
                if(mobWaveChallenge.inProgress()){
                CommandHandler.sendMessage(targetPlayer, 
                    "Another challenge is currently in progress, please wait for the other challenge to finish!");
                return;
                }
            }catch(Exception e){}

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
            MobSpawner.spawnMobEntity(uid, targetPlayer, args, nuMobs, nuWaves, lvMobs, step, paramList, time, waveType);
        } // start

        else {
            this.sendUsageMessage(targetPlayer);
        } // else

        return;
    }

    // Return current challenge
    public WorldChallenge getChallenge() {
        return mobWaveChallenge;
    } // getChallenge

    // Return for event listener
    public static int getMobSceneGroup() {
        return 80085;
    } // getMobSceneGroup

    // Taken from SpawnCommand.java
    private static class SpawnParameters {
        @Setter public int lvl = 1;
        @Setter public int time = 300;
        @Setter public int waves = 1;
        @Setter public int amount = 1;
        @Setter public int hp = -1;
        @Setter public int maxHP = -1;
        @Setter public int tiHP = -1;
        @Setter public int atk = -1;
        @Setter public int def = -1;
        @Setter public int type = -1;
    }
    // Taken from SpawnCommand.java
    public static void applyCommonParameters(EntityMonster entity, SpawnParameters param) {
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
        if (param.tiHP != -1) {
            // Increase hp by provided multiplier
            entity.setFightProperty(
                FightProperty.FIGHT_PROP_CUR_HP, param.hp == 0 ? Float.MAX_VALUE :
                (entity.getFightProperty(FightProperty.FIGHT_PROP_CUR_HP) * param.hp));
        }
    }
} // MobWaveCommand
