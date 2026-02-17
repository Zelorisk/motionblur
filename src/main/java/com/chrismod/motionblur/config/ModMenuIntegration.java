package com.chrismod.motionblur.config;

import com.chrismod.motionblur.MotionBlurMod;
import com.chrismod.motionblur.gui.MotionBlurScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MotionBlurScreen(parent, MotionBlurMod.getConfig());
    }
}
