package bakery.scenarios

/**
 * Layout-specific Cucumber steps — BKY-JB-7.
 *
 * All step definitions are reused from ThymeleafComponentSteps
 * and GoogleFormsSteps (file existence + content assertions).
 * This class exists to keep the step binding discoverable
 * should layout-specific steps be needed later.
 */
class LayoutSteps(private val world: BakeryWorld)