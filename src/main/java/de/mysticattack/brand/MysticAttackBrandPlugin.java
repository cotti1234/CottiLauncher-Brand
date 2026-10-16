package de.mysticattack.brand;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Ueberschreibt den Server-Brand (rechts oben im F3-Debug-Screen)
 * mit "Mysticattack.de" als Farbverlauf.
 */
@Plugin(
        id = "mysticattack-brand",
        name = "MysticAttackBrand",
        version = "1.0.0",
        authors = {"MysticAttack"}
)
public class MysticAttackBrandPlugin {

    private static final MinecraftChannelIdentifier BRAND_CHANNEL =
            MinecraftChannelIdentifier.create("minecraft", "brand");

    // ---- Hier anpassen ----
    private static final String BRAND_TEXT = "MysticAttack";
    private static final String START_COLOR = "540D83";
    private static final String END_COLOR = "58358F";
    // ------------------------

    private final ProxyServer server;
    private final Logger logger;
    private byte[] cachedBrandPacket;

    @Inject
    public MysticAttackBrandPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    // Ansatz A: statt die durchlaufende Konfigurationsphasen-Nachricht
    // abzufangen (die ViaVersion vorher auf Netty-Ebene wegschnappt),
    // schicken wir NACH dem Verbinden aktiv unsere eigene Brand-Nachricht.
    // Der Client akzeptiert ein spaeteres Update auch in der Play-Phase
    // und aktualisiert die F3-Anzeige damit.
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        CompletableFuture.runAsync(() -> {
            try {
                // kleine Verzoegerung, damit der Kanal beim Client sicher
                // registriert ist, bevor wir senden
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (player.isActive()) {
                byte[] packet = getBrandPacket();
                player.sendPluginMessage(BRAND_CHANNEL, packet);
                logger.info("Brand-Nachricht an {} gesendet (Laenge: {} bytes)", player.getUsername(), packet.length);
            }
        });
    }

    // Ansatz B (Fallback): falls die Nachricht doch mal ganz normal durchs
    // Event-System laeuft (z.B. bei einem spaeteren Serverwechsel), gleich
    // mit ersetzen. Schadet nicht, greift aber ggf. nicht wegen ViaVersion.
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(BRAND_CHANNEL)) {
            return;
        }
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (event.getTarget() instanceof Player player) {
            player.sendPluginMessage(BRAND_CHANNEL, getBrandPacket());
        }
    }

    private byte[] getBrandPacket() {
        if (cachedBrandPacket == null) {
            String gradientText = buildGradient(START_COLOR ,BRAND_TEXT, END_COLOR);
            cachedBrandPacket = encodeMinecraftString(gradientText);
        }
        return cachedBrandPacket;
    }

    /**
     * Erzeugt einen Farbverlauf zwischen zwei Hex-Farben, Zeichen fuer Zeichen,
     * im Legacy-Hex-Format (§x§R§R§G§G§B§B), das der Client seit 1.16 versteht.
     */
    private static String buildGradient(String text, String startHex, String endHex) {
        int startR = Integer.parseInt(startHex.substring(0, 2), 16);
        int startG = Integer.parseInt(startHex.substring(2, 4), 16);
        int startB = Integer.parseInt(startHex.substring(4, 6), 16);
        int endR = Integer.parseInt(endHex.substring(0, 2), 16);
        int endG = Integer.parseInt(endHex.substring(2, 4), 16);
        int endB = Integer.parseInt(endHex.substring(4, 6), 16);

        StringBuilder result = new StringBuilder();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            double ratio = (len == 1) ? 0 : (double) i / (len - 1);
            int r = (int) Math.round(startR + ratio * (endR - startR));
            int g = (int) Math.round(startG + ratio * (endG - startG));
            int b = (int) Math.round(startB + ratio * (endB - startB));
            String hex = String.format("%02X%02X%02X", r, g, b);

            result.append(toLegacyHexCode(hex));
            result.append(text.charAt(i));
        }

        return result.toString();
    }

    private static String toLegacyHexCode(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    /**
     * Kodiert den String im Minecraft-Protokollformat:
     * VarInt-Laenge gefolgt von UTF-8-Bytes.
     */
    private static byte[] encodeMinecraftString(String value) {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarInt(out, utf8.length);
        out.write(utf8, 0, utf8.length);
        return out.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
