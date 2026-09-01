// Fix conflit classpath : Gradle 9.6.1 épingle le Kotlin embarqué (strictly 2.3.21)
// en conflit avec les constraints platform workspace-bom (2.4.10) tirées transitivement
// via bakery-plugin → document-plugin → plantuml-plugin → workspace-bom.
buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
            force("org.jetbrains:annotations:26.0.2-1")
        }
    }
}

plugins {
    alias(libs.plugins.bakery)
    alias(libs.plugins.readme)
}

bakery {
    configPath = file("site.yml").absolutePath
    ia {
        baseUrl = "http://localhost:11441"
        modelName = "gemma4:31b-cloud"
        enabled = true
        timeout = java.time.Duration.ofSeconds(300)
    }
    contentI18nMigration {
        sourceDir = "/home/cheroliv/workspace/office/sites/cheroliv.com/jbake"
        outputDir = "/home/cheroliv/workspace/office/sites/cheroliv.com/i18n"
        sourceLanguage = "fr"
        targetLanguages = listOf("en")
        dryRun = false
        excludePaths = listOf("content/draft")
        parallelism = 4
    }
}
