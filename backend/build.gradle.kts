plugins {
    id ("com.github.johnrengelman.shadow")
}

val targetJavaVersion = 8
val shadowGroup = "top.mrxiaom.sweet.checkout.backend.libs"

dependencies {
    implementation("com.zaxxer:HikariCP:4.0.3") { isTransitive = false }
    implementation("org.jetbrains:annotations:21.0.0")
}

fun Jar.setupManifest() {
    manifest {
        attributes(
            "main-class" to "top.mrxiaom.sweet.checkout.backend.ConsoleMain"
        )
    }
}
tasks {
    shadowJar {
        archiveClassifier.set("")
        setupManifest()
        mapOf(
            "org.intellij.lang.annotations" to "annotations.intellij",
            "org.jetbrains.annotations" to "annotations.jetbrains",
            "com.zaxxer.hikari" to "hikari",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
    }
    build {
        dependsOn(shadowJar)
    }
}
