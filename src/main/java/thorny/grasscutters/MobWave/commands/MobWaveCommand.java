package thorny.grasscutters.MobWave.commands;

import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.command.commands.SpawnCommand;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.MonsterData;
import emu.grasscutter.game.dungeons.challenge.WorldChallenge;
import emu.grasscutter.game.dungeons.challenge.trigger.ChallengeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.InTimeTrigger;
import emu.grasscutter.game.dungeons.challenge.trigger.KillMonsterTrigger;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.scripts.data.SceneGroup;
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
    //int level;
    int goal;
    int alive = 0;
    String randomMob;
    boolean progress;
    Instant start;
    int generatedCount = 0;
    public SceneGroup mobSG = new SceneGroup();

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
        int time = 300;     // Time between waves in seconds;
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
            mobWaveChallenge.fail();
            
            
            if (isWaves) {
                isWaves = false;
                CommandHandler.sendMessage(targetPlayer, 
                    "Stopping waves!");
            } // if isWaves
            else {
                CommandHandler.sendMessage(targetPlayer, "No waves to stop!");
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
            goal = cMobs;
            int step=0;

            //this.candidateMonsters.addAll(mobsInt);
            //createMobList(cMobs, targetPlayer, cLevel, mobs);
            //List<String> clistMobs = mobs;
            
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

            // Old method for spawning monsters
/*              executor.scheduleAtFixedRate(() -> {

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
*/
            // Send wave time message
            // if(cWaves > 1){
            // CommandHandler.sendMessage(targetPlayer,
            //         "Custom waves started! You have " + time + " seconds before the next wave starts!");
            // }
            
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
// Old method no longer in use
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
// **
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

    public void createMobList(int nuMobs, Player targetPlayer, int cLevel, List<String> mobs){
        for(int t = 0; t < nuMobs; t++){
        Random pRandom = new Random();
        String randomMob = mobs.get(pRandom.nextInt(mobs.size()));
        mobsInt.add(Integer.parseInt(randomMob));
        setEntities(randomMob, targetPlayer, cLevel);
        }
    }
    
    public EntityMonster setEntities(String randomMob, Player targetpPlayer, int cLevel){
        monsterData = setMonsterData(); 
        EntityMonster entMonster = new EntityMonster(targetpPlayer.getScene(), monsterData, targetpPlayer.getPosition(), cLevel);
        monsters.add(entMonster);
        setMonsters(monsters);
        return entMonster;
    }

    public MonsterData setMonsterData(){
        Random pRandom = new Random();
        String randomMob = mobs.get(pRandom.nextInt(mobs.size()));
        monsterData = GameData.getMonsterDataMap().get(Integer.parseInt(randomMob));
        return monsterData;
    }

    public WorldChallenge getChallenge(){
        return mobWaveChallenge;
    }
    public void setMonsters(List<EntityMonster> monsters) {
        activeMonsters.clear();
        activeMonsters.addAll(monsters);
        for (EntityMonster monster : monsters) {
            monster.setGroupId(mobSG.id);
        }
    }
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
	public static int getMobScene() {
		return 80085;
	}
    
	public void spawnMobEntity(Player sender, Player targetPlayer, List<String> args, int nuMobs, int nuWaves,
    List<String> mobs, int mLevel, int step, List<Integer> paramList, int time){
        List<EntityMonster> newMonsters = new ArrayList<>();
        int goal = nuMobs;
        isWaves = true;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
		Scene scene = targetPlayer.getScene();
		Position pos = targetPlayer.getPosition();
        newMonsters.clear();
        
		if (getAliveMonstersCount() <= 2) {

            // Check if waves are completed and shutdown if so
            if (!checkWave(nuWaves) || !isWaves) {
                executor.shutdown();
                CommandHandler.sendMessage(targetPlayer, "Custom waves finished.");
                isWaves = false;
                generatedCount = 0;
                resetWaves();
                return;
            } // if

			if (generatedCount < goal) {
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
                if(mobWaveChallenge.isSuccess() && n < nuWaves){
                    generatedCount=0;
                    mobWaveChallenge = new WorldChallenge(targetPlayer.getScene(), mobSG, 180, 180, paramList, time, nuMobs, cTrigger);
                    mobWaveChallenge.start();
                    spawnMobEntity(sender, targetPlayer, args, nuMobs, nuWaves, mobs, mLevel, step, paramList, time);
                }
            }
		}
        }, 0, 1, TimeUnit.SECONDS);
	}
}
