/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;

public class ScoreHolder {
	public static ScoreHolder read(Reader reader) throws ReaderException {
		if(reader.peek() == '@'){
			Selector selector = Selector.read(reader);
			if(selector instanceof Selector.SingleSelector){
				return new Entity((Selector.SingleSelector) selector);
			}else{
				return new Entities(selector);
			}
		}else{
			return new Named(reader.readUntilWhitespace());
		}
	}
	
	abstract public static class SingleScoreHolder extends ScoreHolder{
	
	}
	
	public static class Named extends SingleScoreHolder{
		final String name;
		
		public Named(String name) {
			this.name = name;
		}
	}
	
	public static class Entity extends SingleScoreHolder{
		final Selector.SingleSelector selector;
		
		public Entity(Selector.SingleSelector selector) {
			this.selector = selector;
		}
	}
	
	public static class Entities extends ScoreHolder{
		final Selector selector;
		
		public Entities(Selector selector) {
			this.selector = selector;
		}
	}
	
	static All ALL = new All();
	public static class All extends ScoreHolder{}
}
