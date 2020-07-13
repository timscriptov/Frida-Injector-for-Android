package com.mcal.fridainjectorpe.data;

public final class ScreenMode {
    public static Mode getCurrentMode() {
        return Preferences.isFullscreenMode() ? Mode.FULLSCREEN : Mode.DEFAULT;
    }

    public enum Mode {
        DEFAULT, FULLSCREEN
    }
}