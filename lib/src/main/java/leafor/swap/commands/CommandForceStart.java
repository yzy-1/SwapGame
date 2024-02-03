package leafor.swap.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import leafor.swap.listeners.GameListener;
import leafor.swap.listeners.WaitListener;

public class CommandForceStart implements CommandExecutor {
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(GameListener.GetListener() instanceof WaitListener)) {
      sender.sendMessage("Game is not in waiting.");
      return true;
    }
    var listener = (WaitListener) GameListener.GetListener();
    listener.StartGame();
    return true;
  }
}
