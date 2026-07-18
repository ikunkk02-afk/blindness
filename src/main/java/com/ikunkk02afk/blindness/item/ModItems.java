package com.ikunkk02afk.blindness.item;

import com.ikunkk02afk.blindness.BlindnessMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    public static final Item GUIDANCE_CANE = new GuidanceCaneItem(new Item.Settings().maxCount(1));

    private ModItems() {}

    public static void register() {
        Registry.register(Registries.ITEM, BlindnessMod.id("guidance_cane"), GUIDANCE_CANE);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(GUIDANCE_CANE));
    }
}
