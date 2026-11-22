package com.draxer.draxbackpacks;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Drax Backpacks: mochilas que guardan su contenido en el propio ítem.
 * Cuero 27 espacios, hierro 36, oro 45, diamante 54. Se mejoran en la mesa de crafteo sin perder lo que llevan.
 */
@Mod(DraxBackpacks.MODID)
public class DraxBackpacks {
    public static final String MODID = "drax_backpacks";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<BackpackItem> LEATHER = ITEMS.registerItem("leather_backpack",
            p -> new BackpackItem(p.stacksTo(1), 3));
    public static final DeferredItem<BackpackItem> IRON = ITEMS.registerItem("iron_backpack",
            p -> new BackpackItem(p.stacksTo(1), 4));
    public static final DeferredItem<BackpackItem> GOLD = ITEMS.registerItem("gold_backpack",
            p -> new BackpackItem(p.stacksTo(1).rarity(Rarity.UNCOMMON), 5));
    public static final DeferredItem<BackpackItem> DIAMOND = ITEMS.registerItem("diamond_backpack",
            p -> new BackpackItem(p.stacksTo(1).rarity(Rarity.RARE).fireResistant(), 6));

    public DraxBackpacks(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(DraxBackpacks::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(LEATHER);
            event.accept(IRON);
            event.accept(GOLD);
            event.accept(DIAMOND);
        }
    }
}
