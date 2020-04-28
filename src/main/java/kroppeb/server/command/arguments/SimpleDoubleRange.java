/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;

public class SimpleDoubleRange {
	final double minValue;
	final double maxValue;
	
	public SimpleDoubleRange(double minValue, double maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	public static SimpleDoubleRange read(Reader reader) throws ReaderException{
		double minValue;
		double maxValue;
		
		if(reader.tryRead("..")){
			minValue = Double.NEGATIVE_INFINITY;
			maxValue = reader.readDouble();
		}else{
			minValue = reader.readDouble();
			if(reader.tryRead("..")){
				if(reader.canRead() && !reader.isWhiteSpace()) {
					maxValue = reader.readDouble();
				}else{
					maxValue = Double.POSITIVE_INFINITY;
				}
			}else{
				maxValue = minValue;
			}
		}
		return new SimpleDoubleRange(minValue,maxValue);
	}
}
