rootProject.name = "SweetCheckout"

include(":backend")
include(":bukkit")
include(":bukkit:nms")
for (file in File("bukkit/nms").listFiles() ?: arrayOf()) {
    if (file.isDirectory) {
        include(":bukkit:nms:${file.name}")
    }
}
