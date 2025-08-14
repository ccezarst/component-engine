package EngineCore.DefaultComponents.Extra.UserInterface;

import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import EngineCore.EngineCore;
import EngineCore.DefaultComponents.ComponentType;
import EngineCore.DefaultComponents.CoreComponent;

public abstract class UserInterface extends CoreComponent {

    public UserInterface(String name, Boolean active, EngineCore core, ComponentType... type) {
        super(name, active, core, CoreComponent.mergeComponentTypes(ComponentType.USER_INTERFACE, type));
    }

    public abstract static class ResponseOutputStream extends OutputStream {}

    protected Map.Entry<String, CommandHandler>[] handlers;

    public static class CommandNotFoundException extends RuntimeException {
        public CommandNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
        public CommandNotFoundException(String message) {
            super(message);
        }
    }

    // Blocking operation — waits until command finishes execution
    public void sendCommand(String command, ResponseOutputStream out, String... args) {
    	try {
    		
    	}catch(RuntimeException e) {}
        for (Map.Entry<String, CommandHandler> entry : handlers) {
            if (entry.getKey().equals(command)) {
                entry.getValue().handleCommand(command, out, args);
                return;
            }
        }

        StringBuilder available = new StringBuilder();
        for (Map.Entry<String, CommandHandler> entry : handlers) {
            available.append(entry.getKey()).append(", ");
        }

        throw new RuntimeException("Couldn't find handler for command: " + command +
            ". Available commands: " + available.toString());
    }

    protected CommandHandler getHandlerFromName(String name) {
        for (Map.Entry<String, CommandHandler> entry : handlers) {
            if (entry.getKey().equals(name)) {
                return entry.getValue();
            }
        }
        throw new CommandNotFoundException("Command not found: " + name);
    }

    @Override
    protected void update(EngineCore core) {
        ArrayList<Map.Entry<String, CommandHandler>> registeredHandlers = new ArrayList<>();

        for (CommandHandler handler : this.core.getComponentsOfType(
                ComponentType.COMMAND_HANDLER, CommandHandler.class)) {

            for (Map.Entry<String, CommandHandler> existing : registeredHandlers) {
                for (String allowed : handler.getAllowedCommand()) {
                    if (allowed.equals(existing.getKey())) {
                        throw new RuntimeException("Duplicate command detected: " + allowed);
                    }
                }
            }

            for (String allowed : handler.getAllowedCommand()) {
                registeredHandlers.add(new AbstractMap.SimpleEntry<>(allowed, handler));
            }
        }

        this.handlers = registeredHandlers.toArray(new Map.Entry[0]);
        this.customUpdate(core);
    }

    public abstract void customUpdate(EngineCore core);
}