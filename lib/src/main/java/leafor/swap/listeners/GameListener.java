package leafor.swap.listeners;

import org.bukkit.event.Listener;

import javax.annotation.Nonnull;


public abstract class GameListener implements Listener {
  private static GameListener listener;

  protected static void SetListener(@Nonnull GameListener gm) {
    listener = gm;
  }

  public static GameListener GetListener() {
    return listener;
  }

  public static void Init() {
    listener = new InitListener();
  }
}
