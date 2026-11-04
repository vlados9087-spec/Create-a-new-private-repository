
package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "caseihpuma";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<CaseIhPumaEntity>> CASE_IH_PUMA_ENTITY =
            ENTITIES.register("case_ih_puma",
                    () -> EntityType.Builder.<CaseIhPumaEntity>of(
                                    CaseIhPumaEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(3.0F, 2.5F)
                            .clientTrackingRange(10)
                            .build("case_ih_puma"));

    public static final RegistryObject<Item> CASE_IH_PUMA =
            ITEMS.register("case_ih_puma",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public ExampleMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);PumaNetwork.register();

        modEventBus.addListener(this::addCreative);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreative(CreativeModeTabEvent.BuildContents event) {
        if (event.getTab() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CASE_IH_PUMA);
        }
    }
}