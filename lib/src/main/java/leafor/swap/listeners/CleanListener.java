package leafor.swap.listeners;

import leafor.swap.Main;
import leafor.swap.config.Config;
import leafor.swap.utils.BungeeHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class CleanListener extends GameListener {
  public CleanListener(@Nonnull MultiverseWorld gameWorld) {
    Bukkit.getPluginManager().registerEvents(this, Main.GetInstance());
    Bukkit.resetRecipes();
    MultiverseCore core =
        (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
    MVWorldManager worldManager = core.getMVWorldManager();
    gameWorld.getCBWorld().getPlayers().forEach(p -> p.kick(Component.text("Server is unloading")));
    worldManager.removePlayersFromWorld(gameWorld.getName());
    if (!worldManager.deleteWorld(gameWorld.getName(), true, true)) {
      throw new RuntimeException("Delete world failure");
    }
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().runTask(Main.GetInstance(), GameListener::Init);
  }

  @EventHandler
  public void OnPlayerJoin(PlayerJoinEvent e) {
    var p = e.getPlayer();
    e.joinMessage(null);
    if (Config.bungee_enabled) {
      BungeeHelper.BringToLobby(Objects.requireNonNull(p));
    } else {
      p.kick(Component.text("Server is cleaning"));
    }
  }
}
