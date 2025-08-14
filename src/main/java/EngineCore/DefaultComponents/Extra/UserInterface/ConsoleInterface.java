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
	@Override
	protected void step(EngineCore core) {
		String line = s.nextLine(); // it is a blocking operation  but each comp runs on a seperate thread
		String[] splitLine = line.split (" ");
		String com = splitLine[0];
		ArrayList<String> args = new ArrayList<>();
		for(int i = 1; i < splitLine.length; i++) {
			String stripped = splitLine[i].strip();
			if(stripped != "") {
				args.add(stripped);
			}
		}
		try {
			this.sendCommand(com, new ConsoleResponseStream(), args.toArray(new String[0]));
		}catch(CommandNotFoundException e) {
			System.out.println("Couldn't find command " + com);
			System.out.println("Available commands: ");
			for(Map.Entry<String, CommandHandler> entry: handlers) {
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
