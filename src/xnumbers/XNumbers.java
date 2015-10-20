package xnumbers;

/**
 * Copyright (c) 2010-2015 by George Anastassakis
 *
 * This file is part of XNumbers.
 *
 * XNumbers is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XNumbers is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XNumbers. If not, see http://www.gnu.org/licenses/.
 */

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Hashtable;

import org.web3d.x3d.sai.X3DScriptImplementation;
import org.web3d.x3d.sai.Browser;
import org.web3d.x3d.sai.MFFloat;
import org.web3d.x3d.sai.X3DExecutionContext;
import org.web3d.x3d.sai.X3DNode;
import org.web3d.x3d.sai.X3DScriptNode;
import org.web3d.x3d.sai.X3DField;
import org.web3d.x3d.sai.SFInt32;
import org.web3d.x3d.sai.MFNode;
import org.web3d.x3d.sai.SFVec3f;
import org.web3d.x3d.sai.SFNode;
import org.web3d.x3d.sai.SFColor;
import org.web3d.x3d.sai.SFFloat;
import org.web3d.x3d.sai.MFString;
import org.web3d.x3d.sai.X3DFieldEventListener;
import org.web3d.x3d.sai.X3DFieldEvent;
import org.web3d.x3d.sai.SFTime;
import org.web3d.x3d.sai.SFBool;
import org.web3d.x3d.sai.SFString;
import org.web3d.x3d.sai.MFInt32;
import org.web3d.x3d.sai.MFVec3f;

/**
 * <p>The implementation of an externally-scripted X3D Script node for XNumbers.</p>
 * 
 * <p>This implementation merely reflects one possible way to script X3D using
 * the External SAI. It is by no means intended to present preferred approaches
 * and methodologies in any sense. Please rely on this implementation only for
 * informative, demonstrative and educational purposes.<p>
 * 
 * @author George Anastassakis
 * @version 1.0
 */
public class XNumbers implements X3DScriptImplementation, X3DFieldEventListener {

    /**
     * Default new game shuffle depth.
     */
    public static final int DEFAULT_SHUFFLE_DEPTH = 25;

    /**
     * Available spawn methods.
     */
    public enum SpawnMethod {

        /**
         * Always spawn at the world origin.
         */
        ORIGIN,

        /**
         * Use a given sequence of spawn locations.
         */
        SEQUENTIAL,

        /**
         * Randomly select a spawn location from within a given set.
         */
        SHUFFLE,

        /**
         * Randomly select a spawn location in a given area in space.
         */
        RANDOM
    };

    /**
     * A reference to the browser.
     */
    private Browser browser = null;

    /**
     * A reference to the field used to programmatically simulate button-clicks.
     */
    private SFInt32 click = null;

    /**
     * A reference to the {@code children} field that will hold the game's
     * model.
     */
    private MFNode children = null;

    /**
     * URL to help file.
     */
    private String helpURL = null;

    /**
     * Node name prefix for generated nodes.
     */
    private String nodePrefix = null;

    /**
     * The game's width.
     */
    private int width = 0;

    /**
     * The game's height.
     */
    private int height = 0;

    /**
     * Respawn count (including first spawn).
     */
    private int spawnCount = 0;

    /**
     * Debug mode flag.
     */
    private boolean debug = false;

    /**
     * The spawn locations array.
     */
    private float[] spawnLocations = null;

    /**
     * Spawn method.
     */
    private SpawnMethod spawnLogic = SpawnMethod.ORIGIN;

    /**
     * A map of named materials.
     */
    private Hashtable<String, X3DNode> materials = null;

    /**
     * The flattened array of board data.
     */
    private int[] board = null;

    /**
     * The model's root node, used to position the game in space.
     */
    private X3DNode root = null;

    /**
     * The array of tile buttons nodes.
     */
    private X3DNode[] buttons = null;

    /**
     * Left button node.
     */
    private X3DNode button1 = null;

    /**
     * Right button node.
     */
    private X3DNode button2 = null;

    /**
     * Title node.
     */
    private X3DNode title = null;

    /**
     * Primary subtitle node.
     */
    private X3DNode subtitle1 = null;

    /**
     * Secondary subtitle node.
     */
    private X3DNode subtitle2 = null;

    /**
     * Missing tile index.
     */
    private int missingTileIndex = -1;

    /**
     * The game's state.
     */
    private int state = 0;

    /**
     * Step count.
     */
    private int steps = 0;

    /**
     * Gametime node.
     */
    private X3DNode timeSensor = null;

    /**
     * The readable board-state field.
     */
    private MFInt32 boardState = null;

    /**
     * Bounds visibility flag.
     */
    private boolean showBounds = false;

    /*** Inherited methods ****************************************************/

    /**
     *
     */
    @Override public void setBrowser(Browser browser) {

        // dump method name...
        debugMessage(".setBrowser");

        // save reference to browser for later use...
        this.browser = browser;

        // dump some information about the browser...
        debugMessage("Name: " + browser.getName());
        debugMessage("Description: " + browser.getDescription());
        debugMessage("Version: " + browser.getVersion());
    }

    /**
     *
     */
    @Override public void setFields(X3DScriptNode x3DScriptNode, Map fields) {

        // dump method name...
        debugMessage(".setFields");

        // dump information about the X3DScriptNode...
        debugMessage("X3DScriptNode: " + x3DScriptNode);

        // iterate over all script fields, validate and store values...

        Iterator<Entry> i = fields.entrySet().iterator();

        while (i.hasNext()) {

            // get script field name and value...

            Entry e = i.next();
            String n = (String) e.getKey();
            X3DField v = (X3DField) e.getValue();

            // populate fields with script field values...

            if (n.equals("width")) {
                width = ((SFInt32) v).getValue();
            }
            if (n.equals("height")) {
                height = ((SFInt32) v).getValue();
            }
            if (n.equals("spawnLogic")) {
                spawnLogic = SpawnMethod.valueOf(((SFString) v).getValue());
            }
            if (n.equals("spawnLocations")) {
                spawnLocations = new float[((MFFloat) v).getSize()];
                ((MFFloat) v).getValue(spawnLocations);
            }
            if (n.equals("debug")) {
                debug = ((SFBool) v).getValue();
            }
            if (n.equals("showBounds")) {
                showBounds = ((SFBool) v).getValue();
            }
            if (n.equals("children")) {
                children = (MFNode) v;
            }
            if (n.equals("click")) {
                click = (SFInt32) v;
                click.addX3DEventListener(this);
            }
            if (n.equals("helpURL")) {
                helpURL = ((SFString) v).getValue();
            }
            if (n.equals("nodePrefix")) {
                nodePrefix = ((SFString) v).getValue();
            }
            if (n.equals("boardState")) {
                boardState = (MFInt32) e.getValue();
            }
        }

        // check for required fields...

        if (width == 0) {
            throw new IllegalArgumentException("Required field \"width\" not specified");
        }
        if (height == 0) {
            throw new IllegalArgumentException("Required field \"height\" not specified");
        }
        if (children == null) {
            throw new IllegalArgumentException("Required field \"children\" not specified");
        }

        // validate...

        switch (spawnLogic) {
            case RANDOM:
                if (spawnLocations.length != 4) {
                    debugMessage("Spawnlogic is RANDOM but spawnlocations length is " + spawnLocations.length + ", should be 4");
                    debugMessage("Defaulting to ORIGIN");
                    spawnLogic = SpawnMethod.ORIGIN;
                }
                break;
            case SHUFFLE:
                if (spawnLocations.length < 2) {
                    debugMessage("Spawnlogic is SHUFFLE but spawnlocations length is " + spawnLocations.length + ", should be at least 2");
                    debugMessage("Defaulting to ORIGIN");
                    spawnLogic = SpawnMethod.ORIGIN;
                }
                break;
            case SEQUENTIAL:
                if (spawnLocations.length < 2) {
                    debugMessage("Spawnlogic is SEQUENTIAL but spawnlocations length is " + spawnLocations.length + ", should be at least 2");
                    debugMessage("Defaulting to ORIGIN");
                    spawnLogic = SpawnMethod.ORIGIN;
                }
                break;
        }
    }

    /**
     *
     */
    @Override public void initialize() {

        browser.println("XNumbers - " + VersionInfo.PROJECT_DESCRIPTION +
            ", v" + VersionInfo.VERSION_MAJOR +
            "." + VersionInfo.VERSION_MINOR +
            "." + VersionInfo.VERSION_IMPLEMENTATION +
            " (" + VersionInfo.VERSION_TSTAMP + ").");
        browser.println("Copyright (C) 2010 by " + VersionInfo.PROJECT_AUTHOR + ".");
        browser.println("XNumbers is available under the terms of the GNU General Public License version 3 as published by the Free Software Foundation, available at http://www.gnu.org/licenses/.");

        debugMessage(".initialize");

        resetData(false);

        // initialize...
        initializeMaterials(browser.getExecutionContext());
        initModel();
    }

    /**
     *
     */
    @Override public void shutdown() {
        debugMessage(".shutdown");
    }

    /**
     *
     */
    @Override public void eventsProcessed() {
    }

    /**
     *
     */
    @Override public void readableFieldChanged(X3DFieldEvent x3DFieldEvent) {

        if (x3DFieldEvent.getSource() == click) {
            int buttonId = click.getValue();
            debugMessage("Received click event on button id " + buttonId + "...");
            process(buttonId);
        }
        // if the event did not come for a known registered field, it is safe to
        // assume it has come from one of the dynamically-created
        // TouchSensors on the various buttons...
        else {
            int buttonId = ((Integer) ((X3DField) x3DFieldEvent.getSource()).getUserData()).intValue();
            debugMessage("Received TouchSensor event on button id " + buttonId + "...");
            process(buttonId);
        }
    }

    /*** Game logic ***********************************************************/

    /**
     * An initialization method that randomly arranges tiles and selects missing
     * one. Does not guarantee solvability.
     * @param isLastMissing true if the missing tile should always be the last
     * one, false otherwise
     */
    protected void resetDataRandom(boolean isLastMissing) {
        debugMessage(".resetDataRandom");
        int size = width * height;
        int maxIndex = size - 1;
        missingTileIndex = isLastMissing ? maxIndex : (int) Math.round(Math.random() * maxIndex);
        board = new int[size];
        boolean[] indexUsed = new boolean[size];
        for (int i = 0; i != size; i++) {
            indexUsed[i] = false;
        }
        int tileIndex = -1;
        for (int i = 0; i != size; i++) {
            do {
                tileIndex = (int) Math.round(Math.random() * maxIndex);
            }
            while (indexUsed[tileIndex]);
            indexUsed[tileIndex] = true;
            if (tileIndex == missingTileIndex) {
                tileIndex = -1;
            }
            board[i] = tileIndex;
        }
    }

    /**
     * An initialization method that arranges tiles by sequentially performing
     * random moves and randomly selects a missing tile. Guarantees solvability.
     * @param isLastMissing true if the missing tile should always be the last
     * one, false otherwise
     */
    protected void resetData(boolean isLastMissing) {
        debugMessage(".resetData");
        int size = width * height;
        int maxIndex = size - 1;
        missingTileIndex = isLastMissing ? maxIndex : (int) Math.round(Math.random() * maxIndex);
        board = new int[size];
        for (int i = 0; i != size; i++) {
            if (i != missingTileIndex) {
                board[i] = i;
            }
            else {
                board[i] = -1;
            }
        }
        debugMessage("Initial state: " + printArray(board, ",", true));

        ArrayList<Integer> free = new ArrayList<Integer>(4);
        int missingIndex = missingTileIndex;
        int optionIndex;
        for (int i = 0; i != DEFAULT_SHUFFLE_DEPTH; i++) {
            free.clear();
            for (int k = 0; k != size; k++) {
                if (checkFree(k) != -1) {
                    free.add(k);
                }
            }
            optionIndex = free.get((int) Math.abs(Math.random() * free.size())).intValue();
            debugMessage(
                "[" + i + "] " +
                "board: " + printArray(board, ",", true) + ", " +
                "free: " + printArray(free, ",", true) + ", " +
                "option: " + optionIndex);
            board[missingIndex] = board[optionIndex];
            board[optionIndex] = -1;
            missingIndex = optionIndex;
        }
    }

    /**
     * Starts a new game. In particular, it initializes data, sets button labels
     * and titles, starts the timer and arranges tiles.
     */
    protected void startGame() {
        debugMessage(".startGame");
        setButtonText(button1, "Abort");
        setTitleText(title, "Playing...");
        setTitleText(subtitle2, "Steps: 0");
        setTimerEnabled(true);
        resetData(false);
        dumpData();
        arrange();
        steps = 0;
        state = 1;
    }

    /**
     * Ends the current game. In particular, it respawns according to the
     * currently-selected respawn method, sets button labels and titles and
     * stops the timer.
     */
    protected void endGame() {
        debugMessage(".endGame");
        debugMessage("Game ended, checking spawn logic...");
        // respawn...
        respawn();
        // adjust GUI according to spawn logic...
        switch (spawnLogic) {
            case RANDOM:
            case SEQUENTIAL:
            case SHUFFLE:
                // game is unlikely to remain in front of the player, hence
                // there is no point in presenting post-game data...
                setButtonText(button1, "Start");
                setTitleText(title, "Welcome!");
                setTitleText(subtitle1, "");
                setTitleText(subtitle2, "");
                state = 0;
                break;
            case ORIGIN:
            default:
                // game will present meaningful post-game data to the player who
                // is still in front of it, and offer the option to restart...
                setButtonText(button1, "Restart");
                setTitleText(title, "Congratulations!");
                state = 2;
                break;
        }
        // in all cases, stop timer...
        setTimerEnabled(false);
    }

    /**
     * Abords the current game. In particular, it sets button labels and titles
     * and stops the timer.
     */
    protected void abortGame() {
        debugMessage(".abortGame");
        setButtonText(button1, "Reset");
        setTitleText(title, "Game aborted!");
        setTimerEnabled(false);
        state = 2;
    }

    /**
     * Resets the game. In particular, it initializes data, sets button labels
     * and titles, stops the timer and arranges tiles.
     */
    protected void resetGame() {
        debugMessage(".resetGame");
        setButtonText(button1, "Start");
        setTitleText(title, "Welcome!");
        setTitleText(subtitle1, "");
        setTitleText(subtitle2, "");
        setTimerEnabled(false);
        for (int i = 0; i != width * height; i++) {
            board[i] = i;
        }
        arrange();
        state = 0;
    }

    /**
     * Handler for player actions (i.e., tile clicks). Applies game logic, sets
     * titles and arranges tiles.
     *
     * @param id id of the tile the player has clicked on
     */
    protected void playerAction(int id) {
        debugMessage(".playerAction");
        int buttonIndex = -1;
        for (int i = 0; i != board.length; i++) {
            if (board[i] == id) {
                buttonIndex = i;
            }
        }
        int freeIndex = checkFree(buttonIndex);
        if (freeIndex != -1) {
            board[buttonIndex] = -1;
            board[freeIndex] = id;
            steps++;
            debugMessage("Steps: " + steps);
            setTitleText(subtitle2, "Steps: " + String.valueOf(steps));
            arrange();
        }
    }

    /**
     * Checks if the current data represent an ordered arrangement of tiles.
     * 
     * @return true if the current data represent an ordered arrangement of
     * tiles (i.e., a solved game), false otherwise
     */
    protected boolean isSuccessful() {
        debugMessage(".isSuccesful");
        boolean result = true;
        for (int i = 0; i != width * height; i++) {
            if (board[i] != -1 && board[i] != i) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Returns the index of the free tile if it is adjacent to the specified
     * tile.
     *
     * @param index the index of the tile to locate the free tile with respect
     * to
     *
     * @return the index of the free tile if it is adjacent to the specified
     * tile, -1 otherwise
     */
    protected int checkFree(int index) {
        debugMessage(".checkFree, index = " + index);
        int above = index >= width ? index - width : -1;
        int below = index < width * (height - 1) ? index + width : -1;
        int left = (index % width != 0) ? index - 1 : -1;
        int right = ((index + 1) % width) != 0 ? index + 1 : -1;
        int result = -1;
        if (above != -1 && board[above] == -1) {
            result = above;
        }
        if (below != -1 && board[below] == -1) {
            result = below;
        }
        if (left != -1 && board[left] == -1) {
            result = left;
        }
        if (right != -1 && board[right] == -1) {
            result = right;
        }
        return result;
    }

    /**
     * Handler for tile and button clicks. Manages the game according to current
     * state and tile or button clicked.
     *
     * @param index the index of the button or tile the user has clicked on
     */
    protected void process(int index) {

        debugMessage(".process, index = " + index);

        switch (index) {

            case -1:
                switch (state) {
                    case 0:
                        // starting a new game...
                        startGame();
                        break;
                    case 1:
                        // aborting a game...
                        // todo: ask player to abandon current game...
                        abortGame();
                        break;
                    case 2:
                        // resetting the game...
                        resetGame();
                        break;
                }
                break;

            case -2:
                // todo: open child browser window to help url...
                break;

            default:
                if (index < width * height) {
                    switch (state) {
                        case 0:
                        case 2:
                            debugMessage("Buttons inactive!");
                            break;
                        case 1:
                            playerAction(index);
                            if (isSuccessful()) {
                                endGame();
                            }
                            break;
                    }
                }
                else {
                    debugMessage("Invalid button id " + index + "!");
                }
                break;
        }
    }

    /*** model management *****************************************************/

    /**
     * Creates a material with the specified parameters.
     *
     * @param scene the current X3D execution context (i.e., scene)
     * @param diffuseColor the material's diffuse color, as an array of three
     * floats corresponding to r, g and b components, each component in [0, 1]
     * @param specularColor the material's specular color, as an array of three
     * floats corresponding to r, g and b components, each component in [0, 1]
     * @param emissiveColor the material's emissive color, as a an array of
     * three floats corresponding to r, g and b components, each component in
     * [0, 1]
     * @param ambientIntensity the material's ambient intensity, as a float in
     * [0, 1]
     * @param transparency the material's transparency, as a float in [0, 1]
     * @param shininess the material's shininess, as a float in [0, 1]
     *
     * @return the material, as an X3DNode
     */
    protected X3DNode createMaterial(
        X3DExecutionContext scene,
        float[] diffuseColor,
        float[] specularColor,
        float[] emissiveColor,
        float ambientIntensity,
        float shininess,
        float transparency)
    {
        debugMessage(".createMaterial");
        X3DNode material = scene.createNode("Material");
        if (diffuseColor != null) {
            ((SFColor) material.getField("diffuseColor")).setValue(diffuseColor);
        }
        if (specularColor != null) {
            ((SFColor) material.getField("specularColor")).setValue(specularColor);
        }
        if (emissiveColor != null) {
            ((SFColor) material.getField("emissiveColor")).setValue(emissiveColor);
        }
        ((SFFloat) material.getField("ambientIntensity")).setValue(ambientIntensity);
        ((SFFloat) material.getField("shininess")).setValue(shininess);
        ((SFFloat) material.getField("transparency")).setValue(transparency);
        return material;
    }

    /**
     * Creates the materials used by XNumbers and places them in the materials
     * map
     *
     * @param scene the current X3D execution context (i.e., scene)
     */
    protected void initializeMaterials(X3DExecutionContext scene) {
        debugMessage(".initializeMaterials");
        materials = new Hashtable<String, X3DNode>();
        materials.put("buttonTile", createMaterial(scene, new float[]{0, 1, 0}, null, null, 1.0f, 1.0f, 0.5f));
        materials.put("buttonGUI", createMaterial(scene, new float[]{0.02f, 0.24f, 0.53f}, new float[]{0.32f, 0.4f, 0.4f}, new float[]{0.01f, 0.12f, 0.27f}, .0333f, 0.54f, 0.5f));
        materials.put("buttonText", createMaterial(scene, null, new float[]{1.0f, 0, 0}, new float[]{1, 0, 0}, 0, 0.192f, 0.0f));
        materials.put("titleText", createMaterial(scene, new float[]{.2f, .37f, .6f}, new float[]{.32f, .4f, .4f}, new float[]{.1f, .19f, .31f}, .05f, .54f, .0f));
        materials.put("bounds", createMaterial(scene, new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}, 0, 0, 0));
    }

    /**
     * Creates a button with the specified parameters and a TouchSensor for the
     * button. Also, adds this class to the TouchSensor's touchTime field
     * listeners
     *
     * @param scene the current X3D execution context (i.e., scene)
     * @param size the button's dimensions, as an array of three floats
     * corresponding to x, y and z components
     * @param translation the button's translation, as an array of three floats
     * corresponding to x, y and z components
     * @param xScale the button's scale in the x axis, as a float
     * @param label the button's label, as a String
     * @param index the button's index, as an int
     * @param materialBox the material for the button's body, as an X3DNode
     * @param materialText the material for the button's text, as an X3DNode
     *
     * @return the button, as an X3DNode
     */
    protected X3DNode createButton(
        X3DExecutionContext scene,
        float[] size,
        float[] translation,
        float xScale,
        String label,
        int index,
        X3DNode materialBox,
        X3DNode materialText)
    {
        debugMessage(".createButton");
        X3DNode touchSensor = scene.createNode("TouchSensor");

        X3DNode button = createButton(scene, size, translation, xScale, label, index, materialBox, materialText, touchSensor);

        ((SFTime) touchSensor.getField("touchTime")).setUserData(new Integer(index));
        touchSensor.getField("touchTime").addX3DEventListener(this);

        return button;
    }

    /**
     * Creates a button with the specified parameters.
     *
     * @param scene the current X3D execution context (i.e., scene)
     * @param size the button's dimensions, as an array of three floats
     * corresponding to x, y and z components
     * @param translation the button's translation, as an array of three floats
     * corresponding to x, y and z components
     * @param xScale the button's scale in the x axis, as a float
     * @param label the button's label, as a String
     * @param index the button's index, as an int
     * @param materialBox the material for the button's body, as an X3DNode
     * @param materialText the material for the button's text, as an X3DNode
     * @param actionNode an arbitrary node to add to the button's structure,
     * usually a TouchSensor so that the button can be clicked
     *
     * @return the button, as an X3DNode
     */
    protected X3DNode createButton(
        X3DExecutionContext scene,
        float[] size,
        float[] translation,
        float xScale,
        String label,
        int index,
        X3DNode materialBox,
        X3DNode materialText,
        X3DNode actionNode)
    {
        debugMessage(".createButton");

        X3DNode transform = scene.createNode("Transform");
        X3DNode shapeBox = scene.createNode("Shape");
        X3DNode shapeText = scene.createNode("Shape");
        X3DNode appearanceBox = scene.createNode("Appearance");
        X3DNode appearanceText = scene.createNode("Appearance");
        X3DNode box = scene.createNode("Box");
        X3DNode transformText = scene.createNode("Transform");
        X3DNode text = scene.createNode("Text");
        X3DNode fontStyle = scene.createNode("FontStyle");

        ((SFVec3f) box.getField("size")).setValue(size);
        ((SFNode) appearanceBox.getField("material")).setValue(materialBox);
        ((SFNode) shapeBox.getField("appearance")).setValue(appearanceBox);
        ((SFNode) shapeBox.getField("geometry")).setValue(box);

        ((MFString) fontStyle.getField("justify")).setValue(1, new String[]{"MIDDLE"});

        ((SFNode) text.getField("fontStyle")).setValue(fontStyle);
        ((MFString) text.getField("string")).setValue(1, new String[]{label});
        ((SFNode) appearanceText.getField("material")).setValue(materialText);
        ((SFNode) shapeText.getField("appearance")).setValue(appearanceText);
        ((SFNode) shapeText.getField("geometry")).setValue(text);

        ((SFVec3f) transformText.getField("translation")).setValue(new float[]{0.0f, 0.5f, 0.0f});
        ((SFVec3f) transformText.getField("scale")).setValue(new float[]{xScale, 1.0f, 1.0f});
        ((MFNode) transformText.getField("children")).setValue(1, new X3DNode[]{shapeText});

        ((SFVec3f) transform.getField("translation")).setValue(translation);

        X3DNode metadata = scene.createNode("MetadataString");
        ((SFString) metadata.getField("name")).setValue("ALT_NAME");
        String[] metadataValues = new String[]{makeNodeName("BUTTON_" + label)};
        ((MFString) metadata.getField("value")).setValue(metadataValues.length, metadataValues);
        ((SFNode) transform.getField("metadata")).setValue(metadata);

        X3DNode[] ch = null;
        if (actionNode != null) {
            ch = new X3DNode[]{shapeBox, transformText, actionNode};
        }
        else {
            ch = new X3DNode[]{shapeBox, transformText};
        }
        ((MFNode) transform.getField("children")).setValue(ch.length, ch);

        return transform;
    }

    /**
     * Creates a title with the specified parameters.
     *
     * @param scene the current X3D execution context (i.e., scene)
     * @param translation the button's translation, as an array of three floats
     * corresponding to x, y and z components
     * @param xScale the button's scale in the x axis, as a float
     * @param altName the title node's name as it will be reflected by the
     * "ALT_NAME" metadatum
     * @param strings the text to be displayed by the title, as an array of
     * String
     * @param size the title font's size, as a float
     * @param material the title's material, as an X3DNode
     *
     * @return the title, as an X3DNode
     */
    protected X3DNode createTitle(
        X3DExecutionContext scene,
        float[] translation,
        float xScale,
        String altName,
        String[] strings,
        float size,
        X3DNode material)
    {
        debugMessage(".createTitle");
        X3DNode transform = scene.createNode("Transform");
        X3DNode shape = scene.createNode("Shape");
        X3DNode appearance = scene.createNode("Appearance");
        X3DNode text = scene.createNode("Text");
        X3DNode fontStyle = scene.createNode("FontStyle");

        ((SFFloat) fontStyle.getField("size")).setValue(size);
        ((MFString) fontStyle.getField("justify")).setValue(1, new String[]{"MIDDLE"});

        X3DNode metadataText = scene.createNode("MetadataString");
        ((SFString) metadataText.getField("name")).setValue("ALT_NAME");
        String[] metadataValuesText = new String[]{makeNodeName("TITLE_" + altName + "_TEXT")};
        ((MFString) metadataText.getField("value")).setValue(metadataValuesText.length, metadataValuesText);
        ((SFNode) text.getField("metadata")).setValue(metadataText);

        ((SFNode) text.getField("fontStyle")).setValue(fontStyle);
        ((MFString) text.getField("string")).setValue(strings.length, strings);
        // ((MFFloat) text.getField("length")).setValue(1, new float[] {1.0f});
        // ((SFFloat) text.getField("maxExtent")).setValue(1.0f);
        ((SFNode) appearance.getField("material")).setValue(material);
        ((SFNode) shape.getField("appearance")).setValue(appearance);
        ((SFNode) shape.getField("geometry")).setValue(text);

        X3DNode metadataTransform = scene.createNode("MetadataString");
        ((SFString) metadataTransform.getField("name")).setValue("ALT_NAME");
        String[] metadataValuesTransform = new String[]{makeNodeName("TITLE_" + altName)};
        ((MFString) metadataTransform.getField("value")).setValue(metadataValuesTransform.length, metadataValuesTransform);
        ((SFNode) transform.getField("metadata")).setValue(metadataTransform);

        ((SFVec3f) transform.getField("translation")).setValue(translation);
        ((MFNode) transform.getField("children")).setValue(1, new X3DNode[]{shape});

        return transform;
    }

    /**
     * Initializes the game's X3D model.
     */
    public void initModel() {

        debugMessage(".initModel");

        X3DExecutionContext scene = browser.getExecutionContext();

        int tileCount = width * height;

        X3DNode[] newChildren = new X3DNode[tileCount + (showBounds ? 7 : 6)];

        timeSensor = scene.createNode("TimeSensor");
        ((SFBool) timeSensor.getField("loop")).setValue(true);
        ((SFBool) timeSensor.getField("enabled")).setValue(false);
        ((SFTime) timeSensor.getField("cycleInterval")).setValue(1.0f);
        ((SFTime) timeSensor.getField("cycleTime")).addX3DEventListener(new X3DFieldEventListener() {

            @Override public void readableFieldChanged(X3DFieldEvent x3DFieldEvent) {
                double elapsedTime = ((SFTime) timeSensor.getField("elapsedTime")).getValue();
                setTitleText(subtitle1, "Game time: " + String.valueOf((int) elapsedTime / 1000) + " secs");
                // also set the steps counter, as a safeguard to ensure that it
                // will always display the correct count regardless of update
                // delays; this should not be here on a release version, look
                // into it further with latest versions of Xj3D and other APIs...
                setTitleText(subtitle2, "Steps: " + String.valueOf(steps));
            }
        });

        title = createTitle(scene, new float[]{0, height * 2 + 1, 0}, 1.0f, "MAIN", new String[]{"Welcome!"}, 1.0f, materials.get("titleText"));
        subtitle1 = createTitle(scene, new float[]{0, height * 2, 0}, 1.0f, "SUB1", new String[]{}, 0.5f, materials.get("titleText"));
        subtitle2 = createTitle(scene, new float[]{0, height * 2 - 0.5f, 0}, 1.0f, "SUB2", new String[]{}, 0.5f, materials.get("titleText"));
        button1 = createButton(scene, new float[]{3.5f, 1.5f, 0.2f}, new float[]{-1.85f, -2, 0}, 1.0f, "Start", -1, materials.get("buttonGUI"), materials.get("buttonText"));
        button2 = createButton(scene, new float[]{3.5f, 1.5f, 0.2f}, new float[]{1.85f, -2, 0}, 1.0f, "Help", -2, materials.get("buttonGUI"), materials.get("buttonText"), null);

        X3DNode helpAnchor = scene.createNode("Anchor");
        ((SFString) helpAnchor.getField("description")).setValue("Click to open XNumbers2 help in a new browser window");
        ((MFString) helpAnchor.getField("parameter")).setValue(1, new String[]{"target=_blank"});

        if (helpURL != null) {
            ((MFString) helpAnchor.getField("url")).setValue(1, new String[]{helpURL});
        }

        ((MFNode) helpAnchor.getField("children")).setValue(1, new X3DNode[]{button2});

        X3DNode bounds = scene.createNode("Shape");
        X3DNode boundsAppearance = scene.createNode("Appearance");
        X3DNode boundsILS = scene.createNode("IndexedLineSet");
        X3DNode boundsCoordinate = scene.createNode("Coordinate");

        float xmin = Math.min(-0.9f - (2.0f * width / 2), -3.6f);
        float ymin = -2 - (1.5f / 2);
        float zmin = 0.2f / 2;
        float xmax = Math.max(0.9f + (2.0f * width / 2), 3.6f);
        float ymax = height * 2 + 1;
        float zmax = -0.2f / 2;
        float[] points = new float[]{
            xmin, ymin, zmin,
            xmax, ymin, zmin,
            xmax, ymax, zmin,
            xmin, ymax, zmin,
            xmin, ymin, zmax,
            xmax, ymin, zmax,
            xmax, ymax, zmax,
            xmin, ymax, zmax
        };
        ((MFVec3f) boundsCoordinate.getField("point")).setValue(points.length / 3, points);
        int[] indices = new int[]{0, 1, 2, 3, 0, -1, 4, 5, 6, 7, 4, -1, 0, 4, -1, 1, 5, -1, 2, 6, -1, 3, 7, -1};
        ((MFInt32) boundsILS.getField("coordIndex")).setValue(indices.length, indices);
        ((SFNode) boundsILS.getField("coord")).setValue(boundsCoordinate);
        ((SFNode) boundsAppearance.getField("material")).setValue(materials.get("bounds"));
        ((SFNode) bounds.getField("appearance")).setValue(boundsAppearance);
        ((SFNode) bounds.getField("geometry")).setValue(boundsILS);

        if (showBounds) {
            newChildren[newChildren.length - 7] = bounds;
        }
        newChildren[newChildren.length - 6] = timeSensor;
        newChildren[newChildren.length - 5] = subtitle2;
        newChildren[newChildren.length - 4] = subtitle1;
        newChildren[newChildren.length - 3] = title;
        newChildren[newChildren.length - 2] = button1;
        newChildren[newChildren.length - 1] = helpAnchor;

        if (tileCount != 0) {
            int[] tmpBoardState = new int[tileCount];
            buttons = new X3DNode[tileCount];
            for (int i = 0; i != height; i++) {
                for (int j = 0; j != width; j++) {
                    float x = j * 2 - width + 1;
                    int index = i * width + j;
                    X3DNode button = createButton(
                        browser.getExecutionContext(),
                        new float[]{1.8f, 1.8f, 0.2f},
                        new float[]{x, (height - 1 - i) * 2, 0},
                        1.0f,
                        String.valueOf(index + 1),
                        index,
                        materials.get("buttonTile"),
                        materials.get("buttonText"));
                    buttons[index] = button;
                    newChildren[index] = button;
                    scene.updateNamedNode(String.valueOf(index + 1), button);
                    tmpBoardState[index] = index;
                }
            }
            boardState.setValue(tmpBoardState.length, tmpBoardState);
        }

        root = scene.createNode("Transform");
        ((MFNode) root.getField("children")).setValue(newChildren.length, newChildren);
        respawn();
        children.setValue(1, new X3DNode[]{root});

        debugMessage("New children: " + newChildren.length);
    }

    /**
     * Respawns according to the currently-selected respawn method.
     */
    public void respawn() {
        debugMessage(".respawn");
        float x;
        float y;
        switch (spawnLogic) {
            case RANDOM:
                x = spawnLocations[0] + (float) Math.random() * (spawnLocations[2] - spawnLocations[0]);
                y = spawnLocations[1] + (float) Math.random() * (spawnLocations[3] - spawnLocations[1]);
                debugMessage("Selected random location (" + x + ", " + y + ")...");
                break;
            case SHUFFLE:
                int i = (int) (Math.random() * (spawnLocations.length / 2));
                debugMessage("Selected location index " + i + " after shuffle...");
                x = spawnLocations[i * 2 + 0];
                y = spawnLocations[i * 2 + 1];
                break;
            case SEQUENTIAL:
                if (++spawnCount >= spawnLocations.length / 2) {
                    spawnCount = 0;
                }
                debugMessage("Selected location index " + spawnCount + " after sequential selection...");
                x = spawnLocations[spawnCount * 2 + 0];
                y = spawnLocations[spawnCount * 2 + 1];
                break;
            case ORIGIN:
            default:
                // a bit redundant, but set the root node's translation to
                // origin just in case...
                debugMessage("Selected origin...");
                x = 0;
                y = 0;
                break;
        }
        ((SFVec3f) root.getField("translation")).setValue(new float[]{x, 0, y});
    }

    /**
     * Arranges tiles according to current data.
     */
    public void arrange() {
        debugMessage(".arrangeButtons");
        for (int i = 0; i != height; i++) {
            for (int j = 0; j != width; j++) {
                float x = j * 2 - width + 1;
                int tileIndex = board[i * width + j];
                if (tileIndex == -1) {
                    ((SFVec3f) buttons[missingTileIndex].getField("scale")).setValue(new float[]{0, 0, 0});
                }
                else {
                    ((SFVec3f) buttons[tileIndex].getField("scale")).setValue(new float[]{1.0f, 1.0f, 1.0f});
                    ((SFVec3f) buttons[tileIndex].getField("translation")).setValue(new float[]{x, (height - 1 - i) * 2, 0});
                }
            }
        }
        boardState.setValue(board.length, board);
    }

    /**
     * Starts/stops the game timer.
     *
     * @param isEnabled the game timer is enabled if true, disabled otherwise
     */
    protected void setTimerEnabled(boolean isEnabled) {
        debugMessage(".setTimerEnabled, isEnabled = " + isEnabled);
        ((SFBool) timeSensor.getField("enabled")).setValue(isEnabled);
    }

    /**
     * Sets the text of a title node.
     *
     * @param title the title node, as an X3DNode
     * @param text the text to set, as a String
     */
    protected void setTitleText(X3DNode title, String text) {
        // debugMessage(".setTitleText");
        X3DNode s = ((MFNode) title.getField("children")).get1Value(0);
        X3DNode t = ((SFNode) s.getField("geometry")).getValue();
        ((MFString) t.getField("string")).setValue(1, new String[]{text});
    }

    /**
     * Sets the text of a button or tile node.
     *
     * @param button the button or tile node, as an X3DNode
     * @param text the text to set, as a String
     */
    protected void setButtonText(X3DNode button, String text) {
        // debugMessage(".setButtonText");
        X3DNode tr = ((MFNode) button.getField("children")).get1Value(1);
        X3DNode s = ((MFNode) tr.getField("children")).get1Value(0);
        X3DNode t = ((SFNode) s.getField("geometry")).getValue();
        ((MFString) t.getField("string")).setValue(1, new String[]{text});
    }

    /*** XNumbers-specific helpers ********************************************/

    /**
     * Generate a prefixed node name using the prefix specified via the
     * "nodePrefix" field.
     *
     * @param name the text to append the prefix to
     *
     * @return a prefixed node name consisting of the prefic speficied via the
     * "nodePrefix" field and the specified text separated by an underscore
     */
    protected String makeNodeName(String name) {
        debugMessage(".makeNodeName");
        return (nodePrefix != null ? nodePrefix + "_" : "") + name;
    }

    /**
     * Dumps the current data on the browser console in a readable format.
     */
    protected void dumpData() {
        debugMessage(".dumpData");
        if (debug) {
            int i = 0;
            int j = 0;
            while (i * width + j != width * height) {
                browser.print(board[i * width + j]);
                j++;
                if (j == width) {
                    browser.println("");
                    j = 0;
                    i++;
                }
                else {
                    browser.print('\t');
                }
            }
        }
    }

    /*** SAI-specific helpers *************************************************/

    /**
     * Displays the specified message on the browser console if the script is
     * running in debug mode according to the value of the "debug" field.
     *
     * @param message the message to display, as a String
     */
    protected void debugMessage(Object message) {
        if (debug) {
            browser.println(message);
        }
    }

    /*** General-purpose helpers **********************************************/

    /**
     * Generates a readable string representation the elements of the specified
     * generic array without applying any kind of formatting.
     *
     * @param array the array to pring, as an ArrayList<?>
     * @param delimeter the delimeter to use between array elements
     * @param addSpaces if true, a space will be added between a delimeter and
     * the subsequent array element
     *
     * @return the generate string representation, as a String
     */
    public static final String printArray(ArrayList<?> array, String delimeter, boolean addSpaces) {
        return printArray(array, delimeter, addSpaces, null);
    }

    /**
     * Generates a readable string representation the elements of the specified
     * generic array using the specified NumberFormat.
     *
     * @param array the array to pring, as an ArrayList<?>
     * @param delimeter the delimeter to use between array elements
     * @param addSpaces if true, a space will be added between a delimeter and
     * the subsequent array element
     * @param numberFormat the NumberFormat to format the numeric elements with
     *
     * @return the generate string representation, as a String
     */
    public static final String printArray(ArrayList<?> array, String delimeter, boolean addSpaces, NumberFormat numberFormat) {
        StringBuffer s = new StringBuffer();
        if (array == null) {
            s.append("<null>");
        }
        else if (array.size() == 0) {
            s.append("<empty>");
        }
        else {
            int i = 0;
            while (i < array.size() - 1) {
                if (numberFormat != null) {
                    s.append(numberFormat.format(array.get(i))).append(delimeter);
                }
                else {
                    s.append(array.get(i)).append(delimeter);
                }
                if (addSpaces) {
                    s.append(' ');
                }
                i++;
            }
            if (numberFormat != null) {
                s.append(numberFormat.format(array.get(i)));
            }
            else {
                s.append(array.get(i));
            }
        }
        return s.toString();
    }

    /**
     * Generates a readable string representation the elements of the specified
     * integer array without applying any kind of formatting.
     *
     * @param array the array to pring, as an int[]
     * @param delimeter the delimeter to use between array elements
     * @param addSpaces if true, a space will be added between a delimeter and
     * the subsequent array element
     *
     * @return the generate string representation, as a String
     */
    public static final String printArray(int[] array, String delimeter, boolean addSpaces) {
        return printArray(array, delimeter, addSpaces, null);
    }

    /**
     * Generates a readable string representation the elements of the specified
     * integer array using the specified NumberFormat.
     *
     * @param array the array to pring, as an int[]
     * @param delimeter the delimeter to use between array elements
     * @param addSpaces if true, a space will be added between a delimeter and
     * the subsequent array element
     * @param numberFormat the NumberFormat to format the numeric elements with
     *
     * @return the generate string representation, as a String
     */
    public static final String printArray(int[] array, String delimeter, boolean addSpaces, NumberFormat numberFormat) {
        StringBuffer s = new StringBuffer();
        if (array == null) {
            s.append("<null>");
        }
        else if (array.length == 0) {
            s.append("<empty>");
        }
        else {
            int i = 0;
            while (i < array.length - 1) {
                if (numberFormat != null) {
                    s.append(numberFormat.format(array[i])).append(delimeter);
                }
                else {
                    s.append(array[i]).append(delimeter);
                }
                if (addSpaces) {
                    s.append(' ');
                }
                i++;
            }
            if (numberFormat != null) {
                s.append(numberFormat.format(array[i]));
            }
            else {
                s.append(array[i]);
            }
        }
        return s.toString();
    }

    /*** Application entry point **********************************************/

    /**
     * A placeholder for an entry point to an executable future version of this
     * script.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        // todo: create and show a frame with a browser component and a
        // dynamically-created scene containing a single Script node whose
        // implementation shall be this class...
    }
}
