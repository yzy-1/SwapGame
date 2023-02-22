package leafor.swap;

import leafor.swap.config.Config;
import leafor.swap.listeners.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
  private static Main instance;

  public static Main GetInstance() {
    return instance;
  }

  @Override
  public void onEnable() {
    instance = this;
    Config.GenerateDefault();
    Config.Load();
    if (Config.bungee_enabled) {
      Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }
    GameListener.Init();
  }

  @Override
  public void onDisable() {
    instance = null;
  }
}
