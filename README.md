# MobWave Basics

This is a simple plugin to easily generate "waves" of random enemies to fight.
This pulls from a *nearly* complete list of monsters. 
All previously removed bosses were re-added with the implementation of boss waves.

There will probably be issues if you don't use the commands as intended so follow the usage.

Don't blame me if you spawn 100000 mobs and crash your game/server.

If time runs out all monsters from the wave will despawn and any remaining waves will be failed.

## Installation

Get latest release from [releases](https://github.com/NotThorny/MobWave/releases)

Place MobWave.jar into your `\grasscutter\plugins` folder and restart the server if it was already running

## Usage

 `/mw start`
  
   - Start unlimited waves of 5 monsters at level 90 with 5 minutes per wave.
   - Adding wave type will consist of **only** the selected mob type!
   - Every 5th wave is a boss wave.
   - `/mw start [common|elite|boss]` will start waves of **only the selected mob type**
   
 `/mw create w[waves] x[mobs per wave] lv[level]`

   - Create a custom set of waves, each wave will start automatically when the previous one ends.

   - Additional optional settings: t[type] s[wave time in seconds] hp[hp] atk[atk] def[def]
   - **Order does not matter, so feel free to use as many or as few of the modifiers you want and in any order**

   Types for custom waves are 1 (commmon mobs), 2 (elite mobs), and 3 (boss mobs). 

   **For example: `/mw create w5 x3 lv50 t2` will create 5 waves of 3 mobs each, at level 50, all elite mobs**

`/mw skip`
- Skips the current wave

`/mw stop`
- Stops any further scheduled waves from happening and removes any already spawned monsters.

`/mw suffer`
- Suffer and probably die immediately.

  Thanks [@snoobi-seggs](https://github.com/snoobi-seggs) for making suffer happen.

## Version

This uses the Grasscutters plugin template for 1.3.2.

### Issues

#### Any suggestions or issues are welcomed in [issues](https://github.com/NotThorny/MobWave/issues)

#### If you want mob chances changed, please make an issue with the mob(s) name or id and whether it should be more or less frequent

-Timer works but visual on-screen timer does not update.

-No graphic on success/fail.
