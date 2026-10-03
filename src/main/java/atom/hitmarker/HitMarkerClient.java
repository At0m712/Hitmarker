package atom.hitmarker;

import atom.hitmarker.sounds.ModSounds;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public class HitMarkerClient implements ClientModInitializer {

    public static float projectileHitTimer = 0;
    public static boolean isKillIndicator = false;
    public static LivingEntity lastHitEntity = null;

    public static boolean killSoundPlayed = false;
    public static boolean hitSoundPlayed = false;
    public static int ticksSinceHit = 0;

    // Déclaration des textures pour le dessin
    private static final Identifier HIT_SPRITE_1 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit1");
    private static final Identifier HIT_SPRITE_2 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit2");
    private static final Identifier HIT_SPRITE_3 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit3");
    private static final Identifier HIT_SPRITE_4 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit4");

    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("HitMarker")
                    .then(ClientCommands.literal("Sound")
                            .then(ClientCommands.argument("enabled", BoolArgumentType.bool())
                                    .executes(context -> {
                                        boolean isEnabled = BoolArgumentType.getBool(context, "enabled");
                                        ModConfig.playSound = isEnabled;
                                        context.getSource().sendFeedback(Component.literal("§aHitMarker Sound : " + (isEnabled ? "Enabled" : "Disabled")));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommands.literal("Style")
                            .then(ClientCommands.argument("type", IntegerArgumentType.integer(1, 4))
                                    .executes(context -> {
                                        int style = IntegerArgumentType.getInteger(context, "type");
                                        ModConfig.crosshairStyle = style;
                                        context.getSource().sendFeedback(Component.literal("§aHitMarker Style : " + style));
                                        return 1;
                                    })
                            )
                    )
            );
        });

        // Rendu visuel via Fabric HudElementRegistry
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("hitmarker", "render_hitmarker"),
                (graphics, tickCounter) -> {
                    if (projectileHitTimer > 0.0F) {
                        Identifier spriteToUse = HIT_SPRITE_1;
                        if (ModConfig.crosshairStyle == 2) spriteToUse = HIT_SPRITE_2;
                        if (ModConfig.crosshairStyle == 3) spriteToUse = HIT_SPRITE_3;
                        if (ModConfig.crosshairStyle == 4) spriteToUse = HIT_SPRITE_4;

                        int color = isKillIndicator ? 0xFFFF0000 : 0xFFFFFFFF;

                        int width = 15;
                        int height = 15;

                        int x = (graphics.guiWidth() - width) / 2;
                        int y = (graphics.guiHeight() - height) / 2;

                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteToUse, x, y, width, height, color);
                    }
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (projectileHitTimer > 0) {
                projectileHitTimer -= 0.05f;
                if (projectileHitTimer <= 0) {
                    projectileHitTimer = 0;
                    lastHitEntity = null;
                }
            }

            if (lastHitEntity != null) {
                ticksSinceHit++;

                boolean isDead = lastHitEntity.isDeadOrDying() || lastHitEntity.getHealth() <= 0.0F;

                if (isDead) {
                    isKillIndicator = true;

                    if (!killSoundPlayed && client.player != null) {
                        if (ModConfig.playSound) {
                            client.player.playSound(ModSounds.HIT_SOUND_2, 1.0F, 1.0F);
                        }
                        killSoundPlayed = true;
                        hitSoundPlayed = true;
                    }
                } else if (!hitSoundPlayed && ticksSinceHit >= 1) {
                    if (client.player != null) {
                        if (ModConfig.playSound) {
                            client.player.playSound(ModSounds.HIT_SOUND_1, 1.0F, 1.0F);
                        }
                    }
                    hitSoundPlayed = true;
                }
            }
        });
    }

    public static void projectileHit(LivingEntity entity) {
        projectileHitTimer = 0.5f;
        lastHitEntity = entity;
        ticksSinceHit = 0;
        hitSoundPlayed = false;
        killSoundPlayed = false;
        isKillIndicator = entity.isDeadOrDying() || entity.getHealth() <= 0.0F;
    }
}