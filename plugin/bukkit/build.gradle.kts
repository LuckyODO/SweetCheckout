subprojects {
    val pluginBase = "1.5.1"
    extra["pluginBase"] = pluginBase
    extra["dependencies"] = listOf(
        "top.mrxiaom:qrcode-encoder:1.0.0",
        "net.kyori:adventure-api:4.22.0",
        "net.kyori:adventure-platform-bukkit:4.4.0",
        "net.kyori:adventure-text-minimessage:4.22.0",
        "com.github.technicallycoded:FoliaLib:0.4.4",
        "com.zaxxer:HikariCP:4.0.3",
        "org.slf4j:slf4j-nop:2.0.16",
        "org.jetbrains:annotations:24.0.0",
        "top.mrxiaom:PluginBase:$pluginBase",
    )
    extra["shadowRelocations"] = mapOf(
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
    )
    extra["shadowExcludes"] = listOf(
        "top/mrxiaom/pluginbase/func/AbstractGui*",
        "top/mrxiaom/pluginbase/func/gui/*",
        "top/mrxiaom/pluginbase/func/GuiManager*",
        "top/mrxiaom/pluginbase/gui/*",
        "top/mrxiaom/pluginbase/utils/Bytes*",
    )
}
