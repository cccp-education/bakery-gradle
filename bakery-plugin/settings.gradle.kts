@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}
// GBL-014 — the publication channel is a build property, never a branch.
val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")

if (publicationChannel == "jitpack") {
    // CNV-16 — JitPack mirror for the education.cccp.build.* conventions
    // (bypasses Sonatype / Gradle Plugin Portal). The JitPack tag is pinned while
    // the canonical plugin declarations may stay on the released version.
    pluginManagement.repositories.maven("https://jitpack.io")
    pluginManagement.resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("education.cccp.build.")) {
            useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.19")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    id("com.gradleup.nmcp.settings").version("1.5.0")
}

val globalProps =
    java.util.Properties().also {
        val globalFile = file(System.getProperty("user.home") + "/.gradle/gradle.properties")
        if (globalFile.exists()) it.load(globalFile.inputStream())
    }

nmcpSettings {
    centralPortal {
        username = globalProps.getProperty("ossrhUsername") ?: ""
        password = globalProps.getProperty("ossrhPassword") ?: ""
        publishingType = "AUTOMATIC"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

// ── MEM-CAT-3 — Catalog workspace publié (MEMPHIS) : pin unique par borough (D4) ──
// GBL-014 — the catalogue is projected by channel: on JitPack the relabelled
// catalogue (`com.github.cccp-education...`) is served by the tag; on Central it
// is `education.cccp:workspace-catalog` (the `v` prefix is a tag-only concern).
// The version itself lives in `gradle.properties` (`workspaceCatalogVersion`);
// the platform BOM version lives in `workspaceBomVersion`. No literal here.
val workspaceCatalogVersion: String =
    providers.gradleProperty("workspaceCatalogVersion").orNull?.takeIf { it.isNotBlank() }
        ?: error("workspaceCatalogVersion is missing from gradle.properties")

dependencyResolutionManagement {
    versionCatalogs {
        create("ws") {
            if (publicationChannel == "central") {
                from("education.cccp:workspace-catalog:${workspaceCatalogVersion.removePrefix("v")}")
            } else {
                from("com.github.cccp-education.workspace-bom:workspace-catalog:$workspaceCatalogVersion")
            }
        }
    }
}

rootProject.name = "bakery-plugin"

if (publicationChannel == "jitpack") {
    dependencyResolutionManagement.repositories.maven("https://jitpack.io")
}
