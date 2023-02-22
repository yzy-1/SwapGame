package leafor.swap.utils;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nonnull;

public final class AutoSmelt {
  private AutoSmelt() {}

  public static boolean TrySmelt(@Nonnull Player p, @Nonnull Block b) {
    // 非生存模式下不自动熔炼
    if (!p.getGameMode().equals(GameMode.SURVIVAL)) {
      return false;
    }
    var loc = b.getLocation();
    var items = b.getDrops().stream().map(x -> switch (x.getType()) {
      case GOLD_ORE:
      case RAW_GOLD:
        yield new ItemStack(Material.GOLD_INGOT);
      case RAW_COPPER:
      case RAW_IRON:
      case IRON_ORE:
        yield new ItemStack(Material.IRON_INGOT);
      case ANCIENT_DEBRIS:
        // 远古残骸
        yield new ItemStack(Material.NETHERITE_SCRAP);
      default:
        yield null;
    }).filter(x -> x != null).toList();
    if (items.size() <= 0)
      return false;
    items.forEach(x -> b.getWorld().dropItem(loc.add(0.2, 0.2, 0.2), x));
    b.setType(Material.AIR);
    return true;
  }
}
