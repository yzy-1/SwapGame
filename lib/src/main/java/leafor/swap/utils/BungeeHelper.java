package leafor.swap.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import leafor.swap.Main;
import leafor.swap.config.Config;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public final class BungeeHelper {
  private BungeeHelper() {}

  public static void BringToLobby(@Nonnull Player p) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("Connect");
    out.writeUTF(Config.bungee_lobbyServer);
    p.sendPluginMessage(
      Main.GetInstance(), "BungeeCord", out.toByteArray());
  }
}
