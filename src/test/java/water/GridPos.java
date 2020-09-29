package water;

import java.util.Objects;

public final class GridPos {
    protected final int x, y;

    public GridPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridPos gridPos = (GridPos) o;
        return x == gridPos.x &&
                y == gridPos.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
