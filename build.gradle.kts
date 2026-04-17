plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
    id("maven-publish")
}

val pluginVersion = "1.1.0"
val minecraftVersion = "26.1.2"
val protocolVersion = "26.1-SNAPSHOT"
val adventureVer = "4.25.0"
val commandApiVer = "11.2.0"
val serverLocation = "Skript/26-1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")

    // McProtocol
    maven("https://repo.opencollab.dev/main/")

    // Paper
    maven("https://repo.papermc.io/repository/maven-public/")

    // Command API
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    // McProtocol
    implementation("org.geysermc.mcprotocollib:protocol:$protocolVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVer")

    // Paper
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion.build.+")

    // Command Api
    implementation("dev.jorel:commandapi-paper-shade:$commandApiVer")

    // Json
    implementation("com.googlecode.json-simple:json-simple:1.1")

    // bStats
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

tasks {
    register("server", Copy::class) {
        dependsOn("shadowJar")
        from("build/libs") {
            include("StressTestBots-*.jar")
            destinationDir = file("/Users/ShaneBee/Desktop/Server/Minecraft/$serverLocation/plugins/")
        }

    }
    processResources {
        expand("version" to "$pluginVersion-$minecraftVersion",
            "minecraftVersion" to minecraftVersion)
    }
    compileJava {
        options.release = 25
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }
    javadoc {
        title = "StressTestBots $pluginVersion API"
        options.encoding = Charsets.UTF_8.name()
        exclude("com/shanebeestudios/core/plugin")
        (options as StandardJavadocDocletOptions).links(
            "https://jd.papermc.io/paper/1.21.1/",
            "https://jd.advntr.dev/api/4.17.0/",
            "https://repo.opencollab.dev/javadoc/maven-snapshots/org/geysermc/mcprotocollib/protocol/$protocolVersion/raw"
        )
    }
    shadowJar {
        relocate("dev.jorel.commandapi", "com.shanebeestudios.stress.api.commandapi")
        relocate("org.bstats", "com.shanebeestudios.stress.api.bstats")
        archiveFileName = "${project.name}-${pluginVersion}-${minecraftVersion}.jar"
        minimize {
            exclude(dependency("org.cloudburstmc.math:.*:.*"))
        }
    }
    jar {
        dependsOn(shadowJar)
    }
}
