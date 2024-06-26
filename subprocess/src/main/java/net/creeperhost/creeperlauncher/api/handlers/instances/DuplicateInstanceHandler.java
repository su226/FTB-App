package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class DuplicateInstanceHandler implements IMessageHandler<DuplicateInstanceHandler.Request> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(Request data) {
        Instance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Unable to locate instance", null));
            return;
        }

        try {
            Instance newInstance = instance.duplicate(data.newName, data.category);
            if (newInstance == null) {
                WebSocketHandler.sendMessage(new Reply(data, false, "Failed to duplicate instance...", null));
                return;
            }

            Instances.refreshInstances();
            WebSocketHandler.sendMessage(new Reply(data, true, "Duplicated instance!", new InstalledInstancesHandler.SugaredInstanceJson(newInstance)));
        } catch (IOException e) {
            LOGGER.error("Unable to duplicate instance because of", e);
            WebSocketHandler.sendMessage(new Reply(data, false, "Failed to duplicate instance...", null));
        }
    }
    
    public static class Request extends BaseData {
        public String uuid;
        public String newName;
        @Nullable
        public String category;
        
    }
    
    private static class Reply extends Request {
        public String message;
        public boolean success;
        public @Nullable InstalledInstancesHandler.SugaredInstanceJson instance;

        public Reply(Request data, boolean success, String message, @Nullable InstalledInstancesHandler.SugaredInstanceJson instance) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";
            this.uuid = instance == null ? "-1" : instance.uuid.toString();
            this.message = message;
            this.success = success;
            this.instance = instance;
        }
    } 
}

