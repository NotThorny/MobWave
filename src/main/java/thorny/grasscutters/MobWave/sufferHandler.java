package thorny.grasscutters.MobWave;

import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.command.commands.SpawnCommand;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.world.Position;
import emu.grasscutter.server.packet.send.PacketSceneEntityAppearNotify;

import java.util.List;
import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class sufferHandler implements CommandHandler {

	//Thanks to Snoobi for making suffer happen

    int n = 0;
    static List<String> sufferMobs = null;
	public static void readFile (){         // Read file to memory
		try(
		InputStream resource = sufferHandler.class.getResourceAsStream("/suffer.txt"))
		{
			sufferMobs = new BufferedReader(new InputStreamReader(resource,
					StandardCharsets.UTF_8)).lines().collect(Collectors.toList());}
		catch(IOException e){
			Grasscutter.getLogger().info("Failed to load file.", e);
		} // catch
		}// readFile

    public void sufferExecutor(Player sender, Player targetPlayer, List<String> args) {
        // Defaults for simple start
		readFile();
		String state = "start";
        int nuMobs = 3;		// Placeholder # of mobs spawned per wave
		int nuWaves = 50;	// Placeholder # of waves 
        int lvMobs = 100;	// Placeholder level of monsters spawned
		float radius = 8f;	// Placeholder radius of tp and spawns
		float icd = 2500f;	// Placeholder teleport and switch char cd in milliseconds
		final Position pos = new Position(0, 0, 0); //for reference to base place
		final Position rot = new Position(targetPlayer.getRotation().getX(),targetPlayer.getRotation().getY(),targetPlayer.getRotation().getZ()); //we sure dont want char wondering frm base pos
		//catch args for parems yey
		switch (args.size()) {
			case 6:
				try {
					icd = Float.parseFloat(args.get(5));
				} catch (NumberFormatException e) {
					CommandHandler.sendMessage(sender,"invalid interval of tp");
				}
			case 5:
				try {
					radius = Float.parseFloat(args.get(4));
				} catch (NumberFormatException e) {
					CommandHandler.sendMessage(sender,"invalid radius of tp and spawns");
				}
			case 4:
				try {
					lvMobs = Integer.parseInt(args.get(2));
				} catch (NumberFormatException ignored) {
					CommandHandler.sendMessage(sender,"invalid level");
				}
			case 3:
				try {
					nuWaves = Integer.parseInt(args.get(3));
				} catch (NumberFormatException e) {
					CommandHandler.sendMessage(sender,"invalid number of waves");
				}
            case 2:
                try {
                    nuMobs = Integer.parseInt(args.get(1));
                } catch (NumberFormatException ignored) {
                    CommandHandler.sendMessage(sender, "invalid number of Mobs per interval");
                }
            case 1:
				if (args.get(0).toLowerCase().equals("start") || args.get(0).toLowerCase().equals("stop") || args.get(0).toLowerCase().equals("end")
						|| args.get(0).toLowerCase().equals("suffer")) {
                    state = args.get(0).toLowerCase();
                } else {
					CommandHandler.sendMessage(sender,"invalid state");
				}
				break;
			case 0:
				CommandHandler.sendMessage(sender,"used default settings of start,10,10,100,2f,5f");
				break;
            default:
                CommandHandler.sendMessage(sender,"invalid args ew");
				this.sendUsageMessage(sender);
                return;
        }
		
		//tp to 0 0 0 for now
		//targetPlayer.getPosition().set(0,0,0);                									//DEFAULT PLACE FOR FIGHT
		targetPlayer.getPosition().set(pos);                                 			
		targetPlayer.getWorld().transferPlayerToScene(targetPlayer,50099,pos);   					//IF NEED TP TO NEW SCENE
		targetPlayer.getScene().broadcastPacket(new PacketSceneEntityAppearNotify(targetPlayer));   //TRANSFER TO CURRENT POS AFTER DONE
		
		//finals coz java demands lambda snoo to have final vars
		final int nuMobsFinal = nuMobs;
		final int nuWavesFinal = nuWaves;
		final int lvMobsFinal = lvMobs;
		final float radiusFinal = radius;
		long icdFinal = (long)(Math.round(icd));
		final Player targetPlayerFinal = targetPlayer;
		
			
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
			
			// combined if (!checkWave(nuWaves)) { //CHECKS FOR BASECASE
			if ((n >= nuWavesFinal)) {
				targetPlayerFinal.getPosition().set(pos.getX(), pos.getY(), pos.getZ());
				executor.shutdown();
				CommandHandler.sendMessage(targetPlayerFinal, "Custom waves finished.");
				n = 0;
				return;
			}

			// slide arnd
			targetPlayerFinal.getPosition().set(GetRandomPosition(pos, radiusFinal).getX(),
					targetPlayerFinal.getPosition().getY(), GetRandomPosition(pos, radiusFinal).getZ()); // tp random nearby

			targetPlayerFinal.getRotation().set(GetRandomPosition(rot, 360).getX(), GetRandomPosition(rot, 360).getY(),
					GetRandomPosition(rot, 360).getZ()); // rotate random
				
			targetPlayerFinal.getScene().broadcastPacket(new PacketSceneEntityAppearNotify(targetPlayerFinal)); // slide
																												// arnd
																												// heh

			// combined spawnWaves(sender, targetPlayer, args, nuMobs, nuWaves, clistMobs,
			// lvMobs, radius, pos, rot);
			Random pRandom = new Random(); 			// Initialises random object for getting random mob id per iteration
			for (int i = 0; nuMobsFinal > i; i++) { // Increment to number of wav
				String randomMob = sufferMobs.get(pRandom.nextInt(sufferMobs.size())); // Get random mobId from list
																					   // earlier
				args.clear(); 		 // Clean the list for next spawn command
				args.add(randomMob); // Add mobId to spawn command parems
				args.add("x1"); 	 // Number of each mob spawned per randomly selected mob id
				args.add("lv" + Integer.toString(+lvMobsFinal)); // Level of mobs | Change to var for user input
				args.add(String.valueOf(GetRandomPosition(pos, radiusFinal).getX())); // Get random x
				args.add(String.valueOf(targetPlayerFinal.getPosition().getY())); 	  // Get y of current player to prevent
																					  // floating and sinking enemies
				args.add(String.valueOf(GetRandomPosition(pos, radiusFinal).getZ())); // Get random z

				// spawnMob(sender, targetPlayer, args);
				SpawnCommand sMob = new SpawnCommand(); // Call SpawnCommand to make monster
				sMob.execute(sender, targetPlayerFinal, args);
			}

			// combined incrementWaves();
			n++;

		}, icdFinal, icdFinal, TimeUnit.MILLISECONDS); // changed the timing of each interval here

		if (nuWavesFinal > 1) {
			CommandHandler.sendMessage(targetPlayer,
				"Custom waves started! You have " + icd + " seconds before the next wave starts!");
		}

	}
	
	
	private Position GetRandomPosition(Position pos,float radius) { // rand pos creator, pos is  current pos and radius is max distance the new pos is frm original pos
		Position posNew = new Position();
		Random rand = new Random();                                                         
		posNew.setX((float) (pos.getX() + (rand.nextInt(3)-1) * (Math.random() * radius))); // rand.nextInt(3) - 1 returns -1,0,1 for chance of (-) for full coverage
		posNew.setY((float) (pos.getY() + (rand.nextInt(3)-1) * (Math.random() * radius)));
		posNew.setZ((float) (pos.getZ() + (rand.nextInt(3)-1) * (Math.random() * radius)));
		return posNew;
	}
	
	// SPAWNING
    // 1.2.3
	/*public void spawnWaves(Player sender, Player targetPlayer, List<String> args, int nuMobs, int nuWaves, List<String> mobs, int lvMobs, float radius, Position pos, Position rot) {
		Random pRandom = new Random();													  	   // Initialises random object for getting random mob id per iteration
        for (int i = 0; nuMobs > i; i++) {                                                     // Increment to number of wav
            String randomMob = mobs.get(pRandom.nextInt(mobs.size()));                         // Get random mobId from list earlier
            args.clear();                                                                      // Clean the list for next spawn command
            args.add(randomMob);                                                               // Add mobId to spawn command parems
            args.add("1");                                                                     // Number of each mob spawned per randomly selected mob id
            args.add(Integer.toString(lvMobs));                                                // Level of mobs | Change to var for user input
			args.add(String.valueOf(GetRandomPosition(pos,radius).getX()));             // Get random x
			args.add(String.valueOf(targetPlayer.getPosition().getY()));                                       // Get y of current player to prevent floating and sinking enemies
			args.add(String.valueOf(GetRandomPosition(pos,radius).getZ()));             // Get random z
			 
			
			targetPlayer.getPosition().set(GetRandomPosition(pos,radius).getX(),targetPlayer.getPosition().getY(),GetRandomPosition(pos,radius).getZ()); //tp random nearby
			targetPlayer.getRotation().set(GetRandomPosition(rot,360).getX(),GetRandomPosition(rot,360).getY(),GetRandomPosition(rot,360).getZ());       //rotate random 
			targetPlayer.getScene().broadcastPacket(new PacketSceneEntityAppearNotify(targetPlayer));                                                    //slide arnd heh
			
            //spawnMob(sender, targetPlayer, args);
			SpawnCommand sMob = new SpawnCommand();                                           // Call SpawnCommand to make monster
			sMob.execute(sender, targetPlayer, args);  
        } 
    } */
	
	/*public void spawnMob(Player sender, Player targetPlayer, List<String> args) {
        SpawnCommand sMob = new SpawnCommand();                                           // Call SpawnCommand to make monster
        sMob.execute(sender, targetPlayer, args);                                         // Spawn the mob
    }*/
				
				
	/*			
	// 1.3.x
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
    }// spawnMob */

}
