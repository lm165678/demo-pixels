package water;

import com.jme3.system.AppSettings;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WaterSim3 extends WaterSimTemplate {

    public static void main(String... args) {

        WaterSim3 main = new WaterSim3();

        AppSettings appSettings = new AppSettings(true);
        appSettings.setResolution(1280, 720);
        appSettings.setAudioRenderer(null);

        main.setSettings(appSettings);
        main.setShowSettings(false);
        main.setPauseOnLostFocus(false);
        main.start();
    }

    private final float maxMass = 1.0f;
    private final float minFlow = 0.001f;
    private final Set<GridPos> changes = new HashSet<>();

    @Override
    public void addChange(int x, int y) {
        changes.add(new GridPos(x,y));
    }

    private void applyWaterChanges() {
        Iterator<GridPos> iterator = changes.iterator();

        if (iterator.hasNext()) {

            GridPos gridPos = iterator.next();
            iterator.remove();

            doWater(gridPos.x, gridPos.y, mass[gridPos.x][gridPos.y]);

        }

    }

    private final boolean sides = true;

    private void setBlock(int x, int y, float val) {
        mass[x][y] = val;
        blocks[x][y] = (val > 0) ? WATER : AIR;
        draw_block(x, y, val);
    }

    private void doWater(int x, int y, float val) {

        setBlock(x, y, val);

        if (val > 0) {

            if (blocks[x][y - 1] != GROUND) {

                float block_mass = mass[x][y - 1];

                if (block_mass < maxMass) {

                    // the max this block can take.
                    float maxFlow = maxMass - block_mass;

                    // transfer the smallest amount,
                    float flow = Math.min(val, maxFlow);

                    if (flow > 0) {

                        setBlock(x, y, mass[x][y] - flow);
                        setBlock(x, y - 1, mass[x][y] + flow);

                        addChange(x,y);
                        addChange(x, y - 1);

                        val -= flow;
                    }
                }

            }

            if (val <= 0) {
                return;
            }

            if (!sides) {
                return;
            }

            for (int side = x - 1; side <= x + 1; side++) {

                if (x == side) continue;

                if (blocks[side][y] != GROUND) {

                    if (mass[side][y] < val) {

                        // the most this block can take.
                        float maxFlow = (maxMass - mass[side][y]) * 0.5f;

                        // subtract the remaining mass from the block on the left to get the difference.
                        // give the block half of the difference or maxFlow. Whichever is lowest.
                        float diff = (val - mass[side][y]) * 0.5f;

                        float flow = Math.min(maxFlow, diff);

                        if (flow > minFlow) {
                            // addChange(x, y, flow, false);
                            // addChange(x - 1, y, flow, true);

                            setBlock(x, y, mass[x][y] - flow);
                            setBlock(side, y, mass[side][y] + flow);

                            addChange(x,y);
                            addChange(side, y);

                            if (side - 1 > 0 && blocks[side - 1][y] != GROUND) {
                                addChange(side - 1, y);
                            }
                            if (side + 1 < size - 1 && blocks[side + 1][y] != GROUND) {
                                addChange(side + 1, y);
                            }

                            val -= flow;

                            // System.out.println(" left: " + flow);
                        }

                    }

                }
            }

            // Left


        }

    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        applyWaterChanges();

        changesLabel.setText("" + changes.size());
    }

}
