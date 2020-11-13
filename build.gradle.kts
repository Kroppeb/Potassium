import net.fabricmc.loom.task.RemapSourcesJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val minecraftVersion: String by project
val yarnMappings: String by project
val loaderVersion: String by project

val modVersion: String by project
val mavenGroup: String by project
val archivesBaseName: String by project

val fabricVersion:String by project
val asmVersion:String by project
// val loomVersion:String by project


plugins {
	java
	kotlin("jvm") version "1.4.10"
	id("fabric-loom") version "0.5-SNAPSHOT"
}

base {
	archivesBaseName = "$archivesBaseName-mc$minecraftVersion"
}
group = mavenGroup
version = modVersion


java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}


minecraft {
	accessWidener = file("src/main/resources/potassium.accesswidener")
	refmapName = "mixins.potassium.refmap.json"
}

repositories {
	mavenCentral()
	jcenter()
	maven(url = "http://maven.fabricmc.net")
	maven ( url = "https://jitpack.io" )
	maven ( url = "https://repository.ow2.org/nexus" )
}

dependencies {
	//to change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings("net.fabricmc:yarn:$yarnMappings:v2")
	modCompile("net.fabricmc:fabric-loader:$loaderVersion")

	implementation("org.ow2.asm:asm:$asmVersion")
	implementation("org.ow2.asm:asm-util:$asmVersion")
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.1")
}

tasks.withType<ProcessResources> {
	inputs.property("version", project.version)
/*
	from(sourceSets.main.resources.srcDirs) {
		include ("fabric.mod.json")
		expand("version" to version)
	}

	from(sourceSets.main.resources.srcDirs) {
		exclude( "fabric.mod.json")
	}*/
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
val javaCompile = tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
// if it is present.
// If you remove this task, sources will not be generated.
val sourcesJar by tasks.registering(Jar::class) {
	classifier = "sources"
	//from (sourceSets.main.allSource)
	from ("LICENSE")
}

val jar = tasks.getByName<Jar>("jar") {
	from ( "LICENSE")
}

val remapJar = tasks.getByName<RemapSourcesJarTask>("remapSourcesJar")
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
	jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
	jvmTarget = "1.8"
}


tasks.withType(KotlinCompile::class)
	.forEach {
		it.kotlinOptions { freeCompilerArgs = listOf("-Xnew-inference") }
	}
