/*package kroppeb.server.command.commands;

import kroppeb.server.command.Command;
import kroppeb.server.command.arguments.Selector;
import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.server.command.AdvancementCommand;
import net.minecraft.server.command.ServerCommandSource;

public interface Advancement extends Command {
	public static Advancement read(Reader reader) throws ReaderException {
		AdvancementCommand;
		boolean grant;
		String word = reader.readLiteral();
		switch (word){
			case "grant":
				 grant = true; break;
			case "revoke":
				grant = false; break;
			default:
				throw new ReaderException("expected (grant/revoke), got " + word);
		}
		Selector targets = Selector.read(reader);
		reader.moveNext();
		word = reader.readLiteral();
		switch (word){
			case "everything":
			
			case "revoke":
				grant = false; break;
			default:
				throw new ReaderException("expected (grant/revoke), got " + word);
		}
		
	}
	
}
*/