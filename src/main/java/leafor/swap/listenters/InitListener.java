package leafor.swap.listenters;

import leafor.swap.Main;
import leafor.swap.config.Config;
import leafor.swap.utils.WorldHelper;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public final class InitListener extends GameListener {
  public InitListener() {
    Bukkit.getPluginManager().registerEvents(this, Main.GetInstance());
    var w = GenerateGameWorld();
    Bukkit.resetRecipes();
    AddCustomRecipes();
    System.out.println("[SwapGame] Initialized");
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().runTask(Main.GetInstance(), () ->
      GameListener.SetListener(new WaitListener(w)));
  }

  // 添加一些自定义配方
  private void AddCustomRecipes() {
    var m = Main.GetInstance();
    // 腐肉 -> 瓶子
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_bottle"),
      new ItemStack(Material.GLASS_BOTTLE))
                       .shape("R")
                       .setIngredient('R', Material.ROTTEN_FLESH));
    // 沙子 -> 瓶子
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_bottle2"),
      new ItemStack(Material.GLASS_BOTTLE))
                       .shape("S")
                       .setIngredient('S', Material.SAND));
    // 金锭 + 瓶子 -> 瞬间治疗药水
    var potion = new ItemStack(Material.SPLASH_POTION);
    var pMeta = (PotionMeta) potion.getItemMeta();
    pMeta.setColor(Color.RED);
    pMeta.addCustomEffect(new PotionEffect(
      PotionEffectType.HEAL, 1, 1), true);
    potion.setItemMeta(pMeta);
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_heal"), potion)
                       .shape("G", "B")
                       .setIngredient('G', Material.GOLD_INGOT)
                       .setIngredient('B', Material.GLASS_BOTTLE));
    pMeta.setColor(Color.fromRGB(0xE49A3A));
    pMeta.addCustomEffect(new PotionEffect(
      PotionEffectType.HEAL, 1, 2), true);
    pMeta.addCustomEffect(
      new PotionEffect(
        PotionEffectType.FIRE_RESISTANCE, 400, 0),
      true);
    potion.setItemMeta(pMeta);
    // 钻石 + 瓶子 -> 防火药水
    Bukkit.addRecipe(new ShapedRecipe(
      new NamespacedKey(m, "sg_fire"), potion)
                       .shape("D", "B")
                       .setIngredient('D', Material.DIAMOND)
                       .setIngredient('B', Material.GLASS_BOTTLE));
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

  // 生成游戏世界
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
    /*
      WARING:
      在 1.16.4/5 版本中, 新生成的世界将不会刷怪
      也就是 setDifficulty 无效
      这是一个来自 vanilla 的 bug, 所以本插件不会去修复它
      https://hub.spigotmc.org/jira/browse/SPIGOT-6330
      可以通过安装 Multiverse-Core 来修复这个 bug
     */
    w.setDifficulty(Difficulty.HARD);
    w.setAutoSave(false);
    w.setHardcore(false);
    w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true); // 立即重生
    w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false); // 不显示成就
    w.setGameRule(GameRule.NATURAL_REGENERATION, false); // 自然回复
    w.setGameRule(GameRule.DO_MOB_SPAWNING, true); // 怪物生成
    w.setGameRule(GameRule.DO_WEATHER_CYCLE, false); // 取消天气变化
    w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); // 暂时锁住时间
    w.setFullTime(Config.game_area_time);
    WorldBorder wb = w.getWorldBorder();
    wb.setCenter(w.getSpawnLocation());
    wb.setSize(Config.game_area_radius * 2 + 1); // 边界边长 = 半径 * 2 + 1
    wb.setDamageAmount(Double.MAX_VALUE); // 使玩家离开边界时死亡
    return w;
  }

  @EventHandler
  public void OnPlayerJoin(PlayerJoinEvent e) {
    var p = e.getPlayer();
    e.setJoinMessage("");
    p.kickPlayer("Server is initializing");
  }
}
