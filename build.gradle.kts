import java.util.Locale

plugins {
    id ("com.github.johnrengelman.shadow") version "7.0.0" apply false
}
allprojects {
    group = "top.mrxiaom.sweet.checkout"
    version = "1.0.2"
}
subprojects {
    apply(plugin = "java")
    val targetJavaVersion = 8
    
    repositories {
        if (Locale.getDefault().country == "CN") {
            maven("https://mirrors.huaweicloud.com/repository/maven/")
        }
        mavenCentral()
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.helpch.at/releases/")
        maven("https://jitpack.io")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.rosewooddev.io/repository/public/")
    }

    project.extensions.configure<JavaPluginExtension> {
        val javaVersion = JavaVersion.toVersion(targetJavaVersion)
        if (JavaVersion.current() < javaVersion) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.isWarnings = false
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
}
