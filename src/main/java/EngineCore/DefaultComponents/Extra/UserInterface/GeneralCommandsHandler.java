package EngineCore.DefaultComponents.Extra.UserInterface;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
	    	List<String> caca = InstrumentationInterface.getAllLoadedClassNames();
	    	ArrayList<String> filters = new ArrayList<>();
	    	int i = 0;
	    	while(true) {
	    		if(i >= args.length) {
	    			break;
	    		}
	    		String arg = args[i];
	    		if(arg.equals("-exclude")) {
	    			i += 1;
	    			while(true) {
	    	    		if(i >= args.length) {
	    	    			break;
	    	    		}
	    	    		filters.add(args[i]);
		    			i += 1;
	    			}
	    			
	    		}else if(arg.equals("-defaultFilters")) {
	    			filters.add("com");
	    			filters.add("jdk");
	    			filters.add("org");
	    			filters.add("sun");
	    			filters.add("java");
	    			filters.add("[");
	    		}
	    		i += 1;
	    	}
	    	for(String className: caca) {
	    		boolean pass = true;
	    		for(String filter: filters) {
	    			if(className.startsWith(filter)) {
	    				pass = false;
	    			}
	    		}
	    		if(pass) {
	    			p.println(className);
	    		}
	    	}
	    	p.println("Args: ");
	    	p.println("-exclude <filter> <filter2> ...: excludes any classes with that full names start with <filter> or <filter2> ...");
	    	p.println("-defaultFilters: exludes java,com,jdk,org,sun,[ from the list");
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
