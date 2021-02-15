package leafor.swap.listenters;

import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import leafor.swap.Main;
import leafor.swap.config.Config;
import leafor.swap.utils.AutoSmelt;
import leafor.swap.utils.RandomSpawn;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public final class RunListener extends GameListener {
  private final Set<Player> players; // 游戏玩家
  private final Set<Player> spectators; // 观战玩家
  private final World gameWorld; // 游戏世界
  private final SwapCountdown countdown = new SwapCountdown();
  private boolean isGameEnd = false;
  private boolean isProtectionPeriod = true;
  private BossBar countdownBar;

  public RunListener(
    @Nonnull Set<Player> players,
    @Nonnull Set<Player> spectators,
    @Nonnull World gameWorld) {
    Bukkit.getPluginManager().registerEvents(this, Main.GetInstance());
    this.players = players;
    this.spectators = spectators;
    this.gameWorld = gameWorld;
    // 开始时间变化
    gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
    countdownBar = Bukkit.createBossBar(
      "Protection period", BarColor.RED, BarStyle.SOLID);
    countdownBar.setProgress(1.0);
    final var MAX_HEALTH = Config.game_player_health;
    // 初始化每个玩家
    players.forEach(p -> {
      p.teleport(RandomSpawn.Get(gameWorld));
      p.setGameMode(GameMode.SURVIVAL);
      p.setFireTicks(0);
      p.setFoodLevel(20);
      p.setSaturation(20);
      Objects.requireNonNull(p.getAttribute(Attribute.GENERIC_MAX_HEALTH))
        .setBaseValue(MAX_HEALTH);
      p.setHealthScaled(false);
      p.setHealth(MAX_HEALTH);
      p.setExp(0);
      p.setLevel(0);
      p.getActivePotionEffects().forEach(
        e -> p.removePotionEffect(e.getType()));
      p.addPotionEffect(new PotionEffect(
        PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 1));
      var inv = p.getInventory();
      inv.clear();
      inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
      p.sendTitle(ChatColor.RED + "Game start", "",
                  10, 70, 20);
      p.playSound(
        p.getLocation(),
        Sound.ENTITY_ENDER_DRAGON_GROWL,
        SoundCategory.MASTER,
        1F,
        1F);
      countdownBar.addPlayer(p);
    });
    // 将观战玩家 tp 到游戏场地
    spectators.forEach(p -> {
      InitSpectator(p);
      p.teleport(gameWorld.getSpawnLocation());
      p.sendTitle(ChatColor.RED + "Game start", "",
                  10, 70, 20);
      p.playSound(
        p.getLocation(),
        Sound.ENTITY_ENDER_DRAGON_GROWL,
        SoundCategory.MASTER,
        1F,
        1F);
      countdownBar.addPlayer(p);
    });
    countdown.runTaskTimer(Main.GetInstance(), 0, 20);
  }

  private void InitSpectator(@Nonnull Player p) {
    p.getActivePotionEffects().forEach(e -> {
      p.setGameMode(GameMode.SPECTATOR);
      p.removePotionEffect(e.getType());
    });
    p.getInventory().clear();
    if (countdownBar != null) {
      countdownBar.addPlayer(p);
    }
  }

  // 结束游戏
  public void EndGame() {
    if (!isGameEnd) {
      players.forEach(x -> x.playSound(
        x.getLocation(),
        Sound.ENTITY_WITHER_DEATH,
        SoundCategory.MASTER,
        1F,
        1F));
      spectators.forEach(x -> x.playSound(
        x.getLocation(),
        Sound.ENTITY_WITHER_DEATH,
        SoundCategory.MASTER,
        1F,
        1F));
      isGameEnd = true;
      countdown.cancel();
      countdownBar.removeAll();
      countdownBar = null;
      System.out.println("[SwapGame] Game over");
      HandlerList.unregisterAll(this);
      Bukkit.getScheduler().runTaskLater(
        Main.GetInstance(),
        () -> GameListener.SetListener(
          new CleanListener(gameWorld)), 10 * 20);
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
          p.sendTitle(
            ChatColor.RED + "Congratulations!",
            "You won the game",
            10, 70, 20);
        } else {
          p.sendTitle(
            ChatColor.RED + "Game over!",
            "%s is the winner".formatted(winner.getDisplayName()),
            10, 70, 20);
        }
      });
      EndGame();
    }
    return players.size() <= 1;
  }

  private void RemovePlayer(@Nonnull Player p) {
    assert players.contains(p);
    players.remove(p);
    if (countdownBar != null) {
      countdownBar.removePlayer(p);
    }
    var size = players.size();
    players.forEach(x -> x.sendMessage("%s%s%d players left".formatted(
      ChatColor.RED, ChatColor.BOLD, size)));
    spectators.forEach(x -> x.sendMessage("%s%s%d players left".formatted(
      ChatColor.RED, ChatColor.BOLD, size)));
    CheckWinner();
  }

  // 淘汰玩家
  public void EliminatePlayer(@Nonnull Player p) {
    assert players.contains(p);
    gameWorld.strikeLightningEffect(p.getLocation());
    p.sendTitle(
      ChatColor.RED + "You lost the game!",
      "Now you are a spectator",
      10, 70, 20);
    var loc = p.getLocation();
    p.getInventory().forEach(x -> {
      if (x != null) {
        gameWorld.dropItemNaturally(loc, x);
      }
    });
    p.getInventory().clear();
    RemovePlayer(p);
    AddSpectator(p); // 加入观战列表
  }

  @EventHandler
  public void OnPlayerQuit(PlayerQuitEvent e) {
    e.setQuitMessage("");
    if (isGameEnd) {
      return;
    }
    var p = e.getPlayer();
    var name = p.getDisplayName();
    if (players.contains(p)) {
      EliminatePlayer(p);
      players.forEach(x -> x.sendMessage(
        "%s%s exited the game".formatted(name, ChatColor.AQUA)));
      spectators.forEach(x -> x.sendMessage(
        "%s%s exited the game".formatted(name, ChatColor.AQUA)));
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
    e.setJoinMessage("");
    var p = e.getPlayer();
    if (p.hasPermission("leafor.swap.watch")) {
      AddSpectator(p);
      p.teleport(gameWorld.getSpawnLocation());
    } else {
      p.kickPlayer("Game is already running!");
    }
  }

  @EventHandler
  public void OnBlockBreak(BlockBreakEvent e) {
    var p = e.getPlayer();
    if (players.contains(p) && AutoSmelt.TrySmelt(p, e.getBlock())) {
      e.setCancelled(false);
    }
  }

  @EventHandler
  public void OnPlayerDeath(PlayerDeathEvent e) {
    EliminatePlayer(e.getEntity());
    e.setCancelled(true);
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
      if (countdownBar != null) {
        countdownBar.setTitle(
          (isProtectionPeriod ? "PVP will be available in %ds" :
            "Will swap in %ds").formatted(seconds));
        countdownBar.setProgress((double) seconds / maxSeconds);
      }
      // 在交换前播放的歌曲
      final var SWAP_SONG = Config.game_swap_song;
      // 歌曲长度 (秒)
      final var SONG_TIME =
        Math.round((float) Config.game_swap_song.getLength() /
                     Config.game_swap_song.getSpeed());
      if (seconds == SONG_TIME && !isProtectionPeriod) {
        RadioSongPlayer rsp = new RadioSongPlayer(SWAP_SONG);
        players.forEach(rsp::addPlayer);
        spectators.forEach(rsp::addPlayer);
        rsp.setPlaying(true);
      }
      if (seconds <= 0) {
        RandomSeconds();
        if (isProtectionPeriod) {
          // 结束保护期
          isProtectionPeriod = false;
          gameWorld.setPVP(true);
          players.forEach(x -> x.sendMessage(
            ChatColor.RED + "PVP is available now"));
          spectators.forEach(x -> x.sendMessage(
            ChatColor.RED + "PVP is available now"));
        } else {
          var ps = new ArrayList<>(players);
          // 对玩家列表进行洗牌已保证传送随机
          Collections.shuffle(ps, random);
          var locations =
            ps.stream().map(Entity::getLocation).collect(Collectors.toList());
          for (int i = 0; i < ps.size(); i++) {
            var p = ps.get(i);
            var fromLoc = p.getLocation();
            // 玩家将要传送到的座标
            var toLoc = locations.get(i == ps.size() - 1 ? 0 : i + 1);
            p.sendMessage(
              "%sYour position: %s%s%d %d %d%s%s -> %s%s%d %d %d".formatted(
                ChatColor.AQUA,
                ChatColor.YELLOW,
                ChatColor.BOLD,
                fromLoc.getBlockX(),
                fromLoc.getBlockY(),
                fromLoc.getBlockZ(),
                ChatColor.RESET,
                ChatColor.AQUA,
                ChatColor.YELLOW,
                ChatColor.BOLD,
                toLoc.getBlockX(),
                toLoc.getBlockY(),
                toLoc.getBlockZ()
              ));
            p.teleport(toLoc);
            p.playSound(
              toLoc,
              Sound.ENTITY_ENDERMAN_TELEPORT,
              SoundCategory.MASTER,
              1F,
              1F);
          }
        }
      }
    }
  }
}
