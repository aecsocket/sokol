package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.minecommons.core.scheduler.ThreadScheduler;
import com.gitlab.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.Executors;

public class SokolSchedulers {
    private final SokolPlugin plugin;
    private final PaperScheduler paperScheduler;
    private final ThreadScheduler threadScheduler;

    private final Map<UUID, PlayerData> playerData = new HashMap<>();

    public SokolSchedulers(SokolPlugin plugin) {
        this.plugin = plugin;
        paperScheduler = new PaperScheduler(plugin);
        threadScheduler = new ThreadScheduler(Executors.newSingleThreadExecutor());
    }

    public SokolPlugin plugin() { return plugin; }
    public PaperScheduler paperScheduler() { return paperScheduler; }
    public ThreadScheduler threadScheduler() { return threadScheduler; }

    public PlayerData playerData(Player player) { return playerData.computeIfAbsent(player.getUniqueId(), u -> new PlayerData(plugin, player)); }

    void remove(Player player) {
        playerData.remove(player.getUniqueId());
    }

    void setup() {
        paperScheduler.run(Task.repeating(this::paperTick, Ticks.MSPT, 0));
        threadScheduler.run(Task.repeating(this::threadTick, 10, 0));
    }

    void stop() {
        paperScheduler.cancel();
        threadScheduler.cancel();
    }

    public void paperTick(TaskContext ctx) {
        for (PlayerData data : playerData.values()) {
            ctx.run(Task.single(data::paperTick, 0));
        }
    }

    public void threadTick(TaskContext ctx) {
        for (PlayerData data : playerData.values()) {
            ctx.run(Task.single(data::threadTick, 0));
        }
    }
}
