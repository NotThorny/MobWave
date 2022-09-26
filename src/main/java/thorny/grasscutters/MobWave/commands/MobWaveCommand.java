package thorny.grasscutters.MobWave.commands;

import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.MonsterData;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.trigger.ChallengeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.InTimeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.KillMonsterTrigger;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType;
import emu.grasscutter.scripts.ScriptLib;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.server.packet.send.PacketSceneEntityDisappearNotify;
import emu.grasscutter.utils.Position;
import thorny.grasscutters.MobWave.sufferHandler;
import emu.grasscutter.Grasscutter;

import java.util.List;
import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.ArrayList;

// Command usage
@Command(label = "mobwave", aliases = "mw", usage = 
    "start/stop \n /mw create [# waves] [# mobs] [level] [wave time in sec]"+
        "\n Wave time is optional and will default to 60 seconds if not specified")
public class MobWaveCommand implements CommandHandler {
    public static WorldChallenge mobWaveChallenge;
    KillMonsterTrigger killMob = new KillMonsterTrigger();
    InTimeTrigger timeMob = new InTimeTrigger();
    
    int n = 0; // Counter
    boolean isWaves = false; // Whether waves are occuring or not
    static List<String> mobs = null; // Default null list of mobs
    List<Integer> mobsInt = new ArrayList<Integer>();
    List<EntityMonster> monsters = new ArrayList<EntityMonster>();
    List<EntityMonster> activeMonsters = new ArrayList<>();
    EntityMonster entMonster;
    MonsterData monsterData;
    ArrayList<ChallengeTrigger> cTrigger = new ArrayList<>();
    int alive = 0;
    String randomMob;
    boolean progress;
    Instant start;
    int generatedCount = 0;
    public SceneGroup mobSG = new SceneGroup();
    public ScriptLib sl = new ScriptLib();

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
        int time = 180;     // Time between waves in seconds;
        mobSG.id = 80085;   // Set scenegroup id
        
        //for(String s : mobs)mobsInt.addAll(mobs.stream().map(Integer::valueOf).collect(Collectors.toList()));
        
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
        }

        // Stops future waves from ocurring
        else if (args.get(0).equals("stop")) {
            try {
                if (mobWaveChallenge.inProgress()) {
                    removeAliveMobs();
                    mobWaveChallenge.fail();
                    CommandHandler.sendMessage(targetPlayer, "Challenge failed!");
                }
            } catch (Exception e) {
            }
            if (isWaves) {
                isWaves = false;
            } // if isWaves
            else {
                CommandHandler.sendMessage(targetPlayer, "No queued waves to stop!");
            } // else
        } // stop

        else if (args.get(0).equals("create")) {
            int cWaves = Integer.parseInt(args.get(1));
            int cMobs = Integer.parseInt(args.get(2));
            int cLevel = Integer.parseInt(args.get(3));

            if (cWaves < 1){
                cWaves = 1;
            }
            
            cTrigger.clear();
            cTrigger.add(killMob);
            cTrigger.add(timeMob);
            int step=0;
            
            isWaves = true;
            // Determine if time was set by user
            if(args.size() > 4){
                if(args.get(4) != null){
                    try {
                        //Set time to match user input
                        time = Integer.valueOf(args.get(4));
                    } catch (NumberFormatException exception) {
                        this.sendUsageMessage(targetPlayer);
                    }
                }//if args
            }//if size
        
            List<Integer> paramList = List.of(cMobs, time);
            mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time, cMobs, cTrigger);
            mobWaveChallenge.start();
            spawnMobEntity(sender, targetPlayer, args, cMobs, cWaves, mobs, cLevel, step, paramList, time);

        } // create

        else if (args.get(0).equals("start")) {
            List<Integer> paramList = List.of(nuMobs, time);
            int step = 0;
            cTrigger.clear();
            cTrigger.add(killMob);
            cTrigger.add(timeMob);
            mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time, nuMobs, cTrigger);
            mobWaveChallenge.start();
            spawnMobEntity(sender, targetPlayer, args, nuMobs, nuWaves, mobs, lvMobs, step, paramList, time);
        } // start

        else {
            this.sendUsageMessage(targetPlayer);
        } // else

        return;
    }
    //Increase wave counter after wave is spawned
    private void incrementWaves() {
        n++;
    }// incrementWaves
    private void resetWaves() {
        n = 0;
    }// resetWaves

    // Check if the desired number of waves have occured
    private boolean checkWave(int waves) {
        if (n >= waves ) {
            return false;
        } else {
            return true;
        }
    }// checkWave

    public MonsterData setMonsterData(){
        Random pRandom = new Random();
        String randomMob = mobs.get(pRandom.nextInt(mobs.size()));
        monsterData = GameData.getMonsterDataMap().get(Integer.parseInt(randomMob));
        return monsterData;
    }
    // Return current challenge
    public WorldChallenge getChallenge(){
        return mobWaveChallenge;
    }
    // Set groupId for new monsters
    public void setMonsters(List<EntityMonster> monsters) {
        activeMonsters.clear();
        activeMonsters.addAll(monsters);
        for (EntityMonster monster : monsters) {
            monster.setGroupId(mobSG.id);
        }
    }
    // Remove currently alive monsters
    public void removeAliveMobs() {
        for (EntityMonster monster : activeMonsters) {
            mobWaveChallenge.getScene().removeEntity(monster, VisionType.VISION_TYPE_REMOVE);
            mobWaveChallenge.getScene()
                    .broadcastPacket(new PacketSceneEntityDisappearNotify(monster, VisionType.VISION_TYPE_REMOVE));
        }
        activeMonsters.clear();
    }
    // Get count of alive monsters
    public int getAliveMonstersCount() {
        int count=0;
        for (EntityMonster monster: activeMonsters) {
            if (monster.isAlive()) {
                count++;
            }
        }
        return count;
    }
    // Return for event listener
	public static int getMobSceneGroup() {
		return 80085;
	}
    // Spawn the monsters
	public void spawnMobEntity(Player sender, Player targetPlayer, List<String> args, int nuMobs, int nuWaves,
    List<String> mobs, int mLevel, int step, List<Integer> paramList, int time){
        List<EntityMonster> newMonsters = new ArrayList<>();
        int goal = nuMobs;
        isWaves = true;
        generatedCount = 0;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
		Scene scene = targetPlayer.getScene();
		Position pos = targetPlayer.getPosition();
        newMonsters.clear();
        exceedTime(targetPlayer);
        
        // Check to spawn
		if (getAliveMonstersCount() <= 2) {
            // Check if waves are completed and shutdown if so
            if (!checkWave(nuWaves) || !isWaves) {
                executor.shutdown();
                activeMonsters.clear();
                if(nuWaves > 1 && n > 1){
                    CommandHandler.sendMessage(targetPlayer, "Last wave nearing end!");
                }
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
				}
				setMonsters(newMonsters);
                incrementWaves();
			}
            newMonsters.clear();
            
            // Check if there are waves remaining and spawn if last wave finished
            if(nuWaves > 1){
                if(mobWaveChallenge.isSuccess() && n < nuWaves && isWaves){
                    generatedCount=0;
                    mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 183, 183, paramList, time, nuMobs, cTrigger);
                    mobWaveChallenge.start();
                    executor.shutdown();
                    spawnMobEntity(sender, targetPlayer, args, nuMobs, nuWaves, mobs, mLevel, step, paramList, time);
                }
            }
		}
        }, 0, 1, TimeUnit.SECONDS);
	}

    // New implementation of timer
    public boolean exceedTime(Player targetPlayer) {
        var current = System.currentTimeMillis();
        CommandHandler.sendMessage(targetPlayer, "Ran out of time!");
        if (current - mobWaveChallenge.getStartedAt() > mobWaveChallenge
                .getTimeLimit() * 1000L) {
            isWaves = false;
            removeAliveMobs();
            mobWaveChallenge.fail();
            return true;
        } else {
            return false;
        }
    }
}
