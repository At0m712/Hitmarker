package atom.hitmarker;

import atom.hitmarker.sounds.ModSounds;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

public class HitMarkerClient implements ClientModInitializer {

    private static final float HIT_DURATION = 0.5F;
    private static final float FADE_DURATION = 0.25F;

    public static float projectileHitTimer = 0;
    public static boolean isKillIndicator = false;
    public static LivingEntity lastHitEntity = null;

    public static boolean killSoundPlayed = false;
    public static boolean hitSoundPlayed = false;
    public static int ticksSinceHit = 0;
    private static SoundInstance activeHitSound = null;

    // Déclaration des textures pour le dessin
    private static final Identifier HIT_SPRITE_1 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit1");
    private static final Identifier HIT_SPRITE_2 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit2");
    private static final Identifier HIT_SPRITE_3 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit3");
    private static final Identifier HIT_SPRITE_4 = Identifier.fromNamespaceAndPath("hitmarker", "crosshair/hit4");

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralCommandNode<FabricClientCommandSource> hitmarkerNode = dispatcher.register(ClientCommands.literal("hitmarker")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("§aHitMarker: Sound=" + (ModConfig.playSound ? "Enabled" : "Disabled") + ", Style=" + ModConfig.crosshairStyle));
                        context.getSource().sendFeedback(Component.literal("§7Usage: /hitmarker <sound|style> <value>"));
                        return 1;
                    })
                    .then(ClientCommands.literal("sound")
                            .then(ClientCommands.argument("enabled", BoolArgumentType.bool())
                                    .executes(context -> {
                                        boolean isEnabled = BoolArgumentType.getBool(context, "enabled");
                                        ModConfig.playSound = isEnabled;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.literal("§aHitMarker Sound : " + (isEnabled ? "Enabled" : "Disabled")));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommands.literal("style")
                            .then(ClientCommands.argument("type", IntegerArgumentType.integer(1, 4))
                                    .executes(context -> {
                                        int style = IntegerArgumentType.getInteger(context, "type");
                                        ModConfig.crosshairStyle = style;
                                        ModConfig.save();
                                        context.getSource().sendFeedback(Component.literal("§aHitMarker Style : " + style));
                                        return 1;
                                    })
                            )
                    )
            );
            dispatcher.register(ClientCommands.literal("HitMarker").redirect(hitmarkerNode));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetState());

        // Rendu visuel via Fabric HudElementRegistry
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("hitmarker", "render_hitmarker"),
                (graphics, tickCounter) -> {
                    if (projectileHitTimer <= 0.0F) {
                        return;
                    }

                    float smoothTimer = Math.max(0.0F, projectileHitTimer - 0.05F * tickCounter.getGameTimeDeltaPartialTick(false));
                    float alpha = Math.min(1.0F, smoothTimer / FADE_DURATION);
                    int alphaByte = (int) (alpha * 255.0F) & 0xFF;
                    if (alphaByte <= 0) {
                        return;
                    }

                    Identifier spriteToUse = HIT_SPRITE_1;
                    if (ModConfig.crosshairStyle == 2) spriteToUse = HIT_SPRITE_2;
                    if (ModConfig.crosshairStyle == 3) spriteToUse = HIT_SPRITE_3;
                    if (ModConfig.crosshairStyle == 4) spriteToUse = HIT_SPRITE_4;

                    int baseColor = isKillIndicator ? 0x00FF0000 : 0x00FFFFFF;
                    int color = (alphaByte << 24) | baseColor;

                    int width = 15;
                    int height = 15;

                    int x = (graphics.guiWidth() - width) / 2;
                    int y = (graphics.guiHeight() - height) / 2;

                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteToUse, x, y, width, height, color);
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (projectileHitTimer > 0) {
                projectileHitTimer -= 0.05F;
                if (projectileHitTimer <= 0) {
                    projectileHitTimer = 0;
                    lastHitEntity = null;
                    activeHitSound = null;
                }
            }

            if (lastHitEntity != null) {
                ticksSinceHit++;

                boolean isDead = lastHitEntity.isDeadOrDying() || lastHitEntity.getHealth() <= 0.0F;

                if (isDead) {
                    isKillIndicator = true;

                    if (!killSoundPlayed) {
                        if (activeHitSound != null) {
                            client.getSoundManager().stop(activeHitSound);
                            activeHitSound = null;
                        }
                        if (ModConfig.playSound) {
                            playUiSound(client, ModSounds.HIT_SOUND_2);
                        }
                        killSoundPlayed = true;
                        hitSoundPlayed = true;
                    }
                } else if (!hitSoundPlayed && ticksSinceHit >= 1) {
                    if (ModConfig.playSound) {
                        activeHitSound = playUiSound(client, ModSounds.HIT_SOUND_1);
                    }
                    hitSoundPlayed = true;
                }
            }
        });
    }

    private static SoundInstance playUiSound(Minecraft client, SoundEvent sound) {
        SimpleSoundInstance instance = SimpleSoundInstance.forUI(sound, 1.0F, 1.0F);
        client.getSoundManager().play(instance);
        return instance;
    }

    private static void resetState() {
        projectileHitTimer = 0;
        isKillIndicator = false;
        lastHitEntity = null;
        killSoundPlayed = false;
        hitSoundPlayed = false;
        ticksSinceHit = 0;
        activeHitSound = null;
    }

    public static void projectileHit(LivingEntity entity) {
        projectileHitTimer = HIT_DURATION;
        lastHitEntity = entity;
        ticksSinceHit = 0;
        hitSoundPlayed = false;
        killSoundPlayed = false;
        activeHitSound = null;
        isKillIndicator = entity.isDeadOrDying() || entity.getHealth() <= 0.0F;
    }
}
