plugins {
    id("com.gradleup.shadow")
}

java.withJavadocJar()
val shadowGroup = "top.mrxiaom.sweet.checkout.libs"
val shadowLink = configurations.create("shadowLink")
dependencies {
    val dependencies: Map<String, Boolean> by project.extra
    for ((dependency, ignore) in dependencies) {
        if (ignore) {
            implementation(dependency) { isTransitive = false }
        } else {
            implementation(dependency)
        }
    }
    val libraries: List<String> by project.extra
    for (library in libraries) {
        compileOnly(library)
    }
    implementation("top.mrxiaom:Java-WebSocket:1.5.8") { isTransitive = false }
    implementation(project(":plugin:bukkit:shared"))
    for (dependency in project.project(":plugin:nms").allprojects) {
        add("shadowLink", dependency)
    }
    implementation(project(":packets"))
}
tasks {
    shadowJar {
        configurations.add(shadowLink)
        val shadowRelocations: Map<String, String> by project.extra
        val shadowExcludes: List<String> by project.extra
        shadowRelocations.forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
        shadowExcludes.forEach(this::exclude)
    }
    val copyTask = create<Copy>("copyBuildArtifact") {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs)
        rename { "SweetCheckout-bukkit-ws-$version.jar" }
        into(rootProject.file("out"))
    }
    build {
        dependsOn(copyTask)
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml")
        }
    }
}
