# MobSpawn Basics

This is a simple plugin to easily generate "waves" of random enemies to fight.
This pulls from a *nearly* complete list of monsters. Too many bosses were spawning so I removed a bunch.
Azdha was removed for appearing in every 4 of 5 runs.

There will probably be issues if you don't use the commands as intended so follow the usage.

Don't blame me if you spawn 100000 mobs and crash your game/server.

## Installation

Get latest release from releases

Place MobWave.jar into your `\grasscutter\plugins` folder and restart the server if it was already running

## Usage

 `/mw start`

   -Will generate one wave of 5 random enemies at level 90.

 `/mw create [# of waves] [# of monsters] [level of monsters] [OPTIONAL: time per wave in seconds]`

   -Create a custom set of waves, each wave will last for 1 minute so be quick!

`/mw stop`

   -Stops any further scheduled waves from happening

## Version

This uses the Grasscutters plugin template for 1.3.1 (updated for new spawn command usage).

## Issues

Sometimes a friendly monster will spawn and support you, you are unable to damage said monster.

Stopping a custom wave then starting a new one before the timer for the current one has run out will pick up from where the last wave left off.

This plugin will only work on GC 1.3.1+ unless spawn command gets changed again.

Additional spawn modifiers added in 1.3.1 'extended spawn command' update are not applicable yet
