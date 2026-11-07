package net.mizukilab.pit.util.music;

import cn.charlotte.pit.ThePit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SongPlayer {

    protected Song song;
    protected boolean playing = false;
    protected short tick = -1;
    protected ArrayList<String> playerList = new ArrayList<String>();
    protected boolean loop;
    protected boolean autoDestroy = false;
    protected boolean destroyed = false;
    protected BukkitTask playerThread;
    protected byte fadeTarget = 100;
    protected byte volume = 100;
    protected byte fadeStart = volume;
    protected int fadeDuration = 60;
    protected int fadeDone = 0;
    protected FadeType fadeType = FadeType.FADE_LINEAR;

    public SongPlayer(Song song) {
        this.song = song;
        createThread();
    }

    public FadeType getFadeType() {
        return fadeType;
    }

    public void setFadeType(FadeType fadeType) {
        this.fadeType = fadeType;
    }

    public byte getFadeTarget() {
        return fadeTarget;
    }

    public void setFadeTarget(byte fadeTarget) {
        this.fadeTarget = fadeTarget;
    }

    public byte getFadeStart() {
        return fadeStart;
    }

    public void setFadeStart(byte fadeStart) {
        this.fadeStart = fadeStart;
    }

    public int getFadeDuration() {
        return fadeDuration;
    }

    public void setFadeDuration(int fadeDuration) {
        this.fadeDuration = fadeDuration;
    }

    public int getFadeDone() {
        return fadeDone;
    }

    public void setFadeDone(int fadeDone) {
        this.fadeDone = fadeDone;
    }

    protected void calculateFade() {
        if (fadeDone == fadeDuration) {
            return; // no fade today
        }
        double targetVolume = Interpolator.interpLinear(new double[]{0, fadeStart, fadeDuration, fadeTarget}, fadeDone);
        setVolume((byte) targetVolume);
        fadeDone++;
    }

    protected void createThread() {
        // 1.21：改为主线程定时任务。playSound 与 callEvent 在 Paper 上都必须同步执行，
        // 原先 runTaskAsynchronously + Thread.sleep 会导致音乐静默失效。
        playerThread = Bukkit.getScheduler().runTaskTimer(ThePit.getInstance(), () -> {
            if (destroyed || ThePit.getInstance() == null || !ThePit.getInstance().isEnabled()) {
                return;
            }
            synchronized (SongPlayer.this) {
                if (!playing) {
                    return;
                }
                calculateFade();
                tick++;
                if (tick > song.getLength()) {
                    if (loop) {
                        tick = 0;
                        return;
                    }
                    playing = false;
                    tick = -1;
                    Bukkit.getPluginManager().callEvent(new SongEndEvent(SongPlayer.this));
                    if (autoDestroy) {
                        destroy();
                        return;
                    }
                }
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    playTick(onlinePlayer, tick);
                }
            }
        }, 0L, Math.max(1L, song.getDelay()));
    }

    public List<String> getPlayerList() {
        return Collections.unmodifiableList(playerList);
    }

    public void addPlayer(Player p) {
        synchronized (this) {
            if (!playerList.contains(p.getName())) {
                playerList.add(p.getName());
                ArrayList<SongPlayer> songs = NoteBlockPlayerMain.plugin.playingSongs
                        .get(p.getName());
                if (songs == null) {
                    songs = new ArrayList<>();
                }
                songs.add(this);
                NoteBlockPlayerMain.plugin.playingSongs.put(p.getName(), songs);
            }
        }
    }

    public boolean isLoop() {
        synchronized (this) {
            return this.loop;
        }
    }

    public void setLoop(boolean loop) {
        synchronized (this) {
            this.loop = loop;
        }
    }

    public boolean getAutoDestroy() {
        synchronized (this) {
            return autoDestroy;
        }
    }

    public void setAutoDestroy(boolean value) {
        synchronized (this) {
            autoDestroy = value;
        }
    }

    public abstract void playTick(Player p, int tick);

    public void destroy() {
        synchronized (this) {
            SongDestroyingEvent event = new SongDestroyingEvent(this);
            Bukkit.getPluginManager().callEvent(event);
            //Bukkit.getScheduler().cancelTask(threadId);
            if (event.isCancelled()) {
                return;
            }
            destroyed = true;
            playing = false;
            setTick((short) -1);
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
        if (!playing) {
            SongStoppedEvent event = new SongStoppedEvent(this);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    public short getTick() {
        return tick;
    }

    public void setTick(short tick) {
        this.tick = tick;
    }

    public void removePlayer(Player p) {
        synchronized (this) {
            playerList.remove(p.getName());
            if (NoteBlockPlayerMain.plugin.playingSongs.get(p.getName()) == null) {
                return;
            }
            ArrayList<SongPlayer> songs = new ArrayList<SongPlayer>(
                    NoteBlockPlayerMain.plugin.playingSongs.get(p.getName()));
            songs.remove(this);
            NoteBlockPlayerMain.plugin.playingSongs.put(p.getName(), songs);
            if (playerList.isEmpty() && autoDestroy) {
                SongEndEvent event = new SongEndEvent(this);
                Bukkit.getPluginManager().callEvent(event);
                destroy();
            }
        }
    }

    public byte getVolume() {
        return volume;
    }

    public void setVolume(byte volume) {
        this.volume = volume;
    }

    public Song getSong() {
        return song;
    }
}
