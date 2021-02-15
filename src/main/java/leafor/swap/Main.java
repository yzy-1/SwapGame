package leafor.swap;

import leafor.swap.config.Config;
import leafor.swap.listenters.GameListener;
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
    GameListener.Init();
  }

  @Override
  public void onDisable() {
    instance = null;
  }
}
