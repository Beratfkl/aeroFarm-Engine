import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * FarmPanel
 * Renders the floating sky island, 3D isometric tiles, drone, and particle effects.
 * Manages the main 60 FPS animation timer and script interaction callbacks.
 */
public class FarmPanel extends JPanel implements MiniJavaInterpreter.DroneInterface {
    public static final int DEFAULT_GRID_SIZE = 3;
    public static final int TILE_WIDTH = 96;   // 2:1 isometric tile width
    public static final int TILE_HEIGHT = 48;  // 2:1 isometric tile height

    private int gridSize = DEFAULT_GRID_SIZE;
    private Tile[][] grid;
    private final Drone drone;
    private final ParticleSystem particleSystem;

    // Drifting background clouds
    private static class Cloud {
        float x, y, speed, scale, alpha;
        Cloud(float x, float y, float speed, float scale, float alpha) {
            this.x = x; this.y = y; this.speed = speed; this.scale = scale; this.alpha = alpha;
        }
    }
    private final Cloud[] clouds = new Cloud[6];
    private final Random rand = new Random();

    // Harvest counters
    private int hayHarvested = 0;
    private int wheatHarvested = 0;
    private int carrotHarvested = 0;

    // Sky background gradient colors
    private final Color skyTop = new Color(145, 165, 180);
    private final Color skyBottom = new Color(175, 195, 210);

    public FarmPanel() {
        setOpaque(true);
        drone = new Drone(0, 0);
        particleSystem = new ParticleSystem();
        initGrid(gridSize);
        initClouds();

        // 60 FPS render loop (~16ms)
        Timer renderTimer = new Timer(16, e -> {
            updateWorld(0.016);
            repaint();
        });
        renderTimer.start();
    }

    // Resize grid (1x1, 3x3, or 4x4)
    public synchronized void setGridSize(int newSize) {
        this.gridSize = Math.max(1, Math.min(6, newSize));
        initGrid(this.gridSize);
        drone.setPosition(0, 0);
        drone.setDirection(Drone.EAST);
    }

    public synchronized void resetGrid() {
        initGrid(this.gridSize);
        drone.setPosition(0, 0);
        drone.setDirection(Drone.EAST);
    }

    private void initGrid(int size) {
        grid = new Tile[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = new Tile();
            }
        }
    }

    private void initClouds() {
        for (int i = 0; i < clouds.length; i++) {
            clouds[i] = new Cloud(
                    rand.nextInt(1200),
                    30 + rand.nextInt(300),
                    0.2f + rand.nextFloat() * 0.4f,
                    0.8f + rand.nextFloat() * 0.8f,
                    0.25f + rand.nextFloat() * 0.35f
            );
        }
    }

    // Per-frame physics and state updates
    private void updateWorld(double dt) {
        // Drift clouds
        for (Cloud c : clouds) {
            c.x += c.speed;
            if (c.x > getWidth() + 200) {
                c.x = -200;
                c.y = 30 + rand.nextInt(Math.max(100, getHeight() / 2));
            }
        }

        // Update soil moisture and crop growth
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                grid[r][c].updateGrowth(dt);
            }
        }

        // Update drone and particles
        drone.update(dt);
        particleSystem.update();

        // Propeller dust effect
        int originX = getWidth() / 2;
        int originY = (getHeight() - (gridSize * TILE_HEIGHT)) / 2;
        int tileX = originX + (drone.getX() - drone.getY()) * (TILE_WIDTH / 2);
        int tileY = originY + (drone.getX() + drone.getY()) * (TILE_HEIGHT / 2);
        particleSystem.spawnPropellerDust(tileX, tileY - 10);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // Anti-aliasing hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Sky background gradient
        GradientPaint skyPaint = new GradientPaint(0, 0, skyTop, 0, h, skyBottom);
        g2d.setPaint(skyPaint);
        g2d.fillRect(0, 0, w, h);

        // 2. Clouds
        drawClouds(g2d);

        // 3. Center origin for the island
        int originX = w / 2;
        int originY = (h - (gridSize * TILE_HEIGHT)) / 2 + 10;

        // Ground shadow below the floating island
        int shadowW = (int) (gridSize * TILE_WIDTH * 1.05);
        int shadowH = (int) (gridSize * TILE_HEIGHT * 0.95);
        g2d.setColor(new Color(0, 0, 0, 28));
        g2d.fillOval(originX - shadowW / 2, originY + (gridSize * TILE_HEIGHT) / 2 + 45, shadowW, shadowH);

        // 4. Render isometric tiles back-to-front (Painter's Algorithm)
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                int isoX = originX + (c - r) * (TILE_WIDTH / 2);
                int isoY = originY + (r + c) * (TILE_HEIGHT / 2);
                grid[r][c].draw(g2d, isoX, isoY, TILE_WIDTH, TILE_HEIGHT);
            }
        }

        // 5. Harvest particle effects
        particleSystem.draw(g2d);

        // 6. Drone entity
        drone.draw(g2d, originX, originY, TILE_WIDTH, TILE_HEIGHT);
    }

    private void drawClouds(Graphics2D g2d) {
        for (Cloud c : clouds) {
            g2d.setColor(new Color(255, 255, 255, (int) (c.alpha * 255)));
            int cw = (int) (140 * c.scale);
            int ch = (int) (45 * c.scale);
            g2d.fillRoundRect((int) c.x, (int) c.y, cw, ch, ch, ch);
            g2d.fillOval((int) c.x + (int)(20 * c.scale), (int) c.y - (int)(15 * c.scale), (int)(50 * c.scale), (int)(50 * c.scale));
            g2d.fillOval((int) c.x + (int)(55 * c.scale), (int) c.y - (int)(25 * c.scale), (int)(60 * c.scale), (int)(60 * c.scale));
        }
    }

    // =========================================================================
    // DroneInterface Implementation (Thread-safe API calls for script engine)
    // =========================================================================
    @Override
    public synchronized boolean harvest() {
        Tile t = grid[drone.getY()][drone.getX()];
        String harvested = t.harvest();
        if (harvested != null) {
            drone.triggerHarvestAnimation();
            int originX = getWidth() / 2;
            int originY = (getHeight() - (gridSize * TILE_HEIGHT)) / 2 + 10;
            int isoX = originX + (drone.getX() - drone.getY()) * (TILE_WIDTH / 2);
            int isoY = originY + (drone.getX() + drone.getY()) * (TILE_HEIGHT / 2);

            boolean isWheat = harvested.equalsIgnoreCase("WHEAT");
            if (harvested.equalsIgnoreCase("GRASS")) hayHarvested++;
            else if (harvested.equalsIgnoreCase("WHEAT")) wheatHarvested++;
            else if (harvested.equalsIgnoreCase("CARROT")) carrotHarvested++;

            particleSystem.spawnHarvestParticles(isoX, isoY, isWheat);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean canHarvest() {
        Tile t = grid[drone.getY()][drone.getX()];
        return t.canHarvest();
    }

    @Override
    public synchronized boolean move() {
        return drone.move(gridSize);
    }

    @Override
    public synchronized void turnLeft() {
        drone.turnLeft();
    }

    @Override
    public synchronized void turnRight() {
        drone.turnRight();
    }

    @Override
    public synchronized boolean plant(String crop) {
        Tile t = grid[drone.getY()][drone.getX()];
        return t.plant(crop);
    }

    @Override
    public synchronized void water() {
        Tile t = grid[drone.getY()][drone.getX()];
        t.water();
    }

    @Override
    public synchronized void till() {
        Tile t = grid[drone.getY()][drone.getX()];
        t.till();
    }

    @Override
    public synchronized int getPosX() {
        return drone.getX();
    }

    @Override
    public synchronized int getPosY() {
        return drone.getY();
    }

    @Override
    public synchronized int getWorldSize() {
        return gridSize;
    }

    @Override
    public synchronized String getCrop() {
        return grid[drone.getY()][drone.getX()].getCrop();
    }

    @Override
    public synchronized boolean isTilled() {
        return grid[drone.getY()][drone.getX()].isTilled();
    }

    @Override
    public synchronized boolean isWatered() {
        return grid[drone.getY()][drone.getX()].isWatered();
    }

    @Override
    public synchronized void doAFlip() {
        drone.triggerFlip();
    }

    // Harvest score getters
    public int getHayHarvested() { return hayHarvested; }
    public int getWheatHarvested() { return wheatHarvested; }
    public int getCarrotHarvested() { return carrotHarvested; }
    public Drone getDrone() { return drone; }
    public Tile[][] getGrid() { return grid; }
}