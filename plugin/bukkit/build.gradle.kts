subprojects {
    val pluginBase = "1.6.3"
    extra["pluginBase"] = pluginBase
    extra["dependencies"] = mapOf(
        "com.github.technicallycoded:FoliaLib:0.4.4" to true,
        "top.mrxiaom.pluginbase:library:$pluginBase" to false,
        "top.mrxiaom.pluginbase:paper:$pluginBase" to false,
        "top.mrxiaom:LibrariesResolver:$pluginBase:all" to false,
    )
    extra["libraries"] = listOf(
        "top.mrxiaom:qrcode-encoder:1.0.0",
        "net.kyori:adventure-api:4.22.0",
        "net.kyori:adventure-platform-bukkit:4.4.0",
        "net.kyori:adventure-text-minimessage:4.22.0",
        "com.zaxxer:HikariCP:4.0.3",
        "top.mrxiaom:EvalEx-j8:3.4.0",
    )
    extra["shadowRelocations"] = mapOf(
        "top.mrxiaom.pluginbase" to "base",
        "de.tr7zw.changeme.nbtapi" to "nbtapi",
        "org.java_websocket" to "websocket",
        "com.tcoded.folialib" to "folialib",
    )
    extra["shadowExcludes"] = listOf(
        "top/mrxiaom/pluginbase/func/AbstractGui*",
        "top/mrxiaom/pluginbase/func/gui/*",
        "top/mrxiaom/pluginbase/func/GuiManager*",
        "top/mrxiaom/pluginbase/gui/*",
        "top/mrxiaom/pluginbase/utils/Bytes*",
    )
    dependencies {
        add("compileOnly", "org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
        add("compileOnly", "me.clip:placeholderapi:2.11.6")
    }
}
