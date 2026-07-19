package com.ikunkk02afk.blindness.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;

import java.util.ArrayList;
import java.util.List;

@Config(name = "blindness-client", wrapperName = "BlindnessClientConfig")
public class BlindnessClientConfigModel {
    @SectionHeader("visual")
    public boolean enableVisualPostProcessing = true;
    public boolean useDetailedModelOutlines = true;
    @RangeConstraint(min = 0.05, max = 0.5, decimalPlaces = 2) public double contactFadeInTime = 0.10;
    @RangeConstraint(min = 1.0, max = 10.0, decimalPlaces = 2) public double contactHoldTime = 5.00;
    @RangeConstraint(min = 0.4, max = 1.2, decimalPlaces = 2) public double contactFadeOutTime = 0.80;
    @RangeConstraint(min = 0.06, max = 0.12, decimalPlaces = 2) public double adjacentRevealDelay = 0.10;
    @RangeConstraint(min = 2.0, max = 6.0, decimalPlaces = 1) public double centerOutlineThickness = 4.0;
    @RangeConstraint(min = 1.5, max = 5.0, decimalPlaces = 1) public double adjacentOutlineThickness = 3.0;
    @RangeConstraint(min = 0.25, max = 2.0, decimalPlaces = 2) public double centerOutlineBrightness = 1.0;
    @RangeConstraint(min = 0.25, max = 1.5, decimalPlaces = 2) public double adjacentOutlineBrightness = 0.75;
    @RangeConstraint(min = 0.0, max = 16.0, decimalPlaces = 1) public double centerGlowRadius = 10.0;
    @RangeConstraint(min = 0.0, max = 16.0, decimalPlaces = 1) public double adjacentGlowRadius = 8.0;
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double centerGlowStrength = 0.55;
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double adjacentGlowStrength = 0.35;
    public boolean keepHeldCaneVisible = true;
    public boolean blackScreenBehindMenus = true;
    public boolean debugOutlineRendering = false;

    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 1.0, max = 4.0, decimalPlaces = 2) public double outlineThickness = 3.0;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 0.25, max = 2.0, decimalPlaces = 2) public double outlineBrightness = 1.0;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 1.0, max = 6.0, decimalPlaces = 1) public double outlineGlowRadius = 4.0;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double outlineGlowStrength = 0.5;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 0.02, max = 0.30, decimalPlaces = 2) public double baseBrightness = 0.08;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 0.0, max = 4.0, decimalPlaces = 2) public double blurStrength = 1.5;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double saturation = 0.15;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 0.5, max = 3.0, decimalPlaces = 2) public double outlineDuration = 1.5;
    @Deprecated @ExcludeFromScreen
    @RangeConstraint(min = 2.0, max = 20.0, decimalPlaces = 1) public double waveSpeed = 10.0;
    @SectionHeader("accessibility")
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double cameraShakeStrength = 0.45;
    public boolean disableFirstPersonFallTilt = false;
    public boolean showTutorial = true;
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double soundPromptVolume = 0.8;

    public boolean cliffWarningText = true;
    public boolean cliffWarningNarration = true;
    public boolean cliffCameraFeedback = true;
    public boolean hostileWarningText = true;
    public boolean blurEntitySubtitles = true;
    @RangeConstraint(min = 0.0, max = 1.0, decimalPlaces = 2) public double entitySoundOutlineBrightness = 0.60;
    @RangeConstraint(min = 0.5, max = 3.0, decimalPlaces = 1) public double entityFootstepHoldTime = 1.2;
    @RangeConstraint(min = 0.5, max = 4.0, decimalPlaces = 1) public double entityAmbientHoldTime = 1.8;
    @RangeConstraint(min = 0.5, max = 4.0, decimalPlaces = 1) public double entityDangerHoldTime = 2.0;

    @SectionHeader("compatibility_restart_required")
    public boolean blockInformationMods = true;
    public boolean blockMapMods = true;
    public boolean blockWorldInfoHudMods = true;
    public boolean blockXrayMods = true;
    public List<String> additionalBlockedModIds = new ArrayList<>();
    public List<String> ignoredBlockedModIds = new ArrayList<>();
}
