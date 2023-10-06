package com.minikloon.kloonmute;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod(KloonMute.MODID)
public class KloonMute {
    public static final String MODID = "kloonmute";
    public static KloonMute INSTANCE;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Lazy<KeyMapping> MUTE_KEY_MAPPING = Lazy.of(() -> new KeyMapping("key.kloonmute.keybindname",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            "key.kloonmute.keybindcategory"
    ));

    private double recordedSoundLevel;

    public KloonMute() {
        INSTANCE = this;

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerBindings(RegisterKeyMappingsEvent event) {
            event.register(MUTE_KEY_MAPPING.get());
        }
    }
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            while (MUTE_KEY_MAPPING.get().consumeClick()) {
                toggleMute();
            }
        }
    }

    private static void toggleMute() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        Options options = minecraft.options;

        OptionInstance<Double> masterVolumeOption = options.getSoundSourceOptionInstance(SoundSource.MASTER);
        double soundLevel = masterVolumeOption.get();
        LOGGER.info("Sound level: " + soundLevel);
        if (soundLevel == 0) {
            if (KloonMute.INSTANCE.recordedSoundLevel == 0) {
                minecraft.player.sendSystemMessage(Component.translatable("com.kloonmute.could_not_unmute").withStyle(ChatFormatting.RED));
            } else {
                masterVolumeOption.set(KloonMute.INSTANCE.recordedSoundLevel);
                minecraft.player.sendSystemMessage(Component.translatable("com.kloonmute.unmuted").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            minecraft.player.sendSystemMessage(Component.translatable("com.kloonmute.muted_game").withStyle(ChatFormatting.LIGHT_PURPLE));

            KloonMute.INSTANCE.recordedSoundLevel = soundLevel;
            masterVolumeOption.set(0.0);
        }
    }
}
