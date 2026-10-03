import java.awt.*;
import java.util.Random;

/**
 * Tile
 * Represents an individual 3D isometric terrain block on the island.
 * Manages crop growth timers, tilling, watering, and harvest validation.
 */
public class Tile {
    // 3D vertical extrusion depth (in pixels)
    public static final int BLOCK_DEPTH = 32;

    // Terrain and grass color palette
    private final Color grassTopLight = new Color(138, 185, 75);
    private final Color grassTop = new Color(118, 168, 62);
    private final Color grassSide = new Color(92, 138, 48);

    private final Color soilTop = new Color(136, 94, 66);
    private final Color soilSideLeft = new Color(102, 68, 46);
    private final Color soilSideRight = new Color(82, 54, 36);

    private final Color tilledTop = new Color(78, 48, 28);
    private final Color tilledSideLeft = new Color(58, 34, 18);
    private final Color tilledSideRight = new Color(42, 24, 12);

    private final Color wateredBlue = new Color(52, 152, 219, 90);
    private final Color goldWheatTip = new Color(255, 222, 105);
    private final Color goldWheatStem = new Color(220, 180, 65);
    private final Color carrotGreen = new Color(76, 175, 80);
    private final Color carrotOrange = new Color(255, 120, 30);

    // Tile state
    private boolean isTilled = false;
    private boolean isWatered = false;
    private String crop = "GRASS";      // Default wild grass
    private double growth = 0.0;        // 0.0 to 100.0 growth percentage
    private double waterLevel = 0.0;

    // Seed offset for random grass sway
    private final int seed;
    private static final Random randGen = new Random();

    public Tile() {
        this.seed = randGen.nextInt(1000);
        this.growth = 30 + randGen.nextInt(70); // Pre-grow some grass on spawn
    }

    // Growth simulation step called by game loop
    public void updateGrowth(double dt) {
        // Watered soil provides faster growth
        double growthRate = isWatered ? 28.0 : 16.0;

        if (crop.equals("GRASS")) {
            if (!isTilled && growth < 100.0) {
                growth = Math.min(100.0, growth + growthRate * dt);
            }
        } else if (!crop.equals("NONE")) {
            if (growth < 100.0) {
                growth = Math.min(100.0, growth + growthRate * dt);
            }
        }

        // Water evaporates over time
        if (isWatered) {
            waterLevel -= 3.0 * dt;
            if (waterLevel <= 0) {
                isWatered = false;
            }
        }
    }

    // Till / plow the soil
    public void till() {
        this.isTilled = true;
        this.crop = "NONE";
        this.growth = 0;
    }

    // Hydrate the soil
    public void water() {
        this.isWatered = true;
        this.waterLevel = 100.0;
    }

    // Plant seeds on tilled soil
    public boolean plant(String cropType) {
        if (this.isTilled && (this.crop.equals("NONE") || this.crop.equals("GRASS"))) {
            this.crop = (cropType == null || cropType.isEmpty()) ? "WHEAT" : cropType.toUpperCase();
            this.growth = 10.0;
            return true;
        }
        return false;
    }

    // Check if the current crop is mature and harvestable
    public boolean canHarvest() {
        if (crop.equals("GRASS")) {
            return growth >= 70.0;
        }
        return !crop.equals("NONE") && growth >= 95.0;
    }

    // Harvest crop and return collected crop type
    public String harvest() {
        if (canHarvest() || (!crop.equals("NONE") && growth > 25.0)) {
            String collected = this.crop;
            if (isTilled) {
                this.crop = "NONE";
                this.growth = 0;
            } else {
                // Wild grass regrows from zero
                this.crop = "GRASS";
                this.growth = 0;
            }
            return collected;
        }
        return null;
    }

    // Render 3D isometric diamond cube
    public void draw(Graphics2D g2d, int isoX, int isoY, int tileWidth, int tileHeight) {
        int hw = (tileWidth / 2) - 2;
        int hh = (tileHeight / 2) - 1;

        // Top surface diamond vertices
        int[] topX = { isoX, isoX + hw, isoX, isoX - hw };
        int[] topY = { isoY - hh, isoY, isoY + hh, isoY };

        // Left side face
        int[] leftX = { isoX - hw, isoX, isoX, isoX - hw };
        int[] leftY = { isoY, isoY + hh, isoY + hh + BLOCK_DEPTH, isoY + BLOCK_DEPTH };

        // Right side face
        int[] rightX = { isoX, isoX + hw, isoX + hw, isoX };
        int[] rightY = { isoY + hh, isoY, isoY + BLOCK_DEPTH, isoY + hh + BLOCK_DEPTH };

        // 1. Draw side faces
        Color sideLeft = isTilled ? tilledSideLeft : soilSideLeft;
        Color sideRight = isTilled ? tilledSideRight : soilSideRight;

        g2d.setColor(sideLeft);
        g2d.fillPolygon(leftX, leftY, 4);

        g2d.setColor(sideRight);
        g2d.fillPolygon(rightX, rightY, 4);

        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.drawLine(isoX, isoY + hh, isoX, isoY + hh + BLOCK_DEPTH);

        // 2. Draw top soil/grass surface
        if (isTilled) {
            g2d.setColor(tilledTop);
            g2d.fillPolygon(topX, topY, 4);
            // Plow furrow details
            g2d.setColor(new Color(45, 25, 12));
            g2d.drawLine(isoX - hw / 2, isoY - hh / 2, isoX + hw / 2, isoY + hh / 2);
            g2d.drawLine(isoX - hw / 2 + 10, isoY - hh / 2 - 5, isoX + hw / 2 + 10, isoY + hh / 2 - 5);
        } else {
            g2d.setColor(grassTop);
            g2d.fillPolygon(topX, topY, 4);

            g2d.setColor(grassTopLight);
            int[] subTopX = { isoX, isoX + hw - 6, isoX, isoX - hw + 6 };
            int[] subTopY = { isoY - hh + 4, isoY + 2, isoY + hh - 4, isoY + 2 };
            g2d.fillPolygon(subTopX, subTopY, 4);
        }

        // 3. Water sheen overlay
        if (isWatered) {
            g2d.setColor(wateredBlue);
            g2d.fillPolygon(topX, topY, 4);
        }

        // 4. Growing crops with natural wind sway
        double gFactor = Math.min(1.0, growth / 100.0);
        double sway = Math.sin((System.currentTimeMillis() + seed * 10) * 0.004) * 2.5 * gFactor;

        if (crop.equals("GRASS") && growth > 15.0) {
            drawWildGrass(g2d, isoX, isoY, hw, hh, gFactor, sway);
        } else if (crop.equals("WHEAT") && growth > 10.0) {
            drawWheatField(g2d, isoX, isoY, hw, hh, gFactor, sway);
        } else if (crop.equals("CARROT") && growth > 10.0) {
            drawCarrots(g2d, isoX, isoY, gFactor);
        }
    }

    private void drawWildGrass(Graphics2D g2d, int cx, int cy, int hw, int hh, double factor, double sway) {
        g2d.setColor(new Color(145, 205, 75));
        int height = (int) (16 * factor);

        int[][] tufts = {
                {cx - 14, cy - 2},
                {cx + 12, cy - 4},
                {cx - 4, cy + 6},
                {cx + 6, cy + 4},
                {cx, cy - 6}
        };

        for (int[] pos : tufts) {
            int tx = pos[0];
            int ty = pos[1];
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(tx, ty, (int) (tx + sway - 3), ty - height);
            g2d.drawLine(tx, ty, (int) (tx + sway), ty - height - 2);
            g2d.drawLine(tx, ty, (int) (tx + sway + 3), ty - height + 1);
        }
    }

    private void drawWheatField(Graphics2D g2d, int cx, int cy, int hw, int hh, double factor, double sway) {
        int height = (int) (22 * factor);
        Color stemC = factor > 0.8 ? goldWheatStem : new Color(130, 180, 70);
        Color tipC = factor > 0.8 ? goldWheatTip : new Color(160, 210, 90);

        int[][] stalks = {
                {cx - 16, cy - 3},
                {cx + 14, cy - 5},
                {cx - 6, cy + 5},
                {cx + 8, cy + 3},
                {cx, cy - 7},
                {cx - 8, cy - 5},
                {cx + 6, cy - 3}
        };

        for (int[] pos : stalks) {
            int sx = pos[0];
            int sy = pos[1];
            int topX = (int) (sx + sway);
            int topY = sy - height;

            g2d.setColor(stemC);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(sx, sy, topX, topY);

            // Golden wheat ear tip when mature
            if (factor > 0.4) {
                g2d.setColor(tipC);
                g2d.fillOval(topX - 3, topY - 5, 6, 8);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(topX - 1, topY - 4, 2, 2);
            }
        }
    }

    private void drawCarrots(Graphics2D g2d, int cx, int cy, double factor) {
        int s = (int) (12 * factor);
        g2d.setColor(carrotOrange);
        g2d.fillOval(cx - s / 2, cy - s / 3, s, s / 2 + 2);
        g2d.setColor(carrotGreen);
        g2d.fillOval(cx - 3, cy - s / 2 - 4, 6, 6);
    }

    // Getters
    public boolean isTilled() { return isTilled; }
    public boolean isWatered() { return isWatered; }
    public String getCrop() { return crop; }
    public double getGrowth() { return growth; }
    public void setCrop(String crop) { this.crop = crop; }
    public void setGrowth(double growth) { this.growth = growth; }
}
