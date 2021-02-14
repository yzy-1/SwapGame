package leafor.swap;

import leafor.swap.config.Config;
import leafor.swap.controllers.Controller;
import leafor.swap.listenters.EventListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
  private static Main instance;

  public static Main GetInstance() {
    return instance;
  }

  @Override
  public void onEnable() {
    instance = this;
    var pm = getServer().getPluginManager();
    Config.GenerateDefault();
    Config.Load();
    Controller.Init();
    pm.registerEvents(new EventListener(), this);
  }

  @Override
  public void onDisable() {
    instance = null;
  }
}
