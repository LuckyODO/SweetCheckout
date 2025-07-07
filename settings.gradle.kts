rootProject.name = "SweetCheckout"

include(":backend")
include(":backend:common")
include(":backend:cli")
include(":plugin")
include(":plugin:bukkit")
include(":plugin:bukkit:shared")
include(":plugin:bukkit:with-backend")
include(":plugin:bukkit:ws")
include(":plugin:nms")
for (file in File("plugin/nms").listFiles() ?: arrayOf()) {
    if (file.isDirectory && File(file, "build.gradle.kts").exists()) {
        include(":plugin:nms:${file.name}")
    }
}
include(":packets")
