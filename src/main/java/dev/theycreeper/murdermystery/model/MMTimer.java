package dev.theycreeper.murdermystery.model;

public class MMTimer {
    int minutes, seconds;

    public MMTimer(int minutes, int seconds) {
        while (seconds > 60) {
            seconds -= 60;
            ++minutes;
        }
        this.minutes = minutes;
        this.seconds = seconds;
    }

    public void tick() {
        if (seconds - 1 == 0) {
            seconds = 59;
            --minutes;
        }
        else {
            --seconds;
        }
    }

    @Override
    public String toString() {
        return minutes + ":" + seconds;
    }
}
