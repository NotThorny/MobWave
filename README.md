# MobSpawn Basics

This is a simple plugin to easily generate "waves" of random enemies to fight.
This pulls from a *nearly* complete list of monsters. Too many bosses were spawning so I removed a bunch.

There will probably be issues if you don't use the commands as intended so follow the usage.

Don't blame me if you spawn 100000 mobs and crash your game/server.

If time runs out all monsters from the wave will despawn and any remaining waves will be failed.

## Installation

Get latest release from [releases](https://github.com/NotThorny/MobWave/releases)

Place MobWave.jar into your `\grasscutter\plugins` folder and restart the server if it was already running

## Usage

 `/mw start` OR `/mw start [wave type]`

   -Start unlimited waves of 5 monsters at level 90 with 5 minutes per wave.
   -First wave is 4 common and 1 elite mob
   -Every 5th wave is 2 common, 2 elite, 1 boss mob
   -Every other wave is 3 common and 2 elite mobs

   -[wave type] waves consist of only the selected mob type (common, elite, or boss)

 `/mw create [# of waves] [# monsters per wave] [level of monsters] [wave type] [OPTIONAL: wave time in seconds]`

   -Create a custom set of waves, each wave will start automatically when the previous one ends.
   -Wave types: common, elite, and boss.
   -If you want a custom time but not a custom wave type, put `none` for wave type and then add your time.

`/mw skip`

  -Skips the current wave

`/mw stop`

   -Stops any further scheduled waves from happening and removes any already spawned monsters.

`/mw suffer`

  -Suffer and probably die immediately.

  Thanks [@snoobi-seggs](https://github.com/snoobi-seggs) for making suffer happen.

## Version

This uses the Grasscutters plugin template for 1.3.2.

### Planned

Soon will change `create` to follow the spawn command usage of using modifiers (x, lv) so that the command won't require strict user input order

### Issues

#### Any suggestions or issues are welcomed in [issues](https://github.com/NotThorny/MobWave/issues)

#### If you want mob chances changed, please make an issue with the mob(s) name or id and whether it should be more or less frequent

-Timer works but visual on-screen timer does not update.

-No graphic on success/fail.

-Additional spawn modifiers added in 1.3.1 'extended spawn command' update are not applicable.
