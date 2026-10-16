package de.mysticattack.brand;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * MysticAttack.de - Eigene Server-Brand
 *
 * Ersetzt die per Client/ViaVersion gesendete/empfangene Brand-Nachricht
 * (Channel "minecraft:brand") durch "MysticAttack.de" in der Farbe #501C8C.
 *
 * Da der Brand-String selbst kein Farbformat unterstützt (er wird im F3-Screen
 * als reiner String angezeigt, moderne Clients rendern dort aber MiniMessage/§x-Codes
 * teilweise mit), kodieren wir die Farbe über den §x Hex-Farbcode (funktioniert ab
 * 1.16+ Clients, was für ViaVersion-Nutzer i.d.R. zutrifft).
 */
@Plugin(
        id = "mysticattack-brand",
        name = "MysticAttackBrand",
        version = "1.0.0",
        description = "Setzt eine eigene Server-Brand-Nachricht (MysticAttack.de) statt Paper/Backend-Brand",
        authors = {"MysticAttack.de"}
)
public class MysticAttackBrand {

    // Standard-Channel für die Server-Brand (Vanilla/Paper/ViaVersion nutzen diesen)
    private static final MinecraftChannelIdentifier BRAND_CHANNEL =
            MinecraftChannelIdentifier.create("minecraft", "brand");

    // Hex-Farbcode #501C8C
    private static final String HEX_COLOR = "501C8C";
    private static final String BRAND_TEXT = "MysticAttack.de";

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public MysticAttackBrand(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Nur Nachrichten auf dem Brand-Channel abfangen
        if (!event.getIdentifier().equals(BRAND_CHANNEL)) {
            return;
        }

        // Nur Nachrichten, die vom Backend-Server zum Spieler gehen, sind hier relevant
        if (!(event.getSource() instanceof com.velocitypowered.api.proxy.ServerConnection)) {
            return;
        }

        // Original-Brand (Paper, HugoSMP.net, ...) verwerfen
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        com.velocitypowered.api.proxy.ServerConnection connection =
                (com.velocitypowered.api.proxy.ServerConnection) event.getSource();
        Player player = connection.getPlayer();

        try {
            byte[] payload = buildBrandPayload(coloredBrand());
            // Eigene Brand direkt zurück an den Client senden
            player.sendPluginMessage(BRAND_CHANNEL, payload);
        } catch (IOException e) {
            logger.warn("Konnte eigene Brand-Nachricht nicht senden", e);
        }
    }

    // Falls ein Spieler den Server wechselt, erneut die eigene Brand senden,
    // damit sie nicht durch die Brand des neuen Backend-Servers überschrieben wird.
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        try {
            byte[] payload = buildBrandPayload(coloredBrand());
            event.getPlayer().sendPluginMessage(BRAND_CHANNEL, payload);
        } catch (IOException e) {
            logger.warn("Konnte eigene Brand-Nachricht nicht senden", e);
        }
    }

    /**
     * Baut die farbige Brand-Nachricht als Minecraft-VarInt-String-Payload,
     * wie sie das Brand-Protokoll erwartet (VarInt-Länge + UTF-8-Bytes).
     */
    private byte[] buildBrandPayload(String text) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        writeVarInt(out, text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        out.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public String coloredBrand() {
        // §x§5§0§1§C§8§C = Hex-Farbe #501C8C vor dem Text
        StringBuilder hexCode = new StringBuilder("\u00A7x");
        for (char c : HEX_COLOR.toCharArray()) {
            hexCode.append('\u00A7').append(c);
        }
        return hexCode + BRAND_TEXT;
    }
}
