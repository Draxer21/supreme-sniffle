package com.draxer.draxwaypoints.client;

/** Un punto de ruta. {@code dimension} es el id de la dimensión ("minecraft:overworld"...). */
public final class Waypoint {
    public static final int[] COLORS = {0xFF5555, 0x55FF55, 0x5599FF, 0xFFD24A, 0xFF66FF, 0x55FFFF, 0xFFFFFF, 0xFF9933};

    public String name;
    public int x;
    public int y;
    public int z;
    public String dimension;
    public int color;
    public boolean visible = true;

    public Waypoint(String name, int x, int y, int z, String dimension, int color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.color = color;
    }

    public void nextColor() {
        int index = 0;
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == this.color) index = i;
        this.color = COLORS[(index + 1) % COLORS.length];
    }
}
