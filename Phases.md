# Phase 1
* [ ] M: Reimplement the mc commands
    * [x] advancement
    * [x] attribute
    * [x] bossbar
    * [x] clear
    * [ ] clone
    * [x] data
    * [ ] datapack
    * [ ] defaultgamemode
    * [ ] difficulty
    * [ ] effect
    * [ ] enchant
    * [x] execute
    * [ ] experience
    * [ ] fill
    * [ ] forceload
    * [x] function
    * [ ] gamemode
    * [ ] gamerule
    * [ ] give
    * [ ] help
    * [x] kill
    * [ ] list
    * [ ] locate
    * [x] locatebiome
    * [ ] loot
    * [ ] me
    * [ ] msg
    * [ ] particle
    * [x] playsound
    * [ ] recipe
    * [ ] reload
    * [x] replaceitem
    * [ ] say
    * [ ] schedule
    * [x] scoreboard
    * [ ] seed
    * [x] setblock
    * [ ] setworldspawn
    * [ ] spawnpoint
    * [ ] spectate
    * [ ] spreadplayers
    * [ ] stopsound
    * [x] summon
    * [x] tag
    * [ ] team
    * [ ] teammsg
    * [ ] teleport
    * [ ] tell
    * [ ] tellraw
    * [ ] time
    * [ ] title
    * [ ] tp
    * [ ] trigger
    * [ ] w
    * [ ] weather
    * [ ] worldborder
    * [ ] xp
* [x] M: Dynamically generate a class to run the commands
* [ ] M: Load the generated class instead of normal functions

# Phase 2
* [ ] Result removal where possible / Direct callback results (TBD)
* [ ] Early Source decomposition tracking

# Phase 3
* [ ] Mut/Vis tracking
* [ ] Execute flattening
* [ ] Faster selectors

# Phase 4
* [ ] Predicate compiling
* [ ] Loottable compiling
* [ ] Entity type tracking
* [ ] Fast NBT

# Phase 5
* [ ] Multithreading

# Unphased
* Common Helperpack pre compilation
* Common Structures detection
    * Function tree
    * Loops
