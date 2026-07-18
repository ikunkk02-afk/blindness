package com.ikunkk02afk.blindness.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;

@Config(name = "blindness-server", wrapperName = "BlindnessServerConfig")
public class BlindnessServerConfigModel {
    @SectionHeader("scan")
    @RangeConstraint(min = 2, max = 6)
    public int tapRange = 4;

    @RangeConstraint(min = 3, max = 8)
    public int sweepRange = 5;

    @RangeConstraint(min = 5, max = 40)
    public int tapCooldownTicks = 10;

    @RangeConstraint(min = 10, max = 80)
    public int sweepCooldownTicks = 20;

    @RangeConstraint(min = 20, max = 120)
    public int scannedPathTtlTicks = 60;

    @RangeConstraint(min = 16, max = 512)
    public int maxScanHits = 96;

    @RangeConstraint(min = 32, max = 512)
    public int maxPathNodes = 192;

    @SectionHeader("fall")
    @RangeConstraint(min = 0.0, max = 4.0, decimalPlaces = 2)
    public double maximumTripDamage = 2.0;

    @RangeConstraint(min = 0.0, max = 3.0, decimalPlaces = 2)
    public double tripRiskScale = 1.0;

    @RangeConstraint(min = 20, max = 100)
    public int protectionCooldownTicks = 40;
}
