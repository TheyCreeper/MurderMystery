package dev.theycreeper.murdermystery.game;

import org.bukkit.entity.Player;

public class MMPlayer {
    private final Player player;
    private PlayerType playerType;
    private PlayerState playerState;
    private double murderPercentage;
    private double detectivePercentage;
    public MMPlayer(Player player) {
        this.player = player;
        this.playerType = PlayerType.innocent;
        this.playerState = PlayerState.alive;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }
    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }
    public Player getPlayer() {
        return player;
    }
    public PlayerState getPlayerState() {
        return playerState;
    }
    public PlayerType getPlayerType() {
        return playerType;
    }
    public double getMurderPercentage() {
        return murderPercentage;
    }
    public void setMurderPercentage(double newPercentage) {
        this.murderPercentage = newPercentage;
    }
    public double getDetectivePercentage() {
        return detectivePercentage;
    }
    public void setDetectivePercentage(double newPercentage) {
        this.detectivePercentage = newPercentage;
    }
}
