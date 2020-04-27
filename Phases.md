# Phase 1
* [ ] M: Reimplement the mc commands
* [x] M: Dynamically generate a class to run the commands
* [ ] M: Load the generated class instead of normal functions

# Phase 2
* [ ] Result removal where possible.
* [ ] Execute simplification. 
* [ ] (minor) Execute inlining.
* [ ] (minor) Function direct calls
* [ ] Predicate compiling
* [ ] Faster selectors

# Phase 3
* [ ] Source Use tracking
* [ ] Fast NBT
* [ ] Entity type tracking

# Phase 4
* [ ] Mut/Vis tracking
* [ ] Execute Simplification round 2
* [ ] Multithreading

# Unphased
* Common Helperpack pre compilation
* Common Structures detection
    * Function tree
    * if else (needs M/V tracking) 