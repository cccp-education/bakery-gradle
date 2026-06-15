package bakery

object BakeryConstants {
    const val BAKERY_GROUP = "bakery"
    const val GENERATE_GROUP = "generate"
    const val DEPLOY_GROUP = "deploy"
    const val TRANSFORM_GROUP = "transform"
    const val INFO_GROUP = "info"
    const val COLLECT_GROUP = "collect"
    const val VALIDATE_GROUP = "validate"
    const val BAKE_TASK = "bake"
    const val ASCIIDOCTOR_OPTION_REQUIRES = "asciidoctor.option.requires"
    const val ASCIIDOCTOR_DIAGRAM = "asciidoctor-diagram"
    const val ASCIIDOC_ATTRIBUTES_PROP = "asciidoctor.attributes"
    const val ASCIIDOC_DIAGRAMS_DIRECTORY = "imagesDir=diagrams"
    const val ASCIIDOC_SOURCE_DIR = "sourceDir"
    const val BAKERY_CONFIG_PATH_KEY = "bakery.config.path"
    const val CNAME = "CNAME"

    val SUPPORTED_LANGS = setOf("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")
}
