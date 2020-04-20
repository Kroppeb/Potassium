package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Resource {
	final public String namespace;
	final public String[] path;
	
	public Resource(String namespace, String[] path) {
		this.namespace = namespace;
		this.path = path;
	}
	
	public static Resource read(Reader reader) throws ReaderException {
		Identifier ns = reader.readIdentifier();
		
		return new Resource(ns.getNamespace(), ns.getPath().split("/"));
	}
	
	@Override
	public String toString() {
		return (namespace==null?"minecraft":namespace) + ':' + String.join("/", path);
	}
	
	final static String[] EmptyStringArray = new String[0];
}
