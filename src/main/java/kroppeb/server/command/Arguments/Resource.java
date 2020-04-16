package kroppeb.server.command.Arguments;

import kroppeb.server.command.StringReader;
import kroppeb.server.command.commands.Summon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Resource {
	final public String namespace;
	final public String[] path;
	
	public Resource(String namespace, String[] path) {
		this.namespace = namespace;
		this.path = path;
	}
	
	public static Resource read(StringReader reader){
		String ns = reader.readIdentifier();
		List<String> path = new ArrayList<String>();
		if(reader.tryRead(':')){
			do{
				path.add(reader.readIdentifier());
			}while(reader.tryRead(':'));
		}else{
			path.add(ns);
			ns = null;
			while(reader.tryRead(':'))
				path.add(reader.readIdentifier());
		}
		return new Resource(ns, path.toArray(EmptyStringArray));
	}
	
	@Override
	public String toString() {
		return (namespace==null?"minecraft":namespace) + ':' + String.join("/", path);
	}
	
	final static String[] EmptyStringArray = new String[0];
}
