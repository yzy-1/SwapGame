package leafor.swap.utils;

import leafor.swap.config.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import javax.annotation.Nonnull;
import java.util.Random;

public final class RandomSpawn {
  private RandomSpawn() {}

  public static Location Get(@Nonnull World w) {
    var rand = new Random();
    var radius = Config.game_area_radius;
    var originSpawn = w.getSpawnLocation();
    var originX = originSpawn.getBlockX();
    var originZ = originSpawn.getBlockZ();
    Block b;
    do {
      // 生成范围在 [-radius, radius) 之间的整数
      var x = rand.nextInt(radius * 2) - radius + originX;
      var z = rand.nextInt(radius * 2) - radius + originZ;
      var loc = new Location(w, x, 0, z);
      var chunk = loc.getChunk();
      if (!chunk.isLoaded()) {
        chunk.load(true);
      }
      b = w.getHighestBlockAt(loc);
    } while (b.getType() == Material.LAVA); // 玩家的重生点下面不能是岩浆
    return b.getLocation();
  }
}
