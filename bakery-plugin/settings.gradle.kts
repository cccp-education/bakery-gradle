@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        google()
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
// education.cccp:workspace-catalog:0.0.36 — source de vérité des versions cross-borough.
// Le borough ne bump que ce pin ; les versions plugins éducatives viennent de ws.*.
dependencyResolutionManagement {
    versionCatalogs {
        create("ws") {
            from("education.cccp:workspace-catalog:0.0.36")
        }
    }
}

rootProject.name = "bakery-plugin"
