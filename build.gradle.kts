
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}


val mixinConfig = "mixins.fixmod.json"

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "MixinConfigs" to mixinConfig,
            "ForceLoadAsMod" to "true",
            "FMLCorePluginContainsFMLMod" to "true",
            "TweakOrder" to "0"
        )
    }
}
