package leafor.swap.controllers;

import leafor.swap.Main;
import leafor.swap.config.Config;
import leafor.swap.utils.WorldHelper;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public final class InitController extends Controller {
  public InitController() {
    var w = GenerateGameWorld();
    Bukkit.resetRecipes();
    AddCustomRecipes();
    System.out.println("[SwapGame] Initialized");
    Bukkit.getScheduler().runTask(Main.GetInstance(), () ->
      Controller.SetController(new WaitController(w)));
  }

  private void AddCustomRecipes() {
    var m = Main.GetInstance();
    // 腐肉 -> 苹果
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_apple"), new ItemStack(Material.APPLE))
                       .shape("R")
                       .setIngredient('R', Material.ROTTEN_FLESH));
    // 金锭 + 苹果 -> 瞬间治疗药水
    var potion = new ItemStack(Material.SPLASH_POTION);
    var pMeta = (PotionMeta) potion.getItemMeta();
    pMeta.setColor(Color.RED);
    pMeta.addCustomEffect(new PotionEffect(
      PotionEffectType.HEAL, 1, 1), true);
    potion.setItemMeta(pMeta);
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_heal"), potion)
                       .shape("G", "A")
                       .setIngredient('G', Material.GOLD_INGOT)
                       .setIngredient('A', Material.APPLE));
    pMeta.setColor(Color.fromRGB(0xE49A3A));
    pMeta.addCustomEffect(new PotionEffect(
      PotionEffectType.HEAL, 1, 2), true);
    pMeta.addCustomEffect(
      new PotionEffect(
        PotionEffectType.FIRE_RESISTANCE, 400, 0),
      true);
    potion.setItemMeta(pMeta);
    // 钻石 + 苹果 -> 防火药水
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_fire"), potion)
                       .shape("D", "A")
                       .setIngredient('D', Material.DIAMOND)
                       .setIngredient('A', Material.APPLE));
    // 铁锭 * 3 -> 水桶
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_water"),
      new ItemStack(Material.WATER_BUCKET))
                       .shape("I I", " I ")
                       .setIngredient('I', Material.IRON_INGOT));
    // 水桶 + 钻石 -> 岩浆桶
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_lava"),
      new ItemStack(Material.LAVA_BUCKET))
                       .shape("D", "W")
                       .setIngredient('D', Material.DIAMOND)
                       .setIngredient('W', Material.WATER_BUCKET));
  }

  private World GenerateGameWorld() {
    final var WORLD_NAME = Config.game_world;
    var w = Bukkit.getWorld(WORLD_NAME);
    if (w != null) {
      w.getPlayers().forEach(p -> p.kickPlayer("World is unloading"));
      if (!WorldHelper.UnloadWorld(w)) {
        throw new RuntimeException("Unload world failure");
      }
      if (!WorldHelper.DeleteWorld(w.getWorldFolder())) {
        throw new RuntimeException("Delete world failure");
      }
    }
    w = Bukkit.createWorld(
      WorldCreator.name(WORLD_NAME).seed(new Random().nextLong()));
    if (w == null) {
      throw new RuntimeException("Could not create world");
    }
    w.setPVP(false);
    w.setDifficulty(Difficulty.HARD);
    w.setAutoSave(false);
    w.setHardcore(false);
    w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true); // 立即重生
    w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false); // 显示成就
    w.setGameRule(GameRule.NATURAL_REGENERATION, false); // 自然回复
    WorldBorder wb = w.getWorldBorder();
    wb.setCenter(w.getSpawnLocation());
    wb.setSize(Config.game_area_radius * 2);
    wb.setDamageAmount(Double.MAX_VALUE); // 使玩家离开边界时死亡
    return w;
  }
}
