/*
 * Copyright (c) 2020 Kroppeb
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package kroppeb.server.command;

import kroppeb.server.command.arguments.Resource;
import kroppeb.server.command.commands.FunctionCommand;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class FunctionFile {
	public final Resource location;
	public final String name;
	public final String commandName;
	public final File file;
	
	public FunctionFile(Resource location, File file) {
		this.location = location;
		this.name = location.path[location.path.length - 1];
		this.commandName = location.namespace + "$$" + String.join("$",location.path);
		this.file = file;
	}
	
	public static Stream<FunctionFile> getNameSpace(Path ns){
		String namespace = ns.getFileName().toString();
		Path functions = ns.resolve("functions");
		if(!Files.exists(functions))
			return Stream.empty();
		return getFolder(namespace, functions, new String[0]);
	}
	
	private static Stream<FunctionFile> getFolder(String namespace, Path folder, String[] strings) {
		try {
			return Files.list(folder).flatMap(path -> {
				File file = path.toFile();
				if(file.isDirectory()) {
					String[] sub = Arrays.copyOf(strings, strings.length + 1);
					sub[strings.length] = file.getName();
					return getFolder(namespace, path, sub);
				}else{
					String name = file.getName();
					if(!name.endsWith(".mcfunction"))
						return Stream.empty();
					String[] sub = Arrays.copyOf(strings, strings.length + 1);
					sub[strings.length] = name.substring(0, name.length() - 11);
					return Stream.of(new FunctionFile(new Resource(namespace, sub), file));
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
