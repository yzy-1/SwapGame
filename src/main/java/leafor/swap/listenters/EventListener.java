package leafor.swap.listenters;

import leafor.swap.controllers.*;
import leafor.swap.utils.AutoSmelt;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public final class EventListener implements Listener {
  @EventHandler
  public void OnPlayerJoin(PlayerJoinEvent e) {
    var p = e.getPlayer();
    e.setJoinMessage("");
    var c = Controller.GetController();
    if (c instanceof InitController) {
      p.kickPlayer("Server is initializing");
    } else if (c instanceof WaitController) {
      var wc = (WaitController) c;
      wc.AddPlayer(p);
    } else if (c instanceof RunController) {
      var rc = (RunController) c;
      if (p.hasPermission("leafor.swap.watch")) {
        rc.AddSpectator(p);
      } else {
        p.kickPlayer("Game is already running!");
      }
    } else if (c instanceof CleanController) {
      p.kickPlayer("Server is cleaning!");
    } else {
      throw new RuntimeException("bad case ...");
    }
  }

  @EventHandler
  public void OnPlayerQuit(PlayerQuitEvent e) {
    var p = e.getPlayer();
    e.setQuitMessage("");
    var c = Controller.GetController();
    if (c instanceof WaitController) {
      var wc = (WaitController) c;
      wc.QuitPlayer(p);
    } else if (c instanceof RunController) {
      var rc = (RunController) c;
      rc.QuitPlayer(p);
    }
  }

  @EventHandler
  public void OnPlayerDeath(PlayerDeathEvent e) {
    var p = e.getEntity();
    var c = Controller.GetController();
    if (c instanceof WaitController) {
      var wc = (WaitController) c;
      wc.InitPlayer(p);
      e.setCancelled(true);
    } else if (c instanceof RunController) {
      var rc = (RunController) c;
      rc.EliminatePlayer(p);
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void OnBlockBreak(BlockBreakEvent e) {
    var p = e.getPlayer();
    var c = Controller.GetController();
    if (c instanceof RunController) {
      if (AutoSmelt.TrySmelt(p, e.getBlock())) {
        e.setCancelled(false);
      }
    }
  }
}
