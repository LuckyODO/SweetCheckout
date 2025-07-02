plugins {
    id ("com.github.johnrengelman.shadow")
}

val shadowGroup = "top.mrxiaom.sweet.checkout.libs"

allprojects {
    dependencies {
        implementation("de.tr7zw:item-nbt-api:2.15.1")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation("top.mrxiaom:qrcode-encoder:1.0.0")
    implementation("net.kyori:adventure-api:4.22.0")
    implementation("net.kyori:adventure-platform-bukkit:4.4.0")
    implementation("net.kyori:adventure-text-minimessage:4.22.0")
    implementation("com.github.technicallycoded:FoliaLib:0.4.4")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.slf4j:slf4j-nop:2.0.16")
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("top.mrxiaom:PluginBase:1.4.9")
    implementation("top.mrxiaom:Java-WebSocket:1.5.8")
    implementation(project(":bukkit:nms"))
    implementation(project(":packets"))
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
