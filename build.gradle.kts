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
    }
}
