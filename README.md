# MobSpawn Basics

This is a simple plugin to easily generate "waves" of random enemies to fight.
This pulls from a *nearly* complete list of monsters. Too many bosses were spawning so I removed a bunch.


There will probably be issues if you don't use the commands as intended so follow the usage.

Don't blame me if you spawn 100000 mobs and crash your game/server.

## Installation

Get latest release from releases

Place MobWave.jar into your `\grasscutter\plugins` folder and restart the server if it was already running

## Usage

 `/mw start`

   -Will generate one wave of 5 random enemies at level 90.

 `/mw create [# of waves] [# of monsters] [level of monsters]`

   -Create a custom set of waves, each wave will start automatically when the previous one ends.

`/mw stop`

   -Stops any further scheduled waves from happening.

`/mw suffer`

  -Suffer and probably die immediately.

  Thanks @snoobi-seggs for the contribution to making suffer happen.

## Version

This uses the Grasscutters plugin template for 1.3.1.

## Issues

-Sometimes a friendly monster will spawn and support you, you are unable to damage said monster.
Use `/killall` to complete the wave.

-Timer does not work (visual only).

-Ending the wave does not remove already spawned monsters.

-No graphic on completion.

-Additional spawn modifiers added in 1.3.1 'extended spawn command' update are not applicable.
