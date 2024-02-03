package leafor.swap.listeners;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import leafor.swap.Main;
import leafor.swap.config.Config;
import leafor.swap.utils.AutoSmelt;
import leafor.swap.utils.BungeeHelper;
import leafor.swap.utils.RandomSpawn;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class RunListener extends GameListener {
  private final Set<Player> players; // 游戏玩家
  private final Set<Player> spectators; // 观战玩家
  private final @Nonnull MultiverseWorld gameWorld; // 游戏世界
  private final SwapCountdown swapCountdown = new SwapCountdown();
  private final ShrinkCountDown shrinkCountdown = new ShrinkCountDown();
  private final HashMap<Material, Material> randomMap;
  private boolean isGameEnd = false;
  private boolean isFinalPvpPeriod = false;
  private boolean isProtectionPeriod = true;
  private boolean isBeginShrink = true;
  private BossBar swapCountDownBar;
  private BossBar shrinkCountDownBar;

  public RunListener(@Nonnull Set<Player> players, @Nonnull Set<Player> spectators,
      @Nonnull MultiverseWorld gameWorld) {
    Bukkit.getPluginManager().registerEvents(this, Main.GetInstance());
    this.players = players;
    this.spectators = spectators;
    this.gameWorld = gameWorld;
    this.randomMap = GetRandomMap();
    // 开始时间变化
    gameWorld.getCBWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
    this.swapCountDownBar = Bukkit.createBossBar("Protection period", BarColor.RED, BarStyle.SOLID);
    this.swapCountDownBar.setProgress(1.0);
    this.shrinkCountDownBar =
        Bukkit.createBossBar("Begin to shrink", BarColor.BLUE, BarStyle.SOLID);
    this.shrinkCountDownBar.setProgress(1.0);
    final var MAX_HEALTH = Config.game_player_health;
    // 初始化每个玩家
    players.forEach(p -> {
      p.teleport(RandomSpawn.Get(gameWorld, Config.game_area_radius));
      p.setGameMode(GameMode.SURVIVAL);
      p.setFireTicks(0);
      p.setFoodLevel(20);
      p.setSaturation(20);
      Objects.requireNonNull(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(MAX_HEALTH);
      p.setHealthScaled(false);
      p.setHealth(MAX_HEALTH);
      p.setExp(0);
      p.setLevel(0);
      var attr = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
      attr.setBaseValue(4);
      p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
      p.addPotionEffect(
          new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 1, false, false));
      p.addPotionEffect(
          new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
      p.addPotionEffect(
          new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
      var inv = p.getInventory();
      inv.clear();
      inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
      var compass = new ItemStack(Material.COMPASS);
      var cMeta = (CompassMeta) compass.getItemMeta();
      cMeta.setLodestone(gameWorld.getSpawnLocation());
      cMeta.setLodestoneTracked(false);
      compass.setItemMeta(cMeta);
      inv.setItem(0, compass);
      p.showTitle(Title.title(Component.text("Game start", TextColor.color(255, 0, 0)),
          Component.empty(),
          Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))));
      p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 1F, 1F);
      this.swapCountDownBar.addPlayer(p);
      this.shrinkCountDownBar.addPlayer(p);
    });
    // 将观战玩家 tp 到游戏场地
    spectators.forEach(p -> {
      InitSpectator(Objects.requireNonNull(p));
      p.teleport(gameWorld.getSpawnLocation());
      p.showTitle(Title.title(Component.text("Game start", TextColor.color(255, 0, 0)),
          Component.empty(),
          Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))));
      p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 1F, 1F);
      this.swapCountDownBar.addPlayer(p);
      this.shrinkCountDownBar.addPlayer(p);
    });
    swapCountdown.runTaskTimer(Main.GetInstance(), 0, 20);
    shrinkCountdown.runTaskTimer(Main.GetInstance(), 0, Config.game_area_shrinkInterval);
  }

  private HashMap<Material, Material> GetRandomMap() {
    var blocks = new ArrayList<>(Arrays.asList(Material.values()));
    blocks.removeIf(p -> p.isAir() || !p.isItem() || !p.isBlock());
    var toBlocks = new ArrayList<>(blocks);
    Collections.shuffle(toBlocks);
    HashMap<Material, Material> mp = new HashMap<>();
    for (int i = 0; i < blocks.size(); ++i)
      mp.put(blocks.get(i), toBlocks.get(i));
    return mp;
  }

  private void InitSpectator(@Nonnull Player p) {
    p.setGameMode(GameMode.SPECTATOR);
    p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    p.addPotionEffect(
        new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
    p.getInventory().clear();
    if (swapCountDownBar != null) {
      swapCountDownBar.addPlayer(p);
    }
    if (shrinkCountDownBar != null) {
      shrinkCountDownBar.addPlayer(p);
    }
  }

  // 结束游戏
  public void EndGame() {
    if (!isGameEnd) {
      players.forEach(x -> x.playSound(x.getLocation(), Sound.ENTITY_WITHER_DEATH,
          SoundCategory.MASTER, 1F, 1F));
      spectators.forEach(x -> x.playSound(x.getLocation(), Sound.ENTITY_WITHER_DEATH,
          SoundCategory.MASTER, 1F, 1F));
      isGameEnd = true;
      swapCountdown.cancel();
      shrinkCountdown.cancel();
      swapCountDownBar.removeAll();
      swapCountDownBar = null;
      shrinkCountDownBar.removeAll();
      shrinkCountDownBar = null;
      System.out.println("[SwapGame] Game over");
      HandlerList.unregisterAll(this);
      var m = Main.GetInstance();
      if (Config.bungee_enabled) {
        // 防止玩家没全部踢出
        Bukkit.getScheduler().runTaskLater(m, () -> {
          players.forEach(BungeeHelper::BringToLobby);
          spectators.forEach(BungeeHelper::BringToLobby);
        }, 5 * 20);
      }
      Bukkit.getScheduler().runTaskLater(m,
          () -> GameListener.SetListener(new CleanListener(gameWorld)), 10 * 20);
    }
  }

  // 检查是否有冠军产生
  private boolean CheckWinner() {
    if (players.size() == 0) {
      System.out.println("Oops! We can not decide who the winner is");
      EndGame();
    } else if (players.size() == 1) {
      var winner = (Player) players.toArray()[0];
      players.forEach(p -> {
        if (players.contains(p)) {
          p.showTitle(Title.title(Component.text("Congratulations!", TextColor.color(255, 0, 0)),
              Component.text("You won the game"), Times.times(Duration.ofMillis(500),
                  Duration.ofMillis(2000), Duration.ofMillis(500))));
        } else {
          p.showTitle(Title.title(Component.text("Game over!", TextColor.color(255, 0, 0)),
              winner.displayName().append(Component.text(" is the winner")), Times
                  .times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))));
        }
      });
      spectators.forEach(p -> {
        p.showTitle(Title.title(Component.text("Game over!", TextColor.color(255, 0, 0)),
            winner.displayName().append(Component.text(" is the winner")),
            Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))));
      });
      EndGame();
    }
    return players.size() <= 1;
  }

  private void RemovePlayer(@Nonnull Player p, Component message) {
    assert players.contains(p);
    var size = players.size() - 1;
    players.forEach(x -> {
      x.sendMessage(message);
      x.sendMessage("%s%s%d players left".formatted(ChatColor.RED, ChatColor.BOLD, size));
    });
    spectators.forEach(x -> {
      x.sendMessage(message);
      x.sendMessage("%s%s%d players left".formatted(ChatColor.RED, ChatColor.BOLD, size));
    });
    players.remove(p);
    if (swapCountDownBar != null) {
      swapCountDownBar.removePlayer(p);
    }
    if (shrinkCountDownBar != null) {
      shrinkCountDownBar.removePlayer(p);
    }
    CheckWinner();
  }

  // 淘汰玩家
  public void EliminatePlayer(@Nonnull Player p, Component message) {
    assert players.contains(p);
    gameWorld.getCBWorld().strikeLightningEffect(p.getLocation());
    p.showTitle(Title.title(Component.text("You lost the game!", TextColor.color(255, 0, 0)),
        Component.text("Now you are a spectator"),
        Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))));
    var loc = p.getLocation();
    p.getInventory().forEach(x -> {
      if (x != null) {
        gameWorld.getCBWorld().dropItemNaturally(loc, x);
      }
    });
    p.getInventory().clear();
    RemovePlayer(p, message);
    AddSpectator(p); // 加入观战列表
  }

  @EventHandler
  public void OnPlayerQuit(PlayerQuitEvent e) {
    e.quitMessage(null);
    if (isGameEnd) {
      return;
    }
    var p = e.getPlayer();
    var name = p.displayName();
    if (players.contains(p)) {
      EliminatePlayer(p, name.append(Component.text(" left the game".formatted(ChatColor.AQUA))));
    } else if (spectators.contains(p)) {
      spectators.remove(p);
    } else {
      throw new RuntimeException("bad case ...");
    }
  }

  // 添加观战
  public void AddSpectator(@Nonnull Player p) {
    spectators.add(p);
    InitSpectator(p);
  }

  @EventHandler
  public void OnPlayerJoin(PlayerJoinEvent e) {
    e.joinMessage(null);
    var p = Objects.requireNonNull(e.getPlayer());
    if (Config.bungee_enabled) {
      BungeeHelper.BringToLobby(p);
    } else {
      Bukkit.getScheduler().runTask(Main.GetInstance(), () -> {
        p.kick(Component.text("Game is already running"));
      });
    }
  }

  @EventHandler
  public void OnBlockBreak(BlockBreakEvent e) {
    var p = e.getPlayer();
    if (p == null)
      return;
    if (!players.contains(p))
      return;
    var b = e.getBlock();
    if (Config.game_feature_randomDrop && randomMap.containsKey(b.getType())) {
      b.setType(randomMap.get(b.getType()));
    }
    if (AutoSmelt.TrySmelt(p, Objects.requireNonNull(b))) {
      return;
    } else
      b.breakNaturally();
  }

  @EventHandler
  public void OnBlockPlace(BlockPlaceEvent e) {
    var p = e.getPlayer();
    if (p == null)
      return;
    if (!players.contains(p))
      return;
    if (!isFinalPvpPeriod)
      return;
    p.sendMessage("Cannot place block during final pvp period.");
    e.setCancelled(true);
  }

  @EventHandler
  public void OnPlayerDeath(PlayerDeathEvent e) {
    var player = Objects.requireNonNull(e.getPlayer());
    if (!players.contains(player))
      return;
    EliminatePlayer(player, e.deathMessage());
    e.setCancelled(true);
  }

  @EventHandler
  public void OnEntityDamage(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player)) {
      return;
    }
    // 游戏结束后玩家无法受到伤害
    if (isGameEnd) {
      e.setDamage(0);
      e.setCancelled(true);
      return;
    }
  }

  @EventHandler
  public void OnPlayerDamage(EntityDamageByEntityEvent e) {
    if (!Config.game_feature_comboFly) {
      return;
    }
    if (!(e.getDamager() instanceof Player)) {
      return;
    }
    var player = (Player) e.getEntity();
    Bukkit.getScheduler().runTask(Main.GetInstance(), () -> {
      player.setNoDamageTicks(2);
      player.setLastDamage(0);
    });
  }

  // 交换倒计时
  public final class SwapCountdown extends BukkitRunnable {
    private final Random random;
    private int seconds, maxSeconds;

    SwapCountdown() {
      random = new Random();
      seconds = maxSeconds = Config.game_protectTime;
    }

    // 随机确定一个传送时间
    private void RandomSeconds() {
      var minTime = Config.game_swap_time_min;
      var maxTime = Config.game_swap_time_max;
      // 生成 [minTime, maxTime) 范围内均匀分布的随机数
      seconds = maxSeconds = random.nextInt(maxTime - minTime) + minTime;
    }

    @Override
    public void run() {
      --seconds;
      if (swapCountDownBar != null) {
        swapCountDownBar
            .setTitle((isProtectionPeriod ? "PVP will be available in %ds" : "Will swap in %ds")
                .formatted(seconds));
        swapCountDownBar.setProgress((double) seconds / maxSeconds);
      }
      // 在交换前播放的歌曲
      final var SWAP_SONG = Config.game_swap_song;
      if (SWAP_SONG != null) {
        // 歌曲长度 (秒)
        final var SONG_TIME = Math
            .round((float) Config.game_swap_song.getLength() / Config.game_swap_song.getSpeed());
        if (seconds == SONG_TIME && !isProtectionPeriod) {
          RadioSongPlayer rsp = new RadioSongPlayer(SWAP_SONG);
          players.forEach(rsp::addPlayer);
          spectators.forEach(rsp::addPlayer);
          rsp.setPlaying(true);
        }
      }
      if (seconds <= 0) {
        RandomSeconds();
        if (isProtectionPeriod) {
          // 结束保护期
          isProtectionPeriod = false;
          gameWorld.setPVPMode(true);
          players.forEach(x -> x.sendMessage(ChatColor.RED + "PVP is available now"));
          spectators.forEach(x -> x.sendMessage(ChatColor.RED + "PVP is available now"));
        } else {
          var ps = new ArrayList<>(players);
          // 对玩家列表进行洗牌已保证传送随机
          Collections.shuffle(ps, random);
          var locations = ps.stream().map(Entity::getLocation).collect(Collectors.toList());
          for (int i = 0; i < ps.size(); i++) {
            var p = ps.get(i);
            var fromLoc = p.getLocation();
            // 玩家将要传送到的座标
            var toLoc = locations.get(i == ps.size() - 1 ? 0 : i + 1);
            p.sendMessage(
                "%sYour position: %s%s%d %d %d%s%s -> %s%s%d %d %d".formatted(ChatColor.AQUA,
                    ChatColor.YELLOW, ChatColor.BOLD, fromLoc.getBlockX(), fromLoc.getBlockY(),
                    fromLoc.getBlockZ(), ChatColor.RESET, ChatColor.AQUA, ChatColor.YELLOW,
                    ChatColor.BOLD, toLoc.getBlockX(), toLoc.getBlockY(), toLoc.getBlockZ()));
            p.teleport(toLoc);
            p.playSound(toLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 1F, 1F);
          }
        }
      }
    }
  }

  // 缩圈倒计时
  public final class ShrinkCountDown extends BukkitRunnable {
    private int seconds, maxSeconds;

    ShrinkCountDown() {
      seconds = maxSeconds = Config.game_area_shrinkCountDown;
    }

    @Override
    public void run() {
      if (seconds > 0) {
        --seconds;
        if (shrinkCountDownBar != null) {
          shrinkCountDownBar
              .setTitle((isBeginShrink ? "Will shrink in %ds" : "Current border radius: %d")
                  .formatted(seconds));
          shrinkCountDownBar.setProgress((double) seconds / maxSeconds);
        }
        if (!isBeginShrink) {
          if (seconds < 5)
            seconds = 5;
          WorldBorder wb = gameWorld.getCBWorld().getWorldBorder();
          wb.setCenter(gameWorld.getSpawnLocation());
          wb.setSize(seconds * 2 + 1); // 边界边长 = 半径 * 2 + 1
          wb.setDamageAmount(0);
          if (seconds == Config.game_swapTopTime) {
            players.forEach(p -> {
              var pos = p.getLocation();
              pos.setY(gameWorld.getCBWorld().getHighestBlockAt(pos).getLocation().getY() + 1);
              p.teleport(pos);
            });
            isFinalPvpPeriod = true;
          } else {
            players.forEach(p -> {
              if (!wb.isInside(p.getLocation())) {
                p.damage(Config.game_area_borderDamage);
                p.teleport(RandomSpawn.Get(gameWorld, seconds));
              }
            });
          }
        }
        var wLoc = gameWorld.getSpawnLocation();
        var r = isBeginShrink ? Config.game_area_radius : seconds;
        players.forEach(x -> {
          var loc = x.getLocation();
          var dis = r - Double.max(Math.abs(loc.x() - wLoc.x()), Math.abs(loc.z() - wLoc.z()));
          x.sendActionBar(Component.text("%sLive %s%d%s | %sXYZ %s%.1f %.1f %.1f%s | %sDis %s%.1f"
              .formatted(ChatColor.AQUA, ChatColor.YELLOW, players.size(), ChatColor.GRAY,
                  ChatColor.AQUA, ChatColor.YELLOW, loc.x(), loc.y(), loc.z(), ChatColor.GRAY,
                  ChatColor.AQUA, ChatColor.YELLOW, dis)));
        });
      } else if (isBeginShrink) {
        isBeginShrink = false;
        seconds = maxSeconds = Config.game_area_radius;
        players.forEach(x -> {
          x.sendMessage(ChatColor.RED + "Area begin to shrink");
          x.playSound(x.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 1F,
              1F);
          x.addPotionEffect(
              new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
        });
        spectators.forEach(x -> {
          x.sendMessage(ChatColor.RED + "Area begin to shrink");
          x.playSound(x.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 1F,
              1F);
        });
      }
    }
  }
}
