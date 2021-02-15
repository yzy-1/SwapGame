package leafor.swap.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.annotation.Nonnull;
import java.io.File;

public final class WorldHelper {
  private WorldHelper() {}

  public static boolean UnloadWorld(@Nonnull World w) {
    /*
      这里不能写 BringToLobby
      因为 BringToLobby 似乎是异步的, 执行需要时间
      下面的 unloadWorld 会因为玩家还没全部踢出而执行失败
     */
    w.getPlayers().forEach(p -> p.kickPlayer("World is unloading"));
    return Bukkit.unloadWorld(w, false);
  }

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
}
