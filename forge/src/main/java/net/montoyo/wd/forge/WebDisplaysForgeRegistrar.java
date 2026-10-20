package net.montoyo.wd.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.platform.WDRegistrar;

import java.util.function.Supplier;

/**
 * Forge registration through {@code DeferredRegister}. NeoForge 1.20.1 uses the exact same classes.
 */
public class WebDisplaysForgeRegistrar implements WDRegistrar {

    private final DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, WebDisplays.MOD_ID);
    private final DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, WebDisplays.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>> blockEntities =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WebDisplays.MOD_ID);
    private final DeferredRegister<MenuType<?>> menus =
            DeferredRegister.create(Registries.MENU, WebDisplays.MOD_ID);
    private final DeferredRegister<SoundEvent> sounds =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WebDisplays.MOD_ID);

    public WebDisplaysForgeRegistrar(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
        blockEntities.register(modBus);
        menus.register(modBus);
        sounds.register(modBus);
    }

    @Override
    public <T extends Block> Supplier<T> block(String name, Supplier<T> factory) {
        RegistryObject<T> obj = blocks.register(name, factory);
        return obj;
    }

    @Override
    public <T extends Item> Supplier<T> item(String name, Supplier<T> factory) {
        return items.register(name, factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name,
                                                                           Supplier<BlockEntityType<T>> factory) {
        return (Supplier<BlockEntityType<T>>) (Supplier<?>) blockEntities.register(name, factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> menu(String name, Supplier<MenuType<T>> factory) {
        return (Supplier<MenuType<T>>) (Supplier<?>) menus.register(name, factory);
    }

    @Override
    public Supplier<SoundEvent> sound(String name) {
        return sounds.register(name, () -> {
            ResourceLocation rl = new ResourceLocation(WebDisplays.MOD_ID, name);
            return SoundEvent.createVariableRangeEvent(rl);
        });
    }
}
