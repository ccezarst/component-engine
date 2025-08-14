package EngineCore.DefaultComponents.Extra.UserInterface;

import java.io.PrintWriter;
import java.util.Map;

import EngineCore.EngineCore;
import EngineCore.InstrumentationInterface;
import EngineCore.TestingEnviromentCore;
import EngineCore.DefaultComponents.ComponentType;
import EngineCore.DefaultComponents.CoreComponent;

public class GeneralCommandsHandler extends UserInterface implements CommandHandler{
	// extends user interface so i can access the commands
	public GeneralCommandsHandler(Boolean active, EngineCore core) {
		super("GeneralCommandsHandler", active, core, ComponentType.COMMAND_HANDLER);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String[] getAllowedCommand() {
		return new String[] {"help", "commands", "/?", "seeLoadedClasses"};
	}

	public void handleCommand(String command, ResponseOutputStream out, String... args) {
		PrintWriter p = new PrintWriter(out);
	    if (command.equals("help") || command.equals("commands") || command.equals("/?")) {
	        p.println("Available commands:");
	        for (Map.Entry<String, CommandHandler> entry : this.handlers) {
	            p.println("- " + entry.getKey());
	        }
	        // Avoid closing if `out` is shared elsewhere
	    }
	    if(command.equals("seeLoadedClasses")) {
	    	p.println(InstrumentationInterface.getAllLoadedClassNames().toString());
	    }
	    p.flush();
	}
	
	@Override
	protected void step(EngineCore core) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int test(TestingEnviromentCore core) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void customUpdate(EngineCore core) {
		// TODO Auto-generated method stub
		
	}
}
