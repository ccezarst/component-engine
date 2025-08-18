package EngineCore.DefaultComponents.Extra.UserInterface;

import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import EngineCore.EngineCore;
import EngineCore.TestingEnviromentCore;
import EngineCore.DefaultComponents.ComponentType;
import EngineCore.DefaultComponents.CoreComponent;



public class ConsoleInterface extends UserInterface{

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	public ConsoleInterface(Boolean active, EngineCore core) {
		super("ConsoleInterface", active, core, ComponentType.CONSOLE_INTERFACE);
		// TODO Auto-generated constructor stub
	}
	

	public static class ConsoleResponseStream extends ResponseOutputStream{

		@Override
		public void write(int b) throws IOException {
			System.out.print((char) b);
		}
		
	}
	Scanner s;
	final String PROMPT = ANSI_GREEN + "------------> " + ANSI_RESET;
	@Override
	protected void step(EngineCore core) {
		System.out.print(PROMPT);
		System.out.flush();

		String line = s.nextLine(); // it is a blocking operation  but each comp runs on a seperate thread
		String[] splitLine = line.split(" ");
		String com = splitLine[0];
		ArrayList<String> args = new ArrayList<>();
		for (int i = 1; i < splitLine.length; i++) {
		    String stripped = splitLine[i].strip();
		    if (stripped != "") { // keep same logic
		        args.add(stripped);
		    }
		}
		try {
		    this.sendCommand(com, new ConsoleResponseStream(), args.toArray(new String[0]));
		} catch (CommandNotFoundException e) {
		    System.out.println("Couldn't find command " + com);
		    System.out.println("Available commands: ");
		    for (Map.Entry<String, CommandHandler> entry : handlers) {
		        System.out.println(entry.getKey());
		    }
		}
	}

	
	@Override
	protected int test(TestingEnviromentCore core) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void customUpdate(EngineCore core) {
		// TODO Auto-generated method stub
		s = new Scanner(System.in);
	}
}
