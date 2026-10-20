package net.montoyo.wd.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.platform.WDRegistrar;

import java.util.function.Supplier;

/**
 * Fabric registration. Unlike Forge there is no deferred registry, so entries are created and
 * registered immediately and the supplier just hands the same instance back.
 */
public class WebDisplaysFabricRegistrar implements WDRegistrar {

    private static ResourceLocation id(String name) {
        return new ResourceLocation(WebDisplays.MOD_ID, name);
    }

    @Override
    public <T extends Block> Supplier<T> block(String name, Supplier<T> factory) {
        T block = factory.get();
        Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        return () -> block;
    }

    @Override
    public <T extends Item> Supplier<T> item(String name, Supplier<T> factory) {
        T item = factory.get();
        Registry.register(BuiltInRegistries.ITEM, id(name), item);
        return () -> item;
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name,
                                                                           Supplier<BlockEntityType<T>> factory) {
        BlockEntityType<T> type = factory.get();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name), type);
        return () -> type;
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> menu(String name, Supplier<MenuType<T>> factory) {
        MenuType<T> type = factory.get();
        Registry.register(BuiltInRegistries.MENU, id(name), type);
        return () -> type;
    }

    @Override
    public Supplier<SoundEvent> sound(String name) {
        ResourceLocation rl = id(name);
        SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, rl,
                SoundEvent.createVariableRangeEvent(rl));
        return () -> event;
    }
}
