# MobSpawn Basics

This is a simple plugin to easily generate "waves" of random enemies to fight.
This pulls from a *nearly* complete list of monsters. Too many bosses were spawning so I removed a bunch.

There will probably be issues if you don't use the commands as intended so follow the usage.

Don't blame me if you spawn 100000 mobs and crash your game/server.

If time runs out all monsters from the wave will despawn and any remaining waves will be failed.

## Installation

Get latest release from releases

Place MobWave.jar into your `\grasscutter\plugins` folder and restart the server if it was already running

## Usage

 `/mw start`

   -Will generate one wave of 5 random enemies at level 90.

 `/mw create [# of waves] [# of monsters] [level of monsters] [OPTIONAL: wave time in seconds]`

   -Create a custom set of waves, each wave will start automatically when the previous one ends.

`/mw stop`

   -Stops any further scheduled waves from happening and removes any already spawned monsters.

`/mw suffer`

  -Suffer and probably die immediately.

  Thanks @snoobi-seggs for the contribution to making suffer happen.

## Version

This uses the Grasscutters plugin template for 1.3.1.

## Issues

-Timer works but visual on-screen timer does not update.

-No graphic on success/fail.

-Additional spawn modifiers added in 1.3.1 'extended spawn command' update are not applicable.
