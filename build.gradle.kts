plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
    id("maven-publish")
}

val pluginVersion = "1.1.0"
val minecraftVersion = "1.21.5"
val protocolVersion = "1.21.5-SNAPSHOT"
val adventureVer = "4.17.0"
val commandApiVer = "10.0.0"

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
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion-R0.1-SNAPSHOT")

    // Command Api
    implementation("dev.jorel:commandapi-bukkit-shade:$commandApiVer")
}

tasks {
    register("server", Copy::class) {
        dependsOn("shadowJar")
        from("build/libs") {
            include("StressTestBots-*.jar")
            destinationDir = file("/Users/ShaneBee/Desktop/Server/Skript/1-21-5/plugins/")
        }

    }
    processResources {
        expand("version" to pluginVersion)
    }
    compileJava {
        options.release = 21
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }
    javadoc {
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
        archiveFileName = "${project.name}-${pluginVersion}-${minecraftVersion}.jar"
        minimize {
            exclude(dependency("org.cloudburstmc.math:.*:.*"))
        }
    }
    jar {
        dependsOn(shadowJar)
    }
}
