plugins {
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}

// JDK 17 test workers on Windows cannot reliably load classes from Unicode paths.
// Keep sources in this workspace and use an ASCII path for generated build files.
subprojects {
    if (System.getProperty("os.name").startsWith("Windows") && rootDir.path.any { it.code > 127 }) {
        layout.buildDirectory.set(
            File(gradle.gradleUserHomeDir, "recipe-import-build/${rootDir.path.hashCode().toUInt()}/$name")
        )
    }
}
