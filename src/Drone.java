import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Drone
 * Main autonomous farming agent moving across the floating sky island.
 * Handles movement interpolation, propeller rotation, and isometric rendering.
 */
public class Drone {
    // Direction constants (0: North, 1: East, 2: South, 3: West)
    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;

    // Grid coordinates
    private int gridX;
    private int gridY;
    private int direction = EAST;

    // Visual interpolation coordinates (LERP)
    private double visualX;
    private double visualY;
    private double visualAngle = 0;
    private double targetAngle = 0;

    // Animation states
    private double propAngle = 0;      // Rotor spin angle
    private double hoverOffset = 0;    // Hover bobbing sine wave
    private double harvestBob = 0;     // Little bounce on harvest
    private double flipAngle = 0;      // Flip rotation
    private boolean isFlipping = false;

    // Color palette
    private final Color bodyColor = new Color(245, 175, 80);
    private final Color bodyDarkColor = new Color(215, 135, 50);
    private final Color armColor = new Color(70, 70, 75);
    private final Color rotorColor = new Color(40, 42, 45, 220);
    private final Color strawHatBrim = new Color(230, 185, 95);
    private final Color strawHatBrimDark = new Color(195, 150, 70);
    private final Color strawHatTop = new Color(245, 205, 115);
    private final Color strawHatBand = new Color(140, 45, 45);

    public Drone(int startX, int startY) {
        this.gridX = startX;
        this.gridY = startY;
        this.visualX = startX;
        this.visualY = startY;
        updateTargetAngle();
        this.visualAngle = targetAngle;
    }

    // Turn 90 degrees right
    public void turnRight() {
        this.direction = (this.direction + 1) % 4;
        updateTargetAngle();
    }

    // Turn 90 degrees left
    public void turnLeft() {
        this.direction = (this.direction + 3) % 4;
        updateTargetAngle();
    }

    private void updateTargetAngle() {
        switch (direction) {
            case NORTH: targetAngle = -Math.PI / 2; break;
            case EAST: targetAngle = 0; break;
            case SOUTH: targetAngle = Math.PI / 2; break;
            case WEST: targetAngle = Math.PI; break;
        }
    }

    // Move 1 tile forward with grid boundary check
    public boolean move(int maxGridSize) {
        int nextX = this.gridX;
        int nextY = this.gridY;

        switch (this.direction) {
            case NORTH: nextY--; break;
            case EAST: nextX++; break;
            case SOUTH: nextY++; break;
            case WEST: nextX--; break;
        }

        if (nextX >= 0 && nextX < maxGridSize && nextY >= 0 && nextY < maxGridSize) {
            this.gridX = nextX;
            this.gridY = nextY;
            return true;
        }
        return false; // Prevent falling off the island edge
    }

    public void triggerHarvestAnimation() {
        harvestBob = 8.0;
    }

    public void triggerFlip() {
        isFlipping = true;
        flipAngle = 0;
    }

    // 60 FPS update tick
    public void update(double dt) {
        // Smooth linear interpolation to target tile
        double lerpSpeed = 12.0 * dt;
        visualX += (gridX - visualX) * Math.min(1.0, lerpSpeed);
        visualY += (gridY - visualY) * Math.min(1.0, lerpSpeed);

        // Spin rotors continuously
        propAngle += 35.0 * dt;
        if (propAngle > Math.PI * 2) propAngle -= Math.PI * 2;

        // Gentle sine wave hover motion
        hoverOffset = Math.sin(System.currentTimeMillis() * 0.005) * 4.0;

        // Decay harvest bounce
        if (harvestBob > 0.01) {
            harvestBob *= Math.max(0.0, 1.0 - 10.0 * dt);
        } else {
            harvestBob = 0;
        }

        // Acrobatic flip animation
        if (isFlipping) {
            flipAngle += 15.0 * dt;
            if (flipAngle >= Math.PI * 2) {
                flipAngle = 0;
                isFlipping = false;
            }
        }
    }

    // Render drone entity
    public void draw(Graphics2D g2d, int originX, int originY, int tileWidth, int tileHeight) {
        // Isometric screen coordinates
        double tileX = originX + (visualX - visualY) * (tileWidth / 2.0);
        double tileY = originY + (visualX + visualY) * (tileHeight / 2.0);

        // 1. Drop shadow on ground
        g2d.setColor(new Color(0, 0, 0, 45));
        int shadowW = 44;
        int shadowH = 22;
        g2d.fillOval((int) tileX - shadowW / 2, (int) tileY - shadowH / 2, shadowW, shadowH);

        // 2. Hover altitude
        int dronePixelX = (int) Math.round(tileX);
        int dronePixelY = (int) Math.round(tileY - 38 + hoverOffset + harvestBob);

        AffineTransform oldTx = g2d.getTransform();
        g2d.translate(dronePixelX, dronePixelY);
        if (isFlipping) {
            g2d.rotate(flipAngle);
        }

        // 3. Quadcopter arms (X cross)
        g2d.setColor(armColor);
        g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int armLen = 24;
        g2d.drawLine(-armLen, -armLen + 4, armLen, armLen - 4);
        g2d.drawLine(-armLen, armLen - 4, armLen, -armLen + 4);

        // 4. 4 spinning rotors
        drawRotor(g2d, -armLen, -armLen + 4, propAngle);
        drawRotor(g2d, armLen, -armLen + 4, -propAngle);
        drawRotor(g2d, -armLen, armLen - 4, -propAngle);
        drawRotor(g2d, armLen, armLen - 4, propAngle);

        // 5. Drone body chassis
        int bodyR = 15;
        g2d.setColor(bodyDarkColor);
        g2d.fillOval(-bodyR, -bodyR + 2, bodyR * 2, bodyR * 2);
        g2d.setColor(bodyColor);
        g2d.fillOval(-bodyR, -bodyR, bodyR * 2, bodyR * 2);

        // Specular highlight
        g2d.setColor(new Color(255, 230, 160, 160));
        g2d.fillOval(-bodyR + 3, -bodyR + 3, 8, 6);

        // 6. Farmer straw hat
        drawStrawHat(g2d, 0, -4);

        g2d.setTransform(oldTx);
    }

    private void drawRotor(Graphics2D g2d, int cx, int cy, double angle) {
        g2d.setColor(new Color(30, 30, 32));
        g2d.fillOval(cx - 3, cy - 3, 6, 6);

        AffineTransform old = g2d.getTransform();
        g2d.translate(cx, cy);
        g2d.rotate(angle);

        g2d.setColor(rotorColor);
        g2d.fillOval(-12, -2, 24, 4);
        g2d.rotate(Math.PI / 2);
        g2d.setColor(new Color(rotorColor.getRed(), rotorColor.getGreen(), rotorColor.getBlue(), 120));
        g2d.fillOval(-10, -2, 20, 4);

        g2d.setTransform(old);
    }

    private void drawStrawHat(Graphics2D g2d, int hx, int hy) {
        // Hat brim
        int brimW = 34;
        int brimH = 20;
        g2d.setColor(strawHatBrimDark);
        g2d.fillOval(hx - brimW / 2, hy - brimH / 2 + 2, brimW, brimH);
        g2d.setColor(strawHatBrim);
        g2d.fillOval(hx - brimW / 2, hy - brimH / 2, brimW, brimH);

        // Hat texture contour
        g2d.setColor(new Color(200, 155, 75, 120));
        g2d.drawOval(hx - brimW / 2 + 2, hy - brimH / 2 + 1, brimW - 4, brimH - 2);

        // Red band
        int bandW = 16;
        int bandH = 10;
        g2d.setColor(strawHatBand);
        g2d.fillOval(hx - bandW / 2, hy - bandH / 2, bandW, bandH);

        // Hat crown
        int topW = 14;
        int topH = 12;
        g2d.setColor(strawHatTop);
        g2d.fillOval(hx - topW / 2, hy - topH / 2 - 2, topW, topH);

        // Top highlight
        g2d.setColor(new Color(255, 240, 170, 180));
        g2d.fillOval(hx - 3, hy - 7, 6, 4);
    }

    // Getters and Setters
    public int getX() { return gridX; }
    public int getY() { return gridY; }
    public void setPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
        this.visualX = x;
        this.visualY = y;
    }
    public int getDirection() { return direction; }
    public void setDirection(int dir) {
        this.direction = (dir % 4 + 4) % 4;
        updateTargetAngle();
    }
}
