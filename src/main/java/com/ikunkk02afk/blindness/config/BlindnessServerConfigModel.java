package com.ikunkk02afk.blindness.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;

@Config(name = "blindness-server", wrapperName = "BlindnessServerConfig")
public class BlindnessServerConfigModel {
    @SectionHeader("contact")
    @RangeConstraint(min = 20, max = 80)
    public int contactPathTtlTicks = 50;

    @RangeConstraint(min = 5, max = 40)
    public int tapCooldownTicks = 10;

    @RangeConstraint(min = 10, max = 80)
    public int sweepCooldownTicks = 20;

    @RangeConstraint(min = 32, max = 512)
    public int maxPathNodes = 192;

    @SectionHeader("cliff")
    public boolean cliffWarningsEnabled = true;

    @RangeConstraint(min = 2, max = 3)
    public int cliffDropThreshold = 2;

    @RangeConstraint(min = 4, max = 8)
    public int severeCliffDropThreshold = 4;

    @RangeConstraint(min = 1.0, max = 5.0, decimalPlaces = 1)
    public double cliffWarningCooldownSeconds = 2.0;

    @SectionHeader("entity_sound")
    public boolean entitySoundRevealEnabled = true;

    @RangeConstraint(min = 4.0, max = 16.0, decimalPlaces = 1)
    public double entitySoundRevealMaximumDistance = 12.0;

    @RangeConstraint(min = 1, max = 12)
    public int entitySoundMaximumEventsPerSecond = 8;

    @SectionHeader("hostile_awareness")
    public boolean hostileAwarenessEnabled = true;

    @RangeConstraint(min = 8.0, max = 14.0, decimalPlaces = 1)
    public double hostileAwarenessRange = 12.0;

    @RangeConstraint(min = 2.0, max = 10.0, decimalPlaces = 1)
    public double hostileWarningCooldownSeconds = 5.0;

    @RangeConstraint(min = 25.0, max = 45.0, decimalPlaces = 1)
    public double hostileDirectionErrorDegrees = 35.0;

    @Deprecated
    @ExcludeFromScreen
    @RangeConstraint(min = 2, max = 6)
    public int tapRange = 4;

    @Deprecated
    @ExcludeFromScreen
    @RangeConstraint(min = 3, max = 8)
    public int sweepRange = 5;

    @Deprecated
    @ExcludeFromScreen
    @RangeConstraint(min = 20, max = 120)
    public int scannedPathTtlTicks = 60;

    @Deprecated
    @ExcludeFromScreen
    @RangeConstraint(min = 16, max = 512)
    public int maxScanHits = 96;

    @SectionHeader("fall")
    @RangeConstraint(min = 0.0, max = 4.0, decimalPlaces = 2)
    public double maximumTripDamage = 2.0;

    @RangeConstraint(min = 0.0, max = 3.0, decimalPlaces = 2)
    public double tripRiskScale = 1.0;

    @RangeConstraint(min = 20, max = 100)
    public int protectionCooldownTicks = 40;
}
