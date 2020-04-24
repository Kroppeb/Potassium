/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;

public class IntRange {
	final int minValue;
	final int maxValue;
	
	public IntRange(int minValue, int maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	public static IntRange read(Reader reader) throws ReaderException{
		int minValue;
		int maxValue;
		
		if(reader.tryRead("..")){
			minValue = Integer.MIN_VALUE;
			maxValue = reader.readInt();
		}else{
			minValue = reader.readInt();
			if(reader.tryRead("..")){
				if(reader.canRead() && !reader.isWhiteSpace()) {
					maxValue = reader.readInt();
				}else{
					maxValue = Integer.MAX_VALUE;
				}
			}else{
				maxValue = minValue;
			}
		}
		return new IntRange(minValue,maxValue);
	}
}
