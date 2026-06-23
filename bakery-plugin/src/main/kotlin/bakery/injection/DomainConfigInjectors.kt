package bakery.injection

val configInjectors: Map<String, ConfigInjector> = InjectorRegistry.all
    .mapValues { (_, spec) -> ConfigInjector { lines, resolver -> spec.inject(lines, resolver) } }