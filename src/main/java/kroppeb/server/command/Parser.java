package kroppeb.server.command;


import kroppeb.server.command.commands.Summon;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Parser {
	static Object readFunction(StringReader reader){
		String command = reader.readWord();
		switch (command){
			case "summon":
				return Summon.read(reader);
			default:
				throw new NotImplementedException();
		}
	}
	
}
