
import java.util.*;
import java.util.function.Function;

/**
 * MiniJavaInterpreter Script execution engine connecting the AST parser to the
 * physical drone island. Features thread scheduling, pause/resume, cooperative
 * CPU yields, and loop watchdog.
 */
public class MiniJavaInterpreter {

    // ==========================================
    // Drone Action API Interface
    // ==========================================
    public interface DroneInterface {

        boolean harvest();

        boolean canHarvest();

        boolean move();

        void turnLeft();

        void turnRight();

        boolean plant(String crop);

        void water();

        void till();

        int getPosX();

        int getPosY();

        int getWorldSize();

        String getCrop();

        boolean isTilled();

        boolean isWatered();

        void doAFlip();
    }

    // ==========================================
    // UI Event Listener
    // ==========================================
    public interface ExecutionListener {

        void onLineExecute(int lineNumber, String lineText);

        void onPrint(String message);

        void onError(int lineNumber, String message);

        void onFinished();
    }

    // ==========================================
    // Flow Control & Stop Signals
    // ==========================================
    public static class StopException extends Exception {

        public StopException() {
            super("Script stopped");
        }
    }

    public static class ReturnException extends RuntimeException {

        public final Object value;

        public ReturnException(Object value) {
            this.value = value;
        }
    }

    public static class BreakException extends RuntimeException {
    }

    public static class ContinueException extends RuntimeException {
    }

    // ==========================================
    // Lexical Scope / Environment Table
    // ==========================================
    public static class Environment {

        private final Environment parent;
        private final Map<String, Object> variables = new HashMap<>();

        public Environment(Environment parent) {
            this.parent = parent;
        }

        public Object get(String name) {
            if (variables.containsKey(name)) {
                return variables.get(name);
            }
            if (parent != null) {
                return parent.get(name);
            }
            return null;
        }

        public void set(String name, Object value) {
            variables.put(name, value);
        }

        public void update(String name, Object value) {
            if (variables.containsKey(name)) {
                variables.put(name, value);
            } else if (parent != null && parent.contains(name)) {
                parent.update(name, value);
            } else {
                variables.put(name, value);
            }
        }

        public boolean contains(String name) {
            return variables.containsKey(name) || (parent != null && parent.contains(name));
        }
    }

    // Runtime state variables
    private final DroneInterface drone;
    private final ExecutionListener listener;
    private Thread executionThread;
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private volatile boolean isStopped = false;
    private volatile int stepDelayMs = 250; // Delay between drone steps (ms)
    private int currentExecutingLine = 1;
    private long lastLineEventTime = 0;
    private int lastReportedLine = -1;

    // Safety guards & cooperative scheduling
    private int instructionCount = 0;
    private int stepsWithoutAction = 0;
    private int callDepth = 0;
    private static final int YIELD_INTERVAL = 500;    // Yield CPU every 500 statements
    private static final int MAX_IDLE_STEPS = 10000;  // ~10k steps without drone action = halt
    private static final int MAX_CALL_DEPTH = 100;    // Recursion depth limit

    public MiniJavaInterpreter(DroneInterface drone, ExecutionListener listener) {
        this.drone = drone;
        this.listener = listener;
    }

    public void setStepDelayMs(int delayMs) {
        this.stepDelayMs = Math.max(10, delayMs);
    }

    public int getStepDelayMs() {
        return stepDelayMs;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isStopped() {
        return isStopped;
    }

    // Launch script execution on a dedicated background thread
    public synchronized void start(String code) {
        stop();
        isStopped = false;
        isPaused = false;
        isRunning = true;
        lastLineEventTime = 0;
        lastReportedLine = -1;
        instructionCount = 0;
        stepsWithoutAction = 0;
        callDepth = 0;

        executionThread = new Thread(() -> {
            try {
                Environment globalEnv = new Environment(null);
                registerBuiltins(globalEnv);
                List<MiniJavaParser.Statement> statements = MiniJavaParser.parse(code);

                for (MiniJavaParser.Statement stmt : statements) {
                    if (isStopped) {
                        break;
                    }
                    stmt.execute(globalEnv, this);
                }
            } catch (StopException e) {
                // Script stopped gracefully by user
            } catch (Exception e) {
                if (!isStopped && listener != null) {
                    listener.onError(currentExecutingLine, e.getMessage());
                }
            } finally {
                isRunning = false;
                isPaused = false;
                if (listener != null) {
                    listener.onFinished();
                }
            }
        }, "JavaDroneScriptRunner");
        executionThread.start();
    }

    // Stop script execution
    public synchronized void stop() {
        isStopped = true;
        isPaused = false;
        if (executionThread != null && executionThread.isAlive()) {
            executionThread.interrupt();
        }
    }

    // Pause / Resume execution
    public synchronized void togglePause() {
        isPaused = !isPaused;
    }

    public synchronized void setPaused(boolean paused) {
        isPaused = paused;
    }

    // Check pause/stop flags, throttled UI highlighting, and yield CPU
    public void checkState(int lineNumber, String lineText) throws StopException {
        if (isStopped || Thread.currentThread().isInterrupted()) {
            throw new StopException();
        }

        instructionCount++;
        stepsWithoutAction++;

        // Cooperative scheduling: yield every YIELD_INTERVAL steps so CPU doesn't freeze
        if (instructionCount % YIELD_INTERVAL == 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new StopException();
            }
        }

        // Infinite loop guard: halt if too many instructions run without any drone action
        // Using instructionCount so it always increases and can never be reset by other code paths
        if (stepsWithoutAction > MAX_IDLE_STEPS) {
            stepsWithoutAction = 0;
            throw new RuntimeException("Infinite loop: " + MAX_IDLE_STEPS + " steps without a drone action. Add move(), harvest(), or sleep() inside the loop.");
        }

        currentExecutingLine = lineNumber;
        long now = System.currentTimeMillis();
        // Throttle UI event dispatches to maintain 60 FPS UI performance
        if (listener != null && lineNumber > 0) {
            if (lineNumber != lastReportedLine || now - lastLineEventTime > 30) {
                lastReportedLine = lineNumber;
                lastLineEventTime = now;
                listener.onLineExecute(lineNumber, lineText);
            }
        }
        while (isPaused && !isStopped) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new StopException();
            }
        }
    }

    public void checkCallDepth() throws RuntimeException {
        if (++callDepth > MAX_CALL_DEPTH) {
            callDepth = 0;
            throw new RuntimeException("Maximum call depth reached (" + MAX_CALL_DEPTH + ")!");
        }
    }

    public void leaveCall() {
        callDepth = Math.max(0, callDepth - 1);
    }

    // Step delay between physical drone operations
    private void actionDelay() throws StopException {
        stepsWithoutAction = 0; // Reset action counter
        if (stepDelayMs > 0) {
            try {
                Thread.sleep(stepDelayMs);
            } catch (InterruptedException e) {
                throw new StopException();
            }
        }
    }

    // Register built-in constants and drone functions into global environment
    private void registerBuiltins(Environment env) {
        env.set("true", true);
        env.set("false", false);
        env.set("null", null);
        env.set("WHEAT", "WHEAT");
        env.set("CARROT", "CARROT");
        env.set("GRASS", "GRASS");

        // Drone Actions
        env.set("harvest", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "harvest()");
                boolean res = drone.harvest();
                actionDelay();
                return res;
            } catch (StopException e) {
                throw new RuntimeException(e);
            }
        });

        env.set("canHarvest", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "canHarvest()");
                boolean res = drone.canHarvest();
                if (!res) {
                    Thread.sleep(Math.min(60, Math.max(20, stepDelayMs / 4)));
                }
                stepsWithoutAction = 0;
                return res;
            } catch (StopException | InterruptedException e) {
                throw new RuntimeException(new StopException());
            }
        });
        env.set("can_harvest", env.get("canHarvest"));

        env.set("move", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "move()");
                boolean res = drone.move();
                actionDelay();
                return res;
            } catch (StopException e) {
                throw new RuntimeException(e);
            }
        });
        env.set("moveForward", env.get("move"));

        env.set("turnLeft", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "turnLeft()");
                drone.turnLeft();
                actionDelay();
                return null;
            } catch (StopException e) {
                throw new RuntimeException(e);
            }
        });
        env.set("turn_left", env.get("turnLeft"));

        env.set("turnRight", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "turnRight()");
                drone.turnRight();
                actionDelay();
                return null;
            } catch (StopException e) {
                throw new RuntimeException(e);
            }
        });
        env.set("turn_right", env.get("turnRight"));

        env.set("plant", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "plant()");
                String crop = (args != null && !args.isEmpty() && args.get(0) != null) ? args.get(0).toString() : "WHEAT";
                boolean res = drone.plant(crop);
                actionDelay();
                return res;
            } catch (StopException e) {
                throw new RuntimeException(e);
            }
        });

        env.set("water", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "water()");
                drone.water();
                actionDelay();
                return null;
            } catch (StopException e) {
                throw new RuntimeException(e);
            }
        });

        env.set("till", (Function<List<Object>, Object>) args -> {
            try {
                checkState(currentExecutingLine, "till()");
                drone.till();
                actionDelay();
                return null;
            } catch (StopException e) {
                throw new RuntimeException(e);
            }
        });

        env.set("getPosX", (Function<List<Object>, Object>) args -> drone.getPosX());
        env.set("get_pos_x", env.get("getPosX"));

        env.set("getPosY", (Function<List<Object>, Object>) args -> drone.getPosY());
        env.set("get_pos_y", env.get("getPosY"));

        env.set("getWorldSize", (Function<List<Object>, Object>) args -> drone.getWorldSize());
        env.set("get_world_size", env.get("getWorldSize"));

        env.set("getCrop", (Function<List<Object>, Object>) args -> drone.getCrop());
        env.set("get_crop", env.get("getCrop"));

        env.set("isTilled", (Function<List<Object>, Object>) args -> drone.isTilled());
        env.set("is_tilled", env.get("isTilled"));

        env.set("isWatered", (Function<List<Object>, Object>) args -> drone.isWatered());
        env.set("is_watered", env.get("isWatered"));

        env.set("doAFlip", (Function<List<Object>, Object>) args -> {
            drone.doAFlip();
            return null;
        });
        env.set("do_a_flip", env.get("doAFlip"));

        // Console output handler (System.out.println)
        Function<List<Object>, Object> printFn = args -> {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        sb.append(" ");
                    }
                    sb.append(args.get(i));
                }
            }
            if (listener != null) {
                listener.onPrint(sb.toString());
            }
            return null;
        };

        env.set("print", printFn);
        env.set("println", printFn);
        env.set("System.out.println", printFn);
        env.set("System.out.print", printFn);

        env.set("sleep", (Function<List<Object>, Object>) args -> {
            if (args != null && !args.isEmpty() && args.get(0) instanceof Number) {
                try {
                    stepsWithoutAction = 0;
                    long ms = ((Number) args.get(0)).longValue();
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    throw new RuntimeException(new StopException());
                }
            }
            return null;
        });
    }
}
