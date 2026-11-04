package atom.hitmarker;

import atom.hitmarker.sounds.ModSounds;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class HitMarkerClient implements ClientModInitializer {

    public static float projectileHitTimer = 0;
    public static boolean isKillIndicator = false;
    public static LivingEntity lastHitEntity = null;

    public static boolean killSoundPlayed = false;
    public static boolean hitSoundPlayed = false;
    public static int ticksSinceHit = 0;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String root : new String[]{"hitmarker", "HitMarker"}) {
                dispatcher.register(ClientCommandManager.literal(root)
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal("§aHitMarker: Sound=" + (ModConfig.playSound ? "Enabled" : "Disabled") + ", Style=" + ModConfig.crosshairStyle));
                            context.getSource().sendFeedback(Component.literal("§7Usage: /" + root + " <sound|style> <value>"));
                            return 1;
                        })
                        .then(createSoundNode("sound"))
                        .then(createSoundNode("Sound"))
                        .then(createStyleNode("style"))
                        .then(createStyleNode("Style"))
                );
            }
        });

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

    private static LiteralArgumentBuilder<FabricClientCommandSource> createSoundNode(String name) {
        return ClientCommandManager.literal(name)
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean isEnabled = BoolArgumentType.getBool(context, "enabled");
                            ModConfig.playSound = isEnabled;
                            ModConfig.save();
                            context.getSource().sendFeedback(Component.literal("§aHitMarker Sound : " + (isEnabled ? "Enabled" : "Disabled")));
                            return 1;
                        })
                );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createStyleNode(String name) {
        return ClientCommandManager.literal(name)
                .then(ClientCommandManager.argument("type", IntegerArgumentType.integer(1, 4))
                        .executes(context -> {
                            int style = IntegerArgumentType.getInteger(context, "type");
                            ModConfig.crosshairStyle = style;
                            ModConfig.save();
                            context.getSource().sendFeedback(Component.literal("§aHitMarker Style : " + style));
                            return 1;
                        })
                );
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
