import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    id ("com.gradleup.shadow")
}

java.withJavadocJar()
val shadowGroup = "top.mrxiaom.sweet.checkout.libs"
val shadowLink = configurations.create("shadowLink")
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.11.6")

    val dependencies: List<String> by project.extra
    for (dependency in dependencies) {
        implementation(dependency)
    }
    val backendDependencies: List<String> by project.extra
    for (dependency in backendDependencies) {
        implementation(dependency)
    }
    add("shadowLink", project("java9"))
    implementation(project(":backend:common"))
    implementation(project(":plugin:bukkit:shared"))
    implementation(project(":plugin:nms"))
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
        minimize {
            val dependencies = listOf(
                "org.bouncycastle:bcprov-jdk15on",
                "commons-io:commons-io",
            )
            include {
                dependencies.contains("${it.moduleGroup}:${it.moduleName}")
            }
            mergeServiceFiles()
            transform(Log4j2PluginsCacheFileTransformer())
        }
    }
    val copyTask = create<Copy>("copyBuildArtifact") {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs)
        rename { "SweetCheckout-bukkit-with-backend-$version.jar" }
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
