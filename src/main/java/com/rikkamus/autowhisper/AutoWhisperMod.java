package com.rikkamus.autowhisper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(AutoWhisperMod.MOD_ID)
public class AutoWhisperMod {

    public static final String MOD_ID = "autowhisper";

    public static final Logger LOGGER = LogUtils.getLogger();

    private String targetPlayerName;

    public AutoWhisperMod() {
        this.targetPlayerName = null;

        NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(this::onClientLoggedIn);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onOutgoingChatMessage);
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("aw").executes(this::onAutoWhisperCommand));
        event.getDispatcher().register(Commands.literal("aw").then(Commands.argument("target", new PlayerNameArgument()).executes(this::onAutoWhisperTargetCommand)));
    }

    private void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Reset target when joining new world/server
        this.targetPlayerName = null;
    }

    private void onOutgoingChatMessage(ClientChatEvent event) {
        if (this.targetPlayerName != null) {
            event.setCanceled(true);
            Minecraft.getInstance().getConnection().sendCommand(String.format("msg %s %s", this.targetPlayerName, event.getMessage()));
        }
    }

    private int onAutoWhisperCommand(CommandContext<CommandSourceStack> context) {
        if (this.targetPlayerName != null) {
            // Turn off automatic whispering
            this.targetPlayerName = null;
        } else {
            // Try to turn on automatic whispering and set target to nearest player
            Optional<String> nearestPlayer = getNameOfNearestPlayer();

            if (nearestPlayer.isEmpty()) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cNo players found."));
                return 0;
            }

            this.targetPlayerName = nearestPlayer.get();
        }

        showStatusMessage();
        return Command.SINGLE_SUCCESS;
    }

    private int onAutoWhisperTargetCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // Turn on automatic whispering and set target
        this.targetPlayerName = context.getArgument("target", String.class);
        showStatusMessage();

        return Command.SINGLE_SUCCESS;
    }

    private Optional<String> getNameOfNearestPlayer() {
        final LocalPlayer localPlayer = Minecraft.getInstance().player;

        AbstractClientPlayer nearestPlayer = null;
        double minDistance = 0;

        for (AbstractClientPlayer player : localPlayer.clientLevel.players()) {
            if (localPlayer.equals(player)) continue;

            double distance = player.position().distanceTo(localPlayer.position());

            if (nearestPlayer == null || distance < minDistance) {
                nearestPlayer = player;
                minDistance = distance;
            }
        }

        return Optional.ofNullable(nearestPlayer).map(player -> player.getGameProfile().getName());
    }

    private void showStatusMessage() {
        Component message;

        if (this.targetPlayerName != null) {
            message = Component.literal(String.format("§bAuto whisper §aenabled§b. Target: §e%s", this.targetPlayerName));
        } else {
            message = Component.literal("§bAuto whisper §cdisabled§b.");
        }

        Minecraft.getInstance().gui.getChat().addMessage(message);
    }

}
