package com.jayfella.pixels.grid.settings;

import com.jayfella.pixels.core.CellSize;

public interface GridSettingsListener {

    void viewDistanceChanged(int oldValue, int newValue);
    void cellSizeChanged(CellSize oldSize, CellSize newSize);

}
