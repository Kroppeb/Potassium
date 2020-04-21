/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.arguments;

import kroppeb.server.command.reader.Reader;
import kroppeb.server.command.reader.ReaderException;

public enum IntComparator {
	LT{
		@Override
		boolean compare(int a, int b) {
			return a < b;
		}
	}, LTE {
		@Override
		boolean compare(int a, int b) {
			return a <= b;
		}
	}, EQ {
		@Override
		boolean compare(int a, int b) {
			return a == b;
		}
	}, GTE {
		@Override
		boolean compare(int a, int b) {
			return a >= b;
		}
	}, GT {
		@Override
		boolean compare(int a, int b) {
			return a > b;
		}
	};
	
	public static IntComparator read(Reader reader) throws ReaderException {
		String type = reader.readUntilWhitespace();
		switch (type){
			case "<":return LT;
			case "<=":return LTE;
			case "=":return EQ;
			case ">=":return GTE;
			case ">":return GT;
			default:throw new ReaderException("unknown comparison: " + type);
		}
	}
	
	abstract boolean compare(int a, int b);
}
