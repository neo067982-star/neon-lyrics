package dev.neonlyrics;

import dev.neonlyrics.util.MinecraftCompat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.config.Storage;
import dev.neonlyrics.cosmetic.DeathFx;
import dev.neonlyrics.cosmetic.PhoneMenu;
import dev.neonlyrics.gui.LicenseScreen;
import dev.neonlyrics.hud.BeatGlow;
import dev.neonlyrics.hud.Crosshair;
import dev.neonlyrics.hud.GlassHotbar;
import dev.neonlyrics.hud.Island;
import dev.neonlyrics.hud.StartupIntro;
import dev.neonlyrics.lyrics.Favorites;
import dev.neonlyrics.lyrics.SpotifyClient;
import dev.neonlyrics.sky.SkyRenderer;
import dev.neonlyrics.sky.FloatWords;
import dev.neonlyrics.sky.WorldSky;
import dev.neonlyrics.util.AutoPerf;
import dev.neonlyrics.util.Fx;
import dev.neonlyrics.util.Profiles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// $VF: Compiled from NeonLyrics.java
public final class NeonLyrics implements ClientModInitializer {
   public static final String MOD_ID = "neonlyrics";
   public static final Logger LOG = LoggerFactory.getLogger("NeonLyrics");
   private static Config config = new Config();
   public static KeyMapping keyMenu;
   public static KeyMapping keySkip;
   public static KeyMapping keyPrev;
   public static KeyMapping keyPause;
   public static KeyMapping keyLyrics;
   public static KeyMapping keySync;
   public static KeyMapping keyFav;
   public static KeyMapping keyClip;
   public static KeyMapping keyProfile;
   private static boolean licenseHint;

   public static Config cfg() {
      return config;
   }

   public static void saveConfig() {
      config.clamp();
      Storage.save("config.json", config);
   }

   public void onInitializeClient() {
      Storage.migrateFromAuction();
      config = Storage.load("config.json", Config.class, Config::new);
      config.clamp();
      saveConfig();
      Category cat = Category.register(Identifier.fromNamespaceAndPath("neonlyrics", "main"));
      keyMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.openmenu", MinecraftCompat.keyType(), 74, cat));
      keySkip = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.skip", MinecraftCompat.keyType(), 298, cat));
      keyPrev = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.prev", MinecraftCompat.keyType(), InputConstants.UNKNOWN.getValue(), cat));
      keyPause = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.pause", MinecraftCompat.keyType(), InputConstants.UNKNOWN.getValue(), cat));
      keyFav = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.fav", MinecraftCompat.keyType(), 78, cat));
      keyClip = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.clip", MinecraftCompat.keyType(), 299, cat));
      keyProfile = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.profile", MinecraftCompat.keyType(), 297, cat));
      keySync = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.sync", MinecraftCompat.keyType(), InputConstants.UNKNOWN.getValue(), cat));
      keyLyrics = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.neonlyrics.lyrics", MinecraftCompat.keyType(), InputConstants.UNKNOWN.getValue(), cat));
      HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath("neonlyrics", "sky"), SkyRenderer::render);
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("neonlyrics", "island"), Island::render);
      HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, GlassHotbar::wrap);
      HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, Crosshair::wrap);
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("neonlyrics", "beatglow"), BeatGlow::render);
      LevelRenderEvents.COLLECT_SUBMITS.register(WorldSky::render);
      LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(FloatWords::renderAfterTranslucent);
      SpotifyClient.init();
      StartupIntro.register();
      AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
         DeathFx.onAttack(entity);
         return InteractionResult.PASS;
      });
      ClientTickEvents.END_CLIENT_TICK.register(this::tick);
      ClientTickEvents.START_CLIENT_TICK.register(PhoneMenu::startTick);
      LOG.info("Lyrics Cord geladen");
   }

   private void tick(Minecraft mc) {
      if (!License.ok()) {
         while (keyMenu.consumeClick()) {
            if (MinecraftCompat.screen(mc) == null) {
               MinecraftCompat.setScreen(mc, new LicenseScreen());
            }
         }

         for (KeyMapping k : new KeyMapping[]{keySkip, keyPrev, keyPause, keySync, keyFav, keyClip, keyProfile, keyLyrics}) {
            while (k.consumeClick()) {
            }
         }

         if (!licenseHint && mc.player != null) {
            licenseHint = true;
            MinecraftCompat.chat(mc).addClientSystemMessage(Component.translatable("neonlyrics.lic.chat"));
         }
      } else {
         AutoPerf.tick(mc);
         PhoneMenu.tick(mc);
         DeathFx.tick(mc);
         SkyRenderer.tick(mc);

         while (keyMenu.consumeClick()) {
            if (MinecraftCompat.screen(mc) == null) {
               PhoneMenu.open(mc);
            }
         }

         while (keySkip.consumeClick()) {
            SpotifyClient.control(SpotifyClient.Cmd.NEXT);
         }

         while (keyPrev.consumeClick()) {
            SpotifyClient.control(SpotifyClient.Cmd.PREVIOUS);
         }

         while (keyPause.consumeClick()) {
            SpotifyClient.control(SpotifyClient.Cmd.PLAYPAUSE);
         }

         while (keySync.consumeClick()) {
            SpotifyClient.syncHere();
         }

         while (keyFav.consumeClick()) {
            Favorites.saveCurrent();
         }

         while (keyClip.consumeClick()) {
            MinecraftCompat.toggleGuiHidden(mc);
         }

         while (keyProfile.consumeClick()) {
            Profiles.toggle();
         }

         while (keyLyrics.consumeClick()) {
            config.lyricsSky = !config.lyricsSky;
            saveConfig();
            Island.notify(Fx.t(config.lyricsSky ? "neonlyrics.media.lyricson" : "neonlyrics.media.lyricsoff"), config.lyricsSky);
         }
      }
   }
}
