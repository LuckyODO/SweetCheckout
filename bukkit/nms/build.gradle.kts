
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    implementation("de.tr7zw:item-nbt-api:2.13.2")
    for (proj in subprojects) {
        implementation(proj)
        if (proj.name != "shared") {
            proj.dependencies.implementation(project(":bukkit:nms:shared"))
        }
    }
}
