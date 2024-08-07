package leafor.swap.listeners;

import leafor.swap.Main;
import leafor.swap.commands.CommandForceStart;
import leafor.swap.config.Config;
import leafor.swap.utils.BungeeHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import java.util.Objects;

public final class InitListener extends GameListener {
  public InitListener() {
    Bukkit.getPluginManager().registerEvents(this, Main.GetInstance());
    Main.GetInstance().getCommand("force_start").setExecutor(new CommandForceStart());
    var w = GenerateGameWorld();
    Bukkit.resetRecipes();
    AddCustomRecipes();
    System.out.println("[SwapGame] Initialized");
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().runTask(Main.GetInstance(),
        () -> GameListener.SetListener(new WaitListener(Objects.requireNonNull(w))));
  }

  // 添加一些自定义配方
  private void AddCustomRecipes() {
    var m = Main.GetInstance();
    // 腐肉 -> 瓶子
    Bukkit.addRecipe(
        new ShapedRecipe(new NamespacedKey(m, "sg_bottle"), new ItemStack(Material.GLASS_BOTTLE))
            .shape("R").setIngredient('R', Material.ROTTEN_FLESH));
    // 沙子 -> 瓶子
    Bukkit.addRecipe(
        new ShapedRecipe(new NamespacedKey(m, "sg_bottle2"), new ItemStack(Material.GLASS_BOTTLE))
            .shape("S").setIngredient('S', Material.SAND));
    // 金锭 + 瓶子 -> 瞬间治疗药水
    var potion = new ItemStack(Material.SPLASH_POTION);
    var pMeta = (PotionMeta) potion.getItemMeta();
    pMeta.setColor(Color.RED);
    pMeta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 1), true);
    potion.setItemMeta(pMeta);
    Bukkit.addRecipe(new ShapedRecipe(new NamespacedKey(m, "sg_heal"), potion).shape("G", "B")
        .setIngredient('G', Material.GOLD_INGOT).setIngredient('B', Material.GLASS_BOTTLE));
    // 钻石 + 瓶子 -> 防火药水
    potion = new ItemStack(Material.SPLASH_POTION);
    pMeta = (PotionMeta) potion.getItemMeta();
    pMeta.setColor(Color.fromRGB(0xE49A3A));
    pMeta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 2), true);
    pMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 30 * 20, 1), true);
    pMeta.addCustomEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 30 * 20, 0), true);
    potion.setItemMeta(pMeta);
    Bukkit.addRecipe(new ShapedRecipe(new NamespacedKey(m, "sg_fire"), potion).shape("D", "B")
        .setIngredient('D', Material.DIAMOND).setIngredient('B', Material.GLASS_BOTTLE));
    // 铁锭 * 3 -> 水桶
    Bukkit.addRecipe(
        new ShapedRecipe(new NamespacedKey(m, "sg_water"), new ItemStack(Material.WATER_BUCKET))
            .shape("I I", " I ").setIngredient('I', Material.IRON_INGOT));
    // 弓 1
    Bukkit.addRecipe(new ShapedRecipe(new NamespacedKey(m, "sg_bow1"), new ItemStack(Material.BOW))
        .shape("SI ", "S I", "SI ").setIngredient('I', Material.IRON_INGOT)
        .setIngredient('S', Material.STICK));
    // 弓 2
    Bukkit.addRecipe(new ShapedRecipe(new NamespacedKey(m, "sg_bow2"), new ItemStack(Material.BOW))
        .shape(" IS", "I S", " IS").setIngredient('I', Material.IRON_INGOT)
        .setIngredient('S', Material.STICK));
    // 箭
    Bukkit.addRecipe(
        new ShapedRecipe(new NamespacedKey(m, "sg_arrow"), new ItemStack(Material.ARROW, 4))
            .shape("I", "S").setIngredient('I', Material.IRON_INGOT)
            .setIngredient('S', Material.STICK));
  }

  // 生成游戏世界
  private MultiverseWorld GenerateGameWorld() {
    final var WORLD_NAME = Config.game_world;
    MultiverseCore core =
        (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
    MVWorldManager worldManager = core.getMVWorldManager();
    worldManager.removePlayersFromWorld(WORLD_NAME);
    worldManager.deleteWorld(WORLD_NAME, true, true);
    if (!worldManager.addWorld(WORLD_NAME, World.Environment.NORMAL, null, WorldType.NORMAL, true,
        null, true))
      throw new RuntimeException("Can not add world");
    var w = Objects.requireNonNull(worldManager.getMVWorld(WORLD_NAME));
    w.setPVPMode(false);
    /*
     * WARING: 在 1.16.4/5 版本中, 新生成的世界将不会刷怪 也就是 setDifficulty 无效 这是一个来自 spigot 的 bug, 所以本插件不会去修复它
     * https://hub.spigotmc.org/jira/browse/SPIGOT-6330 可以通过安装 Multiverse-Core 或将 server.properties
     * 和大厅和的难度都设为一种非和平的难度 来修复这个 bug
     */
    w.setDifficulty(Difficulty.EASY);
    w.setHunger(false); // 不会饥饿
    w.setAutoHeal(false);// 自然恢复
    w.setBedRespawn(false);
    w.setGameMode(GameMode.SURVIVAL);
    w.getCBWorld().setAutoSave(false);
    w.getCBWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true); // 立即重生
    w.getCBWorld().setGameRule(GameRule.NATURAL_REGENERATION, false); // 自然恢复
    w.getCBWorld().setGameRule(GameRule.DO_INSOMNIA, false); // 幻翼
    w.getCBWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false); // 不显示成就
    w.getCBWorld().setGameRule(GameRule.DO_MOB_SPAWNING, true); // 怪物生成
    w.getCBWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false); // 取消天气变化
    w.getCBWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); // 暂时锁住时间
    w.getCBWorld().setFullTime(Config.game_area_time);
    WorldBorder wb = w.getCBWorld().getWorldBorder();
    wb.setCenter(w.getSpawnLocation());
    wb.setSize(Config.game_area_radius * 2 + 1); // 边界边长 = 半径 * 2 + 1
    wb.setDamageAmount(Double.MAX_VALUE); // 使玩家离开边界时死亡
    return w;
  }

  @EventHandler
  public void OnPlayerJoin(PlayerJoinEvent e) {
    var p = e.getPlayer();
    e.joinMessage(null);
    if (Config.bungee_enabled) {
      BungeeHelper.BringToLobby(Objects.requireNonNull(p));
    } else {
      Bukkit.getScheduler().runTask(Main.GetInstance(), () -> {
        p.kick(Component.text("Server is initializing"));
      });
    }
  }
}
