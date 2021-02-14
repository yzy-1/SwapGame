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
    var hand = p.getInventory().getItemInMainHand();
    var drop = switch (b.getType()) {
      case GOLD_ORE:
        yield Material.GOLD_INGOT;
      case IRON_ORE:
        yield Material.IRON_INGOT;
      case ANCIENT_DEBRIS:
        // 远古残骸
        yield Material.NETHERITE_SCRAP;
      default:
        yield null;
    };
    if (drop == null) {
      return false;
    }
    switch (hand.getType()) {
      case WOODEN_PICKAXE:
      case STONE_PICKAXE:
      case IRON_PICKAXE:
      case GOLDEN_PICKAXE:
      case DIAMOND_PICKAXE:
      case NETHERITE_PICKAXE:
        break;
      default:
        return false;
    }
    // 非生存模式下不自动熔炼
    if (!p.getGameMode().equals(GameMode.SURVIVAL)) {
      return false;
    }
    var loc = b.getLocation();
    b.setType(Material.AIR);
    b.getWorld().dropItemNaturally(
      loc.add(0.2, 0.2, 0.2), new ItemStack(drop));
    return true;
  }
}
