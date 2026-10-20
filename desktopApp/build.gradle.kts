import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.formdev:flatlaf:3.7.2")
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    testImplementation(kotlin("test"))
}

tasks.named<ProcessResources>("processResources") {
    from(rootProject.file("extension")) {
        into("extension")
    }
}

compose.desktop {
    application {
        mainClass = "com.pwdog.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            modules("java.sql", "jdk.httpserver")
            packageName = "PWDog"
            packageVersion = "0.2.1"
            description = "本地加密密码管理器"
            vendor = "PWDog"
            copyright = "Copyright 2026 PWDog"

            windows {
                iconFile.set(project.file("packaging/windows/PWDog.ico"))
                menuGroup = "PWDog"
                shortcut = true
                perUserInstall = true
                upgradeUuid = "31438ada-4e78-47b6-813f-48bec77822c7"
            }
        }
    }
}

// Rebuild the MSI with a WiX template that removes this Windows user's
// %LOCALAPPDATA%\PWDogData directory on uninstall (but never on upgrade).
val packageMsiWithCleanup = tasks.register<Exec>("packageMsiWithCleanup") {
    group = "compose desktop"
    description = "Build the MSI with per-user data cleanup on uninstall"
    dependsOn("createDistributable", ":unzipWix")

    val appImage = layout.buildDirectory.dir("compose/binaries/main/app/PWDog").get().asFile
    val destDir = layout.buildDirectory.dir("compose/binaries/main/msi-clean").get().asFile
    val wixDir = rootProject.layout.buildDirectory.dir("wix311").get().asFile
    val resources = layout.projectDirectory.dir("packaging/windows").asFile
    val icon = layout.projectDirectory.file("packaging/windows/PWDog.ico").asFile
    val jpackage = File(System.getProperty("java.home"), "bin/jpackage.exe")

    doFirst {
        destDir.deleteRecursively()
        destDir.mkdirs()
        environment("PATH", wixDir.absolutePath + File.pathSeparator + (System.getenv("PATH") ?: ""))
    }

    executable = jpackage.absolutePath
    args(
        "--type", "msi",
        "--app-image", appImage.absolutePath,
        "--name", "PWDog",
        "--app-version", "0.2.1",
        "--vendor", "PWDog",
        "--description", "本地加密密码管理器",
        "--copyright", "Copyright 2026 PWDog",
        "--win-menu",
        "--win-menu-group", "PWDog",
        "--win-shortcut",
        "--win-per-user-install",
        "--win-dir-chooser",
        "--win-upgrade-uuid", "31438ada-4e78-47b6-813f-48bec77822c7",
        "--install-dir", "PWDog",
        "--resource-dir", resources.absolutePath,
        "--icon", icon.absolutePath,
        "--dest", destDir.absolutePath,
    )

    doLast {
        val produced = destDir.listFiles { file -> file.extension.equals("msi", ignoreCase = true) }
            ?.firstOrNull()
            ?: throw GradleException("jpackage did not produce an MSI")
        val standardDir = layout.buildDirectory.dir("compose/binaries/main/msi").get().asFile
        standardDir.mkdirs()
        val target = standardDir.resolve(produced.name)
        produced.copyTo(target, overwrite = true)
        logger.lifecycle("Installer with uninstall cleanup written to ${target.absolutePath}")
    }
}

tasks.matching { it.name == "packageMsi" }.configureEach {
    finalizedBy(packageMsiWithCleanup)
}

tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Run the P0 encrypted database benchmark"
    mainClass.set("com.pwdog.desktop.BenchmarkMainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("seedDev") {
    group = "verification"
    description = "Seed a development account and sample entries"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.pwdog.desktop.DevSeedMainKt")
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.register<JavaExec>("dpapiCheck") {
    group = "verification"
    description = "Check Windows DPAPI and Windows Hello availability"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.pwdog.desktop.DpapiCheckMainKt")
    classpath = sourceSets["test"].runtimeClasspath
}
