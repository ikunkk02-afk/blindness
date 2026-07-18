package com.ikunkk02afk.blindness.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;

@Config(name = "blindness-client", wrapperName = "BlindnessClientConfig")
public class BlindnessClientConfigModel {
    @SectionHeader("visual")
    public boolean enableVisualPostProcessing = true;
    @RangeConstraint(min = 0.02, max = 0.30, decimalPlaces = 2) public double baseBrightness = 0.08;
    @RangeConstraint(min = 0.0, max = 4.0, decimalPlaces = 2) public double blurStrength = 1.5;
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double saturation = 0.15;
    @RangeConstraint(min = 0.5, max = 4.0, decimalPlaces = 2) public double outlineThickness = 1.25;
    @RangeConstraint(min = 0.0, max = 3.0, decimalPlaces = 2) public double outlineBrightness = 1.2;
    @RangeConstraint(min = 0.5, max = 3.0, decimalPlaces = 2) public double outlineDuration = 1.5;
    @RangeConstraint(min = 2.0, max = 20.0, decimalPlaces = 1) public double waveSpeed = 10.0;
    public boolean showCreatureBlurredOutline = false;

    @SectionHeader("accessibility")
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double cameraShakeStrength = 0.45;
    public boolean disableFirstPersonFallTilt = false;
    public boolean showTutorial = true;
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double soundPromptVolume = 0.8;
}
