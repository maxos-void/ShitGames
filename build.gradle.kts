plugins {
	id("java-library")
	id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/")
	maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

val paperVersion = "1.21.4-R0.1-SNAPSHOT"
val lampVersion = "4.0.0-rc.17"
val invuiVersion = "3.1.13"
val fastutilVersion = "8.5.16"
val lombokVersion = "1.18.46"

dependencies {
	compileOnly("io.papermc.paper:paper-api:$paperVersion")
	compileOnly("me.clip:placeholderapi:2.11.6")

	compileOnly("dev.triumphteam:triumph-gui:$invuiVersion")
	compileOnly("it.unimi.dsi:fastutil:$fastutilVersion")
	compileOnly("io.github.revxrsal:lamp.common:$lampVersion")
	compileOnly("io.github.revxrsal:lamp.bukkit:$lampVersion")

	compileOnly("org.projectlombok:lombok:$lombokVersion")
	annotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

java {
	toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
	compileJava {
		options.encoding = "UTF-8"
		options.release = 21
	}

	processResources {
		filteringCharset = "UTF-8"
		val props = mapOf(
			"version" to version,
			"lampVersion" to lampVersion,
			"invuiVersion" to invuiVersion,
			"fastutilVersion" to fastutilVersion
		)
		inputs.properties(props)
		filesMatching("plugin.yml") {
			expand(props)
		}
	}

	runServer {
		minecraftVersion("1.21.4")
		jvmArgs("-Xms2G", "-Xmx2G")
	}
}
