plugins {
    id ("com.github.johnrengelman.shadow")
}

val shadowGroup = "top.mrxiaom.sweet.checkout.libs"

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")

    val dependencies: List<String> by project.extra
    for (dependency in dependencies) {
        implementation(dependency)
    }
    implementation("top.mrxiaom:Java-WebSocket:1.5.8")
    implementation(project(":plugin:bukkit:common"))
    implementation(project(":plugin:nms"))
    implementation(project(":packets"))
}
tasks {
    jar {
        archiveBaseName.set("SweetCheckout-bukkit-ws")
    }
    shadowJar {
        archiveBaseName.set("SweetCheckout-bukkit-ws")
        archiveClassifier.set("")
        destinationDirectory.set(rootProject.file("out"))
        val shadowRelocations: Map<String, String> by project.extra
        val shadowExcludes: List<String> by project.extra
        shadowRelocations.forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
        shadowExcludes.forEach(this::exclude)
    }
    build {
        dependsOn(shadowJar)
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml")
        }
    }
}
