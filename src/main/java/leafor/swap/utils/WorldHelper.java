package leafor.swap.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.annotation.Nonnull;
import java.io.File;

public final class WorldHelper {
  private WorldHelper() {}

  public static boolean DeleteWorld(@Nonnull File path) {
    if (path.exists()) {
      var files = path.listFiles();
      if (files == null) {
        return false;
      }
      for (var file : files) {
        if (file.isDirectory()) {
          DeleteWorld(file);
        } else if (!file.delete()) {
          return false;
        }
      }
    }
    return path.delete();
  }

  public static boolean UnloadWorld(@Nonnull World w) {
    w.getPlayers().forEach(p -> p.kickPlayer("World is unloading"));
    return Bukkit.unloadWorld(w, false);
  }
}
