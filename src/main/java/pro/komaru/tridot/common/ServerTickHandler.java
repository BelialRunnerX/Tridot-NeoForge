package pro.komaru.tridot.common;

import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.*;
import pro.komaru.tridot.api.Utils;

public class ServerTickHandler{

    public static int tick;

    // PORT NOTE: the 1.20.1 code subscribed these game events on the MOD bus (where they never fire) and was never
    // called from the entrypoint, so Utils.Schedule.syncTask never ran. They are now registered on the game bus
    // from the Tridot constructor. TickEvent.ServerTickEvent + Phase.END -> ServerTickEvent.Post.
    public static void preInit(IEventBus gameBus){
        gameBus.addListener(EventPriority.NORMAL, false, ServerTickEvent.Post.class, ServerTickHandler::serverTick);
        gameBus.addListener(EventPriority.NORMAL, false, ServerStartingEvent.class, ServerTickHandler::serverStarting);
    }

    private static void serverTick(final ServerTickEvent.Post serverTickEvent){
        tick++;
        Utils.Schedule.handleSyncScheduledTasks(tick);
    }

    private static void serverStarting(final ServerStartingEvent serverStartingEvent){
        Utils.Schedule.serverStartupTasks();
    }
}
