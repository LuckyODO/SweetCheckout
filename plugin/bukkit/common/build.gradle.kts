plugins {
    id ("com.github.johnrengelman.shadow")
}

val shadowGroup = "top.mrxiaom.sweet.checkout.libs"

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")

    val dependencies: List<String> by project.extra
    for (dependency in dependencies) {
        compileOnly(dependency)
    }
    compileOnly(project(":plugin:nms"))
    compileOnly(project(":packets"))
}
tasks {
    jar {
        archiveBaseName.set("SweetCheckout-bukkit")
    }
    shadowJar {
        archiveBaseName.set("SweetCheckout-bukkit")
        archiveClassifier.set("")
        destinationDirectory.set(rootProject.file("out"))
        mapOf(
            "org.intellij.lang.annotations" to "annotations.intellij",
            "org.jetbrains.annotations" to "annotations.jetbrains",
	        "top.mrxiaom.pluginbase" to "base",
            "com.zaxxer.hikari" to "hikari",
            "org.slf4j" to "slf4j",
            "de.tr7zw.changeme.nbtapi" to "nbtapi",
            "net.kyori" to "kyori",
            "top.mrxiaom.qrcode" to "qrcode",
            "org.java_websocket" to "websocket",
            "com.tcoded.folialib" to "folialib",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
        listOf(
            "top/mrxiaom/pluginbase/func/AbstractGui*",
            "top/mrxiaom/pluginbase/func/gui/*",
            "top/mrxiaom/pluginbase/func/GuiManager*",
            "top/mrxiaom/pluginbase/gui/*",
            "top/mrxiaom/pluginbase/utils/Bytes*",
        ).forEach(this::exclude)
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
