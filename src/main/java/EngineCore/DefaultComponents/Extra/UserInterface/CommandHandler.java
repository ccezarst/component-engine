package EngineCore.DefaultComponents.Extra.UserInterface;

import java.io.IOException;
import java.io.OutputStream;

import EngineCore.DefaultComponents.Extra.UserInterface.UserInterface.ResponseOutputStream;

public interface CommandHandler {
	public String[] getAllowedCommand();
	public void handleCommand(String command, ResponseOutputStream out, String... args);
}
