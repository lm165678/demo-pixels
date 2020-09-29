package com.jayfella.pixels.core;

public class WorldConstants {
    
    public static final int CELL_SIZE = 16;
    public static final int CELL_COUNT_Y = 16;
    
    // 2 = 1, 4 = 2, 8 = 3, 16 = 4, 32 = 5, etc.
    public static final int GRID_BITSHIFT = 4; // size = 16

    public static final int MAX_HEIGHT = CELL_SIZE * CELL_COUNT_Y;
    
}
