package leafor.swap.listenters;

import leafor.swap.Main;
import leafor.swap.utils.WorldHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.annotation.Nonnull;

public final class CleanListener extends GameListener {
  public CleanListener(@Nonnull World gameWorld) {
    Bukkit.getPluginManager().registerEvents(this, Main.GetInstance());
    Bukkit.resetRecipes();
    if (!WorldHelper.UnloadWorld(gameWorld)) {
      throw new RuntimeException("Unload world failure");
    }
    if (!WorldHelper.DeleteWorld(gameWorld.getWorldFolder())) {
      throw new RuntimeException("Delete world failure");
    }
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().runTask(Main.GetInstance(), GameListener::Init);
  }

  @EventHandler
  public void OnPlayerJoin(PlayerJoinEvent e) {
    var p = e.getPlayer();
    e.setJoinMessage("");
    p.kickPlayer("Server is cleaning!");
  }
}
