import java.util.ArrayList;
import java.util.List;

/**
 * CodePresets
 * Repository of categorized, ready-to-run automation scripts for the drone farm.
 * Demonstrates basic, intermediate, and advanced Java control flow concepts.
 */
public class CodePresets {

    public static class Preset {
        private final String title;
        private final String description;
        private final String code;

        public Preset(String title, String description, String code) {
            this.title = title;
            this.description = description;
            this.code = code;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static class PresetCategory {
        private final String name;
        private final List<Preset> presets = new ArrayList<>();

        public PresetCategory(String name) {
            this.name = name;
        }

        public void add(Preset preset) {
            presets.add(preset);
        }

        public String getName() {
            return name;
        }

        public List<Preset> getPresets() {
            return presets;
        }
    }

    /**
     * Returns the recommended starter code for a specific island grid size.
     */
    public static String getDefaultPresetForGrid(int size) {
        if (size == 1) {
            return "// 1x1 Island Starter: Automated sensor-based harvester\n"
                    + "while (true) {\n"
                    + "    if (canHarvest()) {\n"
                    + "        harvest();\n"
                    + "    }\n"
                    + "}";
        } else if (size == 3) {
            return "// 3x3 Island Starter: Perimeter traversal and harvest\n"
                    + "while (true) {\n"
                    + "    if (canHarvest()) {\n"
                    + "        harvest();\n"
                    + "    }\n"
                    + "    if (!move()) {\n"
                    + "        turnRight();\n"
                    + "    }\n"
                    + "}";
        } else {
            return "// 4x4 Island Starter: Full cultivation loop\n"
                    + "while (true) {\n"
                    + "    if (!isTilled()) {\n"
                    + "        till();\n"
                    + "    }\n"
                    + "    if (getCrop().equals(\"NONE\")) {\n"
                    + "        plant(\"WHEAT\");\n"
                    + "        water();\n"
                    + "    }\n"
                    + "    if (canHarvest()) {\n"
                    + "        harvest();\n"
                    + "    }\n"
                    + "    if (!move()) {\n"
                    + "        turnRight();\n"
                    + "    }\n"
                    + "}";
        }
    }

    /**
     * Builds categorized script presets organized by topic and difficulty.
     */
    public static List<PresetCategory> getCategorizedPresets(int currentGridSize) {
        List<PresetCategory> categories = new ArrayList<>();

        // 1. Grid Size Specific Starters
        PresetCategory startersCat = new PresetCategory("Island Starters (" + currentGridSize + "x" + currentGridSize + ")");
        if (currentGridSize == 1) {
            startersCat.add(new Preset(
                    "1. Smart Sensor Harvester",
                    "Checks if wheat is fully grown before harvesting.",
                    "while (true) {\n    if (canHarvest()) {\n        harvest();\n    }\n}"
            ));
            startersCat.add(new Preset(
                    "2. Continuous Rapid Mower",
                    "Rapidly attempts to harvest each tick.",
                    "while (true) {\n    harvest();\n}"
            ));
            startersCat.add(new Preset(
                    "3. Single-Tile Cultivator",
                    "Tills, plants wheat, waters, and harvests.",
                    "while (true) {\n"
                            + "    if (!isTilled()) {\n"
                            + "        till();\n"
                            + "    }\n"
                            + "    if (getCrop().equals(\"NONE\")) {\n"
                            + "        plant(\"WHEAT\");\n"
                            + "        water();\n"
                            + "    }\n"
                            + "    if (canHarvest()) {\n"
                            + "        harvest();\n"
                            + "    }\n"
                            + "}"
            ));
        } else if (currentGridSize == 3) {
            startersCat.add(new Preset(
                    "1. Standard Perimeter Harvester",
                    "Traverses around the edges of the 3x3 island.",
                    "while (true) {\n"
                            + "    if (canHarvest()) {\n"
                            + "        harvest();\n"
                            + "    }\n"
                            + "    if (!move()) {\n"
                            + "        turnRight();\n"
                            + "    }\n"
                            + "}"
            ));
            startersCat.add(new Preset(
                    "2. Wheat Seeder & Irrigator",
                    "Tills and waters while planting wheat across the farm.",
                    "while (true) {\n"
                            + "    if (!isTilled()) {\n"
                            + "        till();\n"
                            + "    }\n"
                            + "    if (getCrop().equals(\"NONE\")) {\n"
                            + "        plant(\"WHEAT\");\n"
                            + "        water();\n"
                            + "    }\n"
                            + "    if (canHarvest()) {\n"
                            + "        harvest();\n"
                            + "    }\n"
                            + "    if (!move()) {\n"
                            + "        turnRight();\n"
                            + "    }\n"
                            + "}"
            ));
        } else {
            startersCat.add(new Preset(
                    "1. Grand Island Multi-Crop Automation",
                    "Full cycle management for the 4x4 island.",
                    "while (true) {\n"
                            + "    if (!isTilled()) {\n"
                            + "        till();\n"
                            + "    }\n"
                            + "    if (getCrop().equals(\"NONE\")) {\n"
                            + "        plant(\"CARROT\");\n"
                            + "        water();\n"
                            + "    }\n"
                            + "    if (canHarvest()) {\n"
                            + "        harvest();\n"
                            + "    }\n"
                            + "    if (!move()) {\n"
                            + "        turnRight();\n"
                            + "    }\n"
                            + "}"
            ));
        }
        categories.add(startersCat);

        // 2. Loop Demonstrations (While, Do-While, For, For-Each)
        PresetCategory loopsCat = new PresetCategory("Loops & Iteration");
        loopsCat.add(new Preset(
                "Do-While: Search Until Harvestable",
                "Uses do-while loop to move at least once before testing condition.",
                "// Do-While Demo: Move forward until a harvestable crop is found\n"
                        + "while (true) {\n"
                        + "    do {\n"
                        + "        if (!move()) {\n"
                        + "            turnRight();\n"
                        + "        }\n"
                        + "    } while (!canHarvest());\n"
                        + "    \n"
                        + "    harvest();\n"
                        + "    plant(\"WHEAT\");\n"
                        + "    water();\n"
                        + "}"
        ));

        loopsCat.add(new Preset(
                "For-Each: Multi-Crop Rotation",
                "Iterates through an array of crop types using enhanced for loop.",
                "// For-Each Demo: Plant a sequence of rotating crops\n"
                        + "String[] cropList = {\"WHEAT\", \"CARROT\", \"WHEAT\"};\n"
                        + "\n"
                        + "while (true) {\n"
                        + "    for (String crop : cropList) {\n"
                        + "        if (!isTilled()) {\n"
                        + "            till();\n"
                        + "        }\n"
                        + "        if (canHarvest()) {\n"
                        + "            harvest();\n"
                        + "        }\n"
                        + "        plant(crop);\n"
                        + "        water();\n"
                        + "        if (!move()) {\n"
                        + "            turnRight();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"
        ));

        loopsCat.add(new Preset(
                "Classic For: Bounded Grid Sweep",
                "Uses standard for loops with step counters.",
                "// Classic For Loop: Sweep fixed number of steps\n"
                        + "int worldSize = getWorldSize();\n"
                        + "\n"
                        + "while (true) {\n"
                        + "    for (int step = 0; step < worldSize; step++) {\n"
                        + "        if (canHarvest()) {\n"
                        + "            harvest();\n"
                        + "        }\n"
                        + "        if (!move()) {\n"
                        + "            turnRight();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"
        ));
        categories.add(loopsCat);

        // 3. Conditionals & Switch-Case
        PresetCategory condCat = new PresetCategory("Conditionals & Branching");
        condCat.add(new Preset(
                "Switch-Case: Crop Classifier & Handler",
                "Dispatches actions based on the current tile crop type.",
                "// Switch-Case Demo: Handle different crops specifically\n"
                        + "while (true) {\n"
                        + "    String crop = getCrop();\n"
                        + "    switch (crop) {\n"
                        + "        case \"WHEAT\":\n"
                        + "            if (canHarvest()) {\n"
                        + "                harvest();\n"
                        + "            } else if (!isWatered()) {\n"
                        + "                water();\n"
                        + "            }\n"
                        + "            break;\n"
                        + "        case \"CARROT\":\n"
                        + "            if (canHarvest()) {\n"
                        + "                harvest();\n"
                        + "            }\n"
                        + "            break;\n"
                        + "        default:\n"
                        + "            if (!isTilled()) {\n"
                        + "                till();\n"
                        + "            }\n"
                        + "            plant(\"WHEAT\");\n"
                        + "            water();\n"
                        + "            break;\n"
                        + "    }\n"
                        + "    if (!move()) {\n"
                        + "        turnRight();\n"
                        + "    }\n"
                        + "}"
        ));

        condCat.add(new Preset(
                "If-Else: Priority Farmer",
                "Harvest > Water > Plant priority ladder.",
                "// If-Else Priority Decision Tree\n"
                        + "while (true) {\n"
                        + "    if (canHarvest()) {\n"
                        + "        harvest();\n"
                        + "    } else if (!isTilled()) {\n"
                        + "        till();\n"
                        + "    } else if (getCrop().equals(\"NONE\")) {\n"
                        + "        plant(\"CARROT\");\n"
                        + "    } else if (!isWatered()) {\n"
                        + "        water();\n"
                        + "    }\n"
                        + "    if (!move()) {\n"
                        + "        turnRight();\n"
                        + "    }\n"
                        + "}"
        ));
        categories.add(condCat);

        // 4. Custom Functions
        PresetCategory funcCat = new PresetCategory("Functions & Modular Code");
        funcCat.add(new Preset(
                "Modular Functions: Reusable Routine",
                "Defines void helper functions for modular script design.",
                "// Functions Demo: Modular harvest and sow routines\n"
                        + "void serviceTile(String preferredCrop) {\n"
                        + "    if (canHarvest()) {\n"
                        + "        harvest();\n"
                        + "    }\n"
                        + "    if (!isTilled()) {\n"
                        + "        till();\n"
                        + "    }\n"
                        + "    if (getCrop().equals(\"NONE\")) {\n"
                        + "        plant(preferredCrop);\n"
                        + "    }\n"
                        + "    if (!isWatered()) {\n"
                        + "        water();\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"
                        + "while (true) {\n"
                        + "    serviceTile(\"WHEAT\");\n"
                        + "    if (!move()) {\n"
                        + "        turnRight();\n"
                        + "    }\n"
                        + "}"
        ));

        funcCat.add(new Preset(
                "Acrobatic Drone: Flip on Harvest",
                "Executes celebratory drone flips after harvesting.",
                "// Drone Tricks Demo\n"
                        + "while (true) {\n"
                        + "    if (canHarvest()) {\n"
                        + "        harvest();\n"
                        + "        doAFlip();\n"
                        + "    }\n"
                        + "    if (!move()) {\n"
                        + "        turnRight();\n"
                        + "    }\n"
                        + "}"
        ));
        categories.add(funcCat);

        return categories;
    }
}
