rootProject.name = "SweetCheckout"

include(":backend")
include(":backend:common")
include(":backend:cli")
include(":bukkit")
include(":bukkit:nms")
for (file in File("bukkit/nms").listFiles() ?: arrayOf()) {
    if (file.isDirectory && File(file, "build.gradle.kts").exists()) {
        include(":bukkit:nms:${file.name}")
    }
}
include(":packets")
