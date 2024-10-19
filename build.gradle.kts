plugins {
    id ("com.github.johnrengelman.shadow") version "7.0.0" apply false
}
allprojects {
    group = "top.mrxiaom.sweet.checkout"
    version = "1.0.0"
}
subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    val targetJavaVersion = 8
    
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://jitpack.io")
        maven("https://repo.rosewooddev.io/repository/public/")
        maven("https://oss.sonatype.org/content/groups/public/")
    }

    project.extensions.configure<JavaPluginExtension> {
        val javaVersion = JavaVersion.toVersion(targetJavaVersion)
        if (JavaVersion.current() < javaVersion) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    project.extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components.getByName("java"))
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
    }
}
