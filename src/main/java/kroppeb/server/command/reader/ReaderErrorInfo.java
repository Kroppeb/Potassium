/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.reader;

public class ReaderErrorInfo {
	final String source;
	final int line;
	final int pos;
	final String text;
	
	public ReaderErrorInfo(String source, int line, int pos, String text) {
		this.source = source;
		this.line = line;
		this.pos = pos;
		this.text = text;
	}
}
