/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;


public class CommandData {
	public final Command cmd;
	public final String name;
	
	public CommandData(Command cmd, String name) {
		this.cmd = cmd;
		this.name = "command" + "$" + name;
	}
}
