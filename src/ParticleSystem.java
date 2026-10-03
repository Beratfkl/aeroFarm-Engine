import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * ParticleSystem
 * Manages harvest debris chaff particles and propeller dust effects.
 */
public class ParticleSystem {
    public static class Particle {
        float x, y;
        float vx, vy;
        Color color;
        float size;
        float life;
        float maxLife;
        float gravity;

        public Particle(float x, float y, float vx, float vy, Color color, float size, float maxLife, float gravity) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.maxLife = maxLife;
            this.life = maxLife;
            this.gravity = gravity;
        }

        // Particle physics step (velocity + gravity + friction)
        public boolean update() {
            x += vx;
            y += vy;
            vy += gravity;
            vx *= 0.95f; // Air resistance
            life -= 1.0f;
            return life > 0;
        }

        public void draw(Graphics2D g2d) {
            // Fade out alpha as life decreases
            float alpha = Math.max(0f, Math.min(1f, life / maxLife));
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255));
            g2d.setColor(c);
            int s = Math.max(1, (int) size);
            g2d.fillRect((int) x - s / 2, (int) y - s / 2, s, s);
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random rand = new Random();

    // Burst harvest particles (yellow for wheat, green for wild grass)
    public synchronized void spawnHarvestParticles(int x, int y, boolean isWheat) {
        Color[] colors = isWheat
                ? new Color[]{new Color(245, 215, 110), new Color(225, 185, 80), new Color(255, 240, 160), new Color(180, 140, 50)}
                : new Color[]{new Color(110, 205, 80), new Color(140, 230, 90), new Color(80, 160, 50), new Color(210, 240, 130)};

        for (int i = 0; i < 24; i++) {
            float angle = (float) (rand.nextDouble() * Math.PI * 2);
            float speed = 2.0f + rand.nextFloat() * 5.5f;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed - 3.5f; // Upward burst
            Color c = colors[rand.nextInt(colors.length)];
            float size = 3.0f + rand.nextFloat() * 4.0f;
            float life = 20 + rand.nextInt(25);
            particles.add(new Particle(x, y, vx, vy, c, size, life, 0.25f));
        }
    }

    // Propeller dust trail
    public synchronized void spawnPropellerDust(int x, int y) {
        if (rand.nextFloat() < 0.35f) {
            float vx = (rand.nextFloat() - 0.5f) * 1.5f;
            float vy = 0.5f + rand.nextFloat() * 1.5f;
            Color c = new Color(240, 245, 250, 90);
            particles.add(new Particle(x, y, vx, vy, c, 2.5f, 15, 0.05f));
        }
    }

    public synchronized void update() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            if (!it.next().update()) {
                it.remove();
            }
        }
    }

    public synchronized void draw(Graphics2D g2d) {
        for (Particle p : particles) {
            p.draw(g2d);
        }
    }
}
