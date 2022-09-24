package thorny.grasscutters.MobWave;

import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.props.EntityType;
import emu.grasscutter.server.event.EventHandler;
import emu.grasscutter.server.event.HandlerPriority;
import emu.grasscutter.server.event.entity.EntityDeathEvent;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.server.event.game.ReceivePacketEvent;
// import emu.grasscutter.server.event.game.ServerTickEvent;
import thorny.grasscutters.MobWave.commands.MobWaveCommand;

/**
 * A class containing all event handlers.
 * Syntax in event handler methods are similar to CraftBukkit.
 * To register an event handler, create a new instance of {@link EventHandler}.
 * Pass through the event class you want to handle. (ex. `new EventHandler<>(PlayerJoinEvent.class);`)
 * You can change the point at which the handler method is invoked with {@link EventHandler#priority(HandlerPriority)}.
 * You can set whether the handler method should be invoked when another plugin cancels the event with {@link EventHandler#ignore(boolean)}.
 */
public final class EventListener {
    public static void EntityDeathEvent(EntityDeathEvent event) {
        if (MobWaveCommand.getMobScene() == event.getEntity().getGroupId())
            try {
                //MobWaveCommand mwc = new MobWaveCommand();
                GameEntity monster = event.getEntity();
                var entType = event.getEntity().getEntityType();
                if (EntityType.Monster.getValue() == entType){
                    MobWaveCommand.mobWaveChallenge.onMonsterDeath((EntityMonster) monster);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public static void onPacket(ReceivePacketEvent event) {
            if (event.getPacketId() == PacketOpcodes.DungeonChallengeFinishNotify){
                // Do nothing for now
            }
        }
        // Temp implementation of onTimeTrigger
        // public static void onTick(ServerTickEvent event) {
        // try {
        // if(MobWaveCommand.mobWaveChallenge != null){
        //     // Timer trigger
        //     MobWaveCommand.mobWaveChallenge.onCheckTimeOut();
        // }
        // } catch (Exception e) {
        //   }
        // }
}
