# Potassium

This is an optimization mod that targets vanilla datapacks.

## Basic workings

* A faster multithreaded command parser. Although other steps will most likely slow down reloading
* Better internal Command representation
    * Avoiding partially reparsing of certain arguments on each invocation
* Datapacks are compiled to loop-unrolled bytecode. This will assist the JVM to do way more optimizations.

## Planned features

* Faster selectors
    * Reverse tag lookup
    * Reverse score lookup
* Faster nbt-access by use of `FastNbt`
* Faster predicates
* Precompilation of often used helper datapacks.
* Common datapack structure detection and optimization.
    * Function trees
    * For loops
    * If/else
    * Temp tags
    * Temp variables
* `execute` inlining.
* Reading and mutation tracking.
    * Will allow us to run some commands multithreaded. 

## License

This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
