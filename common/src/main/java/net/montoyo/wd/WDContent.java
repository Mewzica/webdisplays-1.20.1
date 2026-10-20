package net.montoyo.wd;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.montoyo.wd.block.PeripheralBlock;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.DefaultPeripheral;
import net.montoyo.wd.entity.PeripheralBlockEntity;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.item.CraftComponentItem;
import net.montoyo.wd.item.LaserPointerItem;
import net.montoyo.wd.item.LinkerItem;
import net.montoyo.wd.item.MinePadItem;
import net.montoyo.wd.item.OwnershipThiefItem;
import net.montoyo.wd.item.PeripheralItem;
import net.montoyo.wd.item.ScreenConfiguratorItem;
import net.montoyo.wd.item.UpgradeItem;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.platform.WDRegistrar;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Holds the {@link Supplier}s handed out by the loader's registrar.
 * <p>
 * The 1.12.2 mod used {@code GameRegistry} and static fields on the {@code @Mod} class. On 1.20.1 the
 * registry is frozen before mods run their common init, so every entry point fills these fields from
 * its own {@code DeferredRegister} / {@code Registry.register} call and the rest of the code only ever
 * sees the supplier.
 */
public final class WDContent {

    public static Supplier<ScreenBlock> SCREEN_BLOCK;
    public static Supplier<PeripheralBlock> PERIPHERAL_BLOCK;

    public static Supplier<BlockEntityType<ScreenBlockEntity>> SCREEN_BE_TYPE;
    public static Supplier<BlockEntityType<PeripheralBlockEntity>> PERIPHERAL_BE_TYPE;

    /** Block items. */
    public static Supplier<BlockItem> SCREEN_ITEM;
    public static final Map<DefaultPeripheral, Supplier<Item>> PERIPHERAL_ITEMS = new EnumMap<>(DefaultPeripheral.class);

    public static Supplier<Item> SCREEN_CONFIGURATOR;
    public static Supplier<Item> LINKER;
    public static Supplier<Item> MINEPAD;
    public static Supplier<Item> LASER_POINTER;
    public static Supplier<Item> OWNERSHIP_THIEF;
    public static Supplier<Item> UPGRADE;
    public static Supplier<Item> CRAFT_COMPONENT;

    public static Supplier<SoundEvent> SOUND_TYPING;
    public static Supplier<SoundEvent> SOUND_UPGRADE_ADD;
    public static Supplier<SoundEvent> SOUND_UPGRADE_DEL;
    public static Supplier<SoundEvent> SOUND_SCREENCFG;
    public static Supplier<SoundEvent> SOUND_SERVER;
    public static Supplier<SoundEvent> SOUND_IRONIC;

    private WDContent() {
    }

    /**
     * Called by every loader entry point after its own registration hook ran. Registration of the
     * entries themselves is loader-specific, so this only fills in the shared references.
     */
    public static void bootstrap() {
        WDRegistrar reg = Platform.get().registrar();

        SCREEN_BLOCK = reg.block("screen", ScreenBlock::new);
        PERIPHERAL_BLOCK = reg.block("peripheral", PeripheralBlock::new);

        SCREEN_BE_TYPE = reg.blockEntity("screen",
                () -> BlockEntityType.Builder.of(ScreenBlockEntity::new, SCREEN_BLOCK.get()).build(null));
        PERIPHERAL_BE_TYPE = reg.blockEntity("peripheral",
                () -> BlockEntityType.Builder.of(PeripheralBlockEntity::new, PERIPHERAL_BLOCK.get()).build(null));

        SCREEN_ITEM = reg.item("screen", () -> new BlockItem(SCREEN_BLOCK.get(), new Item.Properties()));

        for (DefaultPeripheral kind : DefaultPeripheral.values()) {
            if (!kind.isImplemented())
                continue;

            PERIPHERAL_ITEMS.put(kind, reg.item("peripheral_" + kind.getName(),
                    () -> new PeripheralItem(PERIPHERAL_BLOCK.get(), kind)));
        }

        SCREEN_CONFIGURATOR = reg.item("screen_configurator", ScreenConfiguratorItem::new);
        LINKER = reg.item("linker", LinkerItem::new);
        MINEPAD = reg.item("minepad", MinePadItem::new);
        LASER_POINTER = reg.item("laser_pointer", LaserPointerItem::new);
        OWNERSHIP_THIEF = reg.item("ownership_thief", OwnershipThiefItem::new);
        UPGRADE = reg.item("upgrade", UpgradeItem::new);
        CRAFT_COMPONENT = reg.item("craft_component", CraftComponentItem::new);

        SOUND_TYPING = reg.sound("keyboardType");
        SOUND_UPGRADE_ADD = reg.sound("upgradeAdd");
        SOUND_UPGRADE_DEL = reg.sound("upgradeDel");
        SOUND_SCREENCFG = reg.sound("screencfgOpen");
        SOUND_SERVER = reg.sound("server");
        SOUND_IRONIC = reg.sound("ironic");
    }
}
