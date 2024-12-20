@file:OptIn(ExperimentalPathApi::class)

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.walk
import org.gradle.internal.os.OperatingSystem

plugins {
    `level-db-builds`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

tasks {

    register<Delete>("clean") {
        delete(layout.buildDirectory)
    }

    register<Zip>("testMergeZips") {
        val os = OperatingSystem.current()
        val zipTasks = when {
            os.isMacOsX -> listOf(appleZip, androidZip)
            os.isLinux -> listOf(linuxZip, androidZip, windowsX64Zip, windowsArm64Zip)
            os.isWindows -> listOf(windowsX64Zip, windowsArm64Zip, androidZip)
            else -> listOf(androidZip)
        }

        from("leveldb/include") {
            into("headers/include")
            include("**/c.h", "**/export.h")
        }

        dependsOn(zipTasks)
        archiveBaseName = "leveldb-test"
        destinationDirectory = layout.buildDirectory.dir("archives")
        zipTasks.forEach { from(it.flatMap { it.archiveFile }.map { zipTree(it) }) }
    }

    register<Zip>("mergeZips") {
        archiveBaseName = "leveldb"
        destinationDirectory = layout.buildDirectory.dir("archives")

        from("leveldb/include") {
            into("headers/include")
            include("**/c.h", "**/export.h")
        }

        // Merge all zips in the project directory when running in CI
        Path(".").walk()
            .filter { it.name.startsWith("leveldb-") && it.name.endsWith(".zip") }
            .forEach { from(zipTree(it)) }
    }
}
