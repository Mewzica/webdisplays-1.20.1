package net.montoyo.wd.platform;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Content registration surface, implemented once per loader.
 * <p>
 * Every method returns a {@link Supplier} rather than the object itself, because Forge and NeoForge
 * hand out a {@code RegistryObject} that only resolves after {@code RegisterEvent}. Fabric resolves
 * immediately, so its implementation just wraps the instance.
 */
public interface WDRegistrar {

    <T extends Block> Supplier<T> block(String name, Supplier<T> factory);

    <T extends Item> Supplier<T> item(String name, Supplier<T> factory);

    <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name,
                                                                    Supplier<BlockEntityType<T>> factory);

    <T extends AbstractContainerMenu> Supplier<MenuType<T>> menu(String name, Supplier<MenuType<T>> factory);

    Supplier<SoundEvent> sound(String name);
}
