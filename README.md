# 🌱 aeroFarm Engine - Java Drone Automation & Scripting Simulator

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![GUI](https://img.shields.io/badge/GUI-Java_Swing_%26_Java2D-5382a1?style=for-the-badge&logo=java&logoColor=white)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![Architecture](https://img.shields.io/badge/Architecture-Custom_AST_Interpreter-4ADE80?style=for-the-badge)](https://github.com/)

**aeroFarm Engine** is a cozy automation simulator where you program an autonomous farming drone using Java/C-like scripts on a floating isometric island. 

I built this project inspired by *The Farmer Was Replaced*, but wanted to see if I could build the entire tech stack without relying on heavy game engines (like Unity or Godot) or third-party scripting libraries (like Groovy or Lua). It runs entirely on **Pure Java (Swing & Java2D)** with a **custom-built AST interpreter** under the hood.

---

## 📌 How It Works

You write code inside an in-game floating editor. The engine tokenizes your code, builds an Abstract Syntax Tree (AST), and executes it on a dedicated background thread to control the drone on the island in real time.

---

## ✨ Features

### 1. 🧠 Custom Scripting Engine & AST Interpreter
- **Recursive-Descent Parser:** Tokenizes raw text and parses it into statement/expression AST nodes.
- **Supported Control Flow:**
  - `while`, `do-while`, standard `for`, and enhanced `for-each` loops.
  - `if`, `else if`, `else`, and `switch-case` statements.
  - Variable types: `int`, `boolean`, `String`, and multi-dimensional arrays (`Array/List`).
  - Custom functions: Declare user-defined functions using `def` / `void` with proper `return` handling.
  - Full arithmetic, comparison, logical operators, and `Math.*` helper functions.

### 2. ⚡ Multithreading & System Safety
- **Dedicated Script Thread:** Scripts run on a separate `JavaDroneScriptRunner` worker thread, keeping the Swing Event Dispatch Thread (EDT) responsive and stutter-free.
- **Cooperative CPU Yielding:** Includes a `Thread.sleep(1)` breather every N instructions so tight loops won't peg your CPU at 100%.
- **Infinite Loop Watchdog:** Detects loops running without drone actions (`MAX_IDLE_STEPS`) and stops them safely.
- **Live Line Highlighting:** Syncs with the UI to highlight the exact line of code the drone is executing.

### 3. 🎨 Visuals & Physics
- **2:1 Isometric Projection:** Clean, mathematically projected floating islands.
- **Smooth Movement (LERP):** The drone doesn't just teleport between tiles—it smoothly glides using linear interpolation.
- **Procedural Animations:** Rotor spin, subtle hover bobbing (sine wave), flip animations (`doAFlip()`), harvest hops, and particle dust effects.
- **Farm Mechanics:** Soil tilling (`till`), watering (`water`), crop growth stages, and crop types (Wheat, Carrot).

### 4. 🎛️ In-Game IDE & Controls
- **Floating Code Window:** Draggable, semi-transparent editor with line numbers and syntax highlighting.
- **Island Sizes:** Toggle between 1x1, 3x3, and 4x4 islands on the fly.
- **Speed Settings:** 1x (Normal), 2x (Fast), 4x (Turbo), and Instant speed.
- **Preset Scripts:** Comes loaded with starter algorithms for perimeter runs, snake patterns, and crop rotators.
- **API Handbook:** Built-in cheat sheet for all drone methods.

---

## 📐 Architecture Overview

I structured the project to keep the engine, interpreter, and rendering decoupled:

| Class / Module | Responsibility |
| :--- | :--- |
| **`Main.java`** | Entry point. Sets up the `JLayeredPane`, top HUD bar, and lifecycle. |
| **`MiniJavaParser.java`** | Lexer/tokenizer, recursive-descent parser, and AST node definitions. |
| **`MiniJavaInterpreter.java`** | AST executor, scope/environment manager, thread controller, and CPU watchdog. |
| **`FarmPanel.java`** | 60 FPS game loop, isometric math, tile rendering, and sky background. |
| **`Drone.java`** | Drone logic, grid-to-screen coordinate mapping, LERP motion, and animations. |
| **`FloatingCodeWindow.java`** | Draggable in-game editor with syntax styling. |
| **`CodePresets.java`** | Repository of ready-to-run automation presets. |
| **`Tile.java` & `ParticleSystem.java`** | Crop growth state machine and dust particle physics. |

---

## 🚀 Drone Scripting API

Here are the built-in functions available in the in-game editor:

```java
// --- Actions ---
move();                 // Move 1 tile forward (returns false if at grid edge)
turnRight();            // Turn 90 degrees right
turnLeft();             // Turn 90 degrees left
till();                 // Till current tile into farmland
plant("WHEAT");         // Plant a crop ("WHEAT", "CARROT")
water();                // Water current tile
harvest();              // Harvest mature crop
doAFlip();              // Play an acrobatic flip animation

// --- Sensors & State ---
canHarvest();           // Check if current tile is ready for harvest (boolean)
isTilled();             // Check if soil is tilled (boolean)
isWatered();            // Check if soil is watered (boolean)
getCrop();              // Get crop type on current tile ("NONE", "WHEAT", "CARROT")
getPosX();              // Drone grid X coordinate
getPosY();              // Drone grid Y coordinate
getWorldSize();         // Island grid dimension (e.g. 3 or 4)

// --- System ---
System.out.println(...); // Print output to console / UI status
sleep(ms);              // Pause execution for N milliseconds
```

---

## 💡 Example Automation Script

```java
// Automated cultivation and harvest loop for a 3x3 island
while (true) {
    if (!isTilled()) {
        till();
    }
    
    if (getCrop().equals("NONE")) {
        plant("WHEAT");
        water();
    }
    
    if (canHarvest()) {
        harvest();
    }
    
    // Move forward; if hitting the island boundary, turn right
    if (!move()) {
        turnRight();
    }
}
```

---

## 🛠️ Running & Setup

You can run **aeroFarm Engine** either as a standalone desktop app or directly from source:

### Option A: Standalone Windows App (No Java Installation Needed)
1. Download `aeroFarm-Windows-x64.zip` from the [Releases](../../releases) section.
2. Extract the ZIP folder.
3. Double-click `aeroFarm.exe` to launch immediately.

### Option B: Executable JAR
If you already have Java 17+ installed, you can simply run the standalone JAR:
```bash
java -jar dist/aeroFarm.jar
```

### Option C: Build from Source
No external build tools (Maven/Gradle) required—just pure JDK 17+:

```bash
# 1. Compile all source files into bin/
javac -d bin src/*.java

# 2. Run the application
java -cp bin Main
```
*(Or simply open the project in IntelliJ IDEA / Eclipse and run `Main.java` directly.)*

---

## 👨‍💻 Development & Authorship Notes

This project combines core systems I wrote and designed directly, alongside areas where I leveraged AI as a pair programming assistant:

* ✍️ **Written & Designed by Me:**
  - **Game & Simulation Logic:** Core game design, grid state rules, crop growth stages, and island boundaries (`FarmPanel.java`, `Tile.java`).
  - **System Architecture & Grid Navigation:** Drone grid coordinates, heading management, and boundary safety checks (`Drone.java`).
  - **UI & Layout:** Layered pane architecture (`JLayeredPane`), HUD controls, and the draggable floating code editor (`FloatingCodeWindow.java`, `Main.java`).
  - **Debugging & Performance Tuning:** Caught a major issue during testing where tight `while(true)` loops were pegging CPU usage at 100%. Designed the *Watchdog* (`MAX_IDLE_STEPS`) and *Cooperative Yielding* (`Thread.sleep(1)`) mechanisms to keep things smooth.
  - **Refactoring:** Decoupled the original monolithic interpreter into clean `Parser`, `Interpreter`, and `Presets` modules to respect single responsibility.

* 🤖 **AI-Assisted (Pair Programming):**
  - **AST Parser / Lexer Boilerplate:** Used AI to speed up drafting the recursive-descent grammar rules, regex tokenizer patterns, and AST node boilerplate in `MiniJavaParser.java`.
  - **Vector Graphics & Math Calculations:** Leveraged AI assistance for the Java2D procedural drawing routines, affine matrix transformations (`AffineTransform`), LERP calculations, and particle alpha decay math.
  - **Script Presets:** Generated some of the multi-size algorithm preset templates in `CodePresets.java` to save time on repetitive code examples.
