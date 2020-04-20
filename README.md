# Potassium

This is an optimization mod that targets vanilla datapacks.

## Basic workings

* A faster command parser. Although other steps will most likely slow down reloading
* Better internal Command representation
    * Arguments are no longer partially reparsed on each invocation
* Datapacks are compiled to loop-unrolled bytecode. This will assist the JVM to do way more optimizations.

## Planned features

* Faster nbt-access by use of `Components`
* Precompilation of often used helper datapacks.
* Common datapack structure detection and optimization.
* `execute` inlining.

## License

This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
