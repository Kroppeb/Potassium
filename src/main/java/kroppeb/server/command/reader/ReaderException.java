/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command.reader;


public class ReaderException extends Exception{
	
	public ReaderException(String message) {
		super(message);
	}
	
	public ReaderException(String message, Throwable cause) {
		super(message, cause);
	}
}
