plugins {
    java
}

group = "io.github.petabytebrain"
version = "1.0.0"

val minecraftVersion = "26.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    // Server API (Minecraft 26.2)
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion.build.+")

    // Pl3xMap API – https://modrinth.com/plugin/pl3xmap
    compileOnly("maven.modrinth:pl3xmap:26.2-555")

    // DeadChest – https://modrinth.com/plugin/dead-chest
    compileOnly("maven.modrinth:dead-chest:4.30.0")
}

java {
    // Minecraft 26.x requires Java 25
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        val props = mapOf("version" to project.version, "apiVersion" to minecraftVersion)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveFileName.set("DeadChestPl3xMap-${project.version}.jar")
    }
}
