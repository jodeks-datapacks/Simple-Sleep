package com.jodek.simplesleep.util;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.NotificationUtil;

public class MessageUtil {

    public static void sendActionBar(PlayerRef player, String message) {
        NotificationUtil.sendNotification(
            player.getPacketHandler(),
            Message.raw(message)
        );
    }

    public static void sendStyledNotification(PlayerRef player, String message, NotificationStyle style) {
        NotificationUtil.sendNotification(
            player.getPacketHandler(),
            Message.raw(message),
            style
        );
    }

    public static void sendNotificationWithSubtitle(PlayerRef player, String primaryMessage, String secondaryMessage) {
        NotificationUtil.sendNotification(
            player.getPacketHandler(),
            Message.raw(primaryMessage),
            Message.raw(secondaryMessage)
        );
    }

    public static void broadcastActionBar(Universe universe, String message) {
        // Create colored message
        Message msg = Message.raw(message).color("#FFFF00"); // Yellow

        // Send to each player in each world
        for (var world : universe.getWorlds().values()) {
            for (PlayerRef player : world.getPlayerRefs()) {
                NotificationUtil.sendNotification(
                    player.getPacketHandler(),
                    msg
                );
            }
        }
    }

    public static void broadcastNotificationWithSubtitle(Universe universe, String primary, String secondary) {
        Message primaryMsg = Message.raw(primary).color("#00FF00"); // Green
        Message secondaryMsg = Message.raw(secondary).color("#FFFF00"); // Yellow

        // Send to each player in each world
        for (var world : universe.getWorlds().values()) {
            for (PlayerRef player : world.getPlayerRefs()) {
                NotificationUtil.sendNotification(
                    player.getPacketHandler(),
                    primaryMsg,
                    secondaryMsg
                );
            }
        }
    }

    public static void broadcastStyledNotification(Universe universe, String message, NotificationStyle style) {
        Message msg = Message.raw(message);

        // Send to each player in each world
        for (var world : universe.getWorlds().values()) {
            for (PlayerRef player : world.getPlayerRefs()) {
                NotificationUtil.sendNotification(
                    player.getPacketHandler(),
                    msg,
                    style
                );
            }
        }
    }
}
