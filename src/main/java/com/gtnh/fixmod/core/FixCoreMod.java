package com.gtnh.fixmod.core;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001) // Ensure it loads after other coremods if needed
public class FixCoreMod implements IFMLLoadingPlugin {

    public FixCoreMod() {
        System.out.println("FixCoreMod initialized.");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0]; // No custom ASM transformers needed, using Mixin
    }

    @Override
    public String getModContainerClass() {
        return null; // No custom ModContainer needed
    }

    @Override
    public String getSetupClass() {
        return null; // No custom setup class needed
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // No data injection needed
    }

    @Override
    public String getAccessTransformerClass() {
        return null; // No custom Access Transformer needed
    }
}
