/**
 * Created by Ben on 02/07/2015.
 */
import org.osbot.rs07.api.Skills;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.EquipmentSlot;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

@ScriptManifest(author="Normangorman", info="", name="Flawless Scripts - Lumbridge Cows", version=3.0, logo="http://i.imgur.com/wzKQf3X.png")
public class CowKillerScript extends Script {
    private final boolean DEBUG = true;
    private final String SCRIPT_TITLE = "Flawless Scripts - Lumbridge Cows";
    private final String FONT_URL = "https://github.com/google/fonts/raw/master/ofl/orbitron/Orbitron-Regular.ttf";

    private boolean settings_configured = false;

    // Fonts
    private Font titleFont;
    private int titleFontSize = 24;
    private Font smallFont;
    private int smallFontSize = 16;

    // Emails:
    public boolean hasEmailEnabled = false;
    public EmailManager emailManager;

    // Conversations:
    public boolean hasConversationsEnabled = true;
    public ConversationManager conversationManager;

    // Skills
    private Skill statToTrain = Skill.STRENGTH;
    private int statGoal = 99;

    private boolean shouldBuryBones = false;
    private int prayerGoal = 99;

    // Script data:
    private Random randomGen = new Random();
    private long startTime = System.currentTimeMillis();
    private NPC target;
    private Position targetPosition;
    private int kill_count = 0;
    private int death_count = 0;
    private CowConstants.CowField currentCowField;

    private enum State {
        INITIAL,
        IDLE,
        RUNNING_TO_COWS,
        PASSING_GATE,
        MOVING_TO_TARGET,
        IN_COMBAT,
        LOOTING,
        BURYING_BONES
    }

    private State current_state = State.INITIAL;

    public void onStart() {
        log("Script is starting...");

        Font baseFont;
        try {
            baseFont = Font.createFont(Font.TRUETYPE_FONT, new URL(FONT_URL).openStream());
            //baseFont = new Font("Century Gothic", Font.PLAIN, 20);
            log("Custom font loaded successfully.");
        } catch (Exception e) {
            log("Could not load custom font.");
            log(e.getMessage());
            baseFont = new Font("serif", Font.PLAIN, 20);
        }

        titleFont = baseFont.deriveFont(Font.PLAIN, titleFontSize);
        smallFont = baseFont.deriveFont(Font.PLAIN, smallFontSize);

        initSettings();
    }

    private void initSettings() {
        JFrame frame = new JFrame("Script Settings");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                frame.dispose();
                CowKillerScript.this.stop();
            }
        });
        frame.setLocationByPlatform(true);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        // COMPONENTS
        // Title
        JLabel title = new JLabel(SCRIPT_TITLE);
        title.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        frame.add(title);

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));

        // Email
        JPanel emailPanel = new JPanel();
        emailPanel.setLayout(new GridBagLayout());
        GridBagConstraints c;

        emailPanel.setBorder(BorderFactory.createTitledBorder("Email"));

        JCheckBox enableEmailCheckbox = new JCheckBox("Enable email progress updates?");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        emailPanel.add(enableEmailCheckbox, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        emailPanel.add(new JLabel("Email address:"), c);

        JTextField emailAddressField = new JTextField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        emailPanel.add(emailAddressField, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        emailPanel.add(new JLabel("Email password:"), c);

        JTextField emailPasswordField = new JPasswordField();
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        emailPanel.add(emailPasswordField, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        emailPanel.add(new JLabel("Email SMTP host:"), c);

        JTextField emailSmtpHostField = new JTextField("smtp.gmail.com");
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        emailPanel.add(emailSmtpHostField, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        emailPanel.add(new JLabel("Email SMTP port:"), c);

        JTextField emailSmtpPortField = new JTextField("587");
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        emailPanel.add(emailSmtpPortField, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        JLabel emailFrequencyLabel = new JLabel("Email frequency (mins): 60");
        emailPanel.add(emailFrequencyLabel, c);

        JSlider emailFrequencySlider = new JSlider(1,300);
        emailFrequencySlider.addChangeListener(event -> {
            emailFrequencyLabel.setText("Email frequency (mins): " + emailFrequencySlider.getValue());
        });
        emailFrequencySlider.setValue(60);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        emailPanel.add(emailFrequencySlider, c);

        innerPanel.add(emailPanel);

        // Conversations
        JPanel conversationsPanel = new JPanel();
        conversationsPanel.setLayout(new BoxLayout(conversationsPanel, BoxLayout.Y_AXIS));

        conversationsPanel.setBorder(BorderFactory.createTitledBorder("Conversations"));

        JCheckBox enableConversationsCheckbox = new JCheckBox("Enable conversations with other players?");
        enableConversationsCheckbox.setSelected(true);
        conversationsPanel.add(enableConversationsCheckbox);

        conversationsPanel.add(new JLabel("Username aliases (enter one per line):"));
        JTextArea usernameAliasesField = new JTextArea(5, 12);
        usernameAliasesField.setToolTipText("These are used to detect when someone is talking to you. For example if your name is BobSlayer1, two aliases might be 'Bob' and 'Slayer'.");
        JScrollPane usernameAliasesScrollPane = new JScrollPane(usernameAliasesField);
        usernameAliasesScrollPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        conversationsPanel.add(usernameAliasesScrollPane);

        innerPanel.add(conversationsPanel);

        // Training
        JPanel trainingPanel = new JPanel();
        trainingPanel.setLayout(new GridBagLayout());

        trainingPanel.setBorder(BorderFactory.createTitledBorder("Training"));

        JLabel trainingStatLabel = new JLabel("Skill to train:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        trainingPanel.add(trainingStatLabel, c);

        String[] trainingStatOptions = {"Attack", "Strength", "Defence"};
        JComboBox<String> trainingStatBox = new JComboBox<>(trainingStatOptions);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        trainingPanel.add(trainingStatBox, c);

        JLabel trainingStatLevelGoalLabel = new JLabel("Skill level goal:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        trainingPanel.add(trainingStatLevelGoalLabel, c);

        JTextField trainingStatLevelGoalField = new JTextField();
        trainingStatLevelGoalField.setText("99");
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        trainingPanel.add(trainingStatLevelGoalField, c);

        JCheckBox buryBonesCheckbox = new JCheckBox("Bury bones?");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        trainingPanel.add(buryBonesCheckbox, c);

        JLabel prayerLevelGoalLabel = new JLabel("Prayer level goal:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        trainingPanel.add(prayerLevelGoalLabel, c);

        JTextField prayerLevelGoalField = new JTextField();
        prayerLevelGoalField.setText("99");
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        trainingPanel.add(prayerLevelGoalField, c);

        innerPanel.add(trainingPanel);

        innerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        frame.add(innerPanel);

        // Start button
        JButton startButton = new JButton("Start");
        // All settings are finalised in this lambda function:
        startButton.addActionListener(event -> {
            // Email
            hasEmailEnabled = enableEmailCheckbox.isSelected();

            if (hasEmailEnabled) {
                String address = emailAddressField.getText();
                String password = emailPasswordField.getText();
                String smtpHost = emailSmtpHostField.getText();
                String smtpPort = emailSmtpPortField.getText();
                int frequency = emailFrequencySlider.getValue();

                log("Email is enabled. Emails will be sent every " + frequency + " mins.");
                emailManager = new EmailManager(this, address, password, smtpHost, smtpPort, frequency, this::getProgressReport);
            }

            // Conversations
            hasConversationsEnabled = enableConversationsCheckbox.isSelected();

            if (hasConversationsEnabled) {
                String[] usernameAliases = usernameAliasesField.getText().split("\n");
                for (String alias : usernameAliases) {
                    log("Got username alias: " + alias);
                }


                conversationManager = new ConversationManager(this, usernameAliases);

                if (hasEmailEnabled) {
                    conversationManager.enableEmail(emailManager);
                }
            }

            // Training
            String statToTrainString = (String) trainingStatBox.getSelectedItem();
            if (statToTrainString.equals("Attack")) {
                statToTrain = Skill.ATTACK;
            } else if (statToTrainString.equals("Strength")) {
                statToTrain = Skill.STRENGTH;
            } else if (statToTrainString.equals("Defence")) {
                statToTrain = Skill.DEFENCE;
            }

            try {
                statGoal = Integer.parseInt(trainingStatLevelGoalField.getText());
                if (statGoal < 1 || statGoal > 99) { statGoal = 99; }
            } catch (NumberFormatException e) {
                statGoal = 99;
            }

            log("Stat goal is " + statGoal + ". Script will stop upon reaching this level.");

            shouldBuryBones = buryBonesCheckbox.isSelected();
            if (shouldBuryBones) {
                try {
                    prayerGoal = Integer.parseInt(prayerLevelGoalField.getText());
                    if (prayerGoal < 1 || prayerGoal > 99) { prayerGoal = 99; }
                } catch (NumberFormatException e) {
                    prayerGoal = 99;
                }
                log("Burying bones is enabled. Prayer goal is: " + prayerGoal);
            }

            settings_configured = true;
            frame.dispose();
        });

        Border startButtonBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED);
        startButton.setBorder(startButtonBorder);
        startButton.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        frame.add(startButton);

        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public void onExit() {
        log("Script ending...");
    }

    @Override
    public int onLoop() throws InterruptedException {
        if (!settings_configured) { return 5; }

        if (hasConversationsEnabled) {
            conversationManager.loop();
        }

        if (hasEmailEnabled) {
            emailManager.loop();
        }

        if (getSkills().getDynamic(statToTrain) >= statGoal) {
            log("Stat goal reached!");

            if (hasEmailEnabled) {
                emailManager.sendEmail("Stat goal of " + statGoal + " " + statToTrain.toString() + " reached. Script stopping.");
            }

            CowKillerScript.this.stop();
        }

        if (shouldBuryBones && getSkills().getDynamic(Skill.PRAYER) >= prayerGoal) {
            shouldBuryBones = false;
            log("Prayer goal reached.");
            emailManager.sendEmail("Reached prayer goal of " + prayerGoal + ". Bones will no longer be buried.");
        }

        // Accept any level up dialogues.
        if (getDialogues().inDialogue()) {
            getDialogues().clickContinue();
        }

        // Run if possible.
        if (getSettings().getRunEnergy() > 5) {
            getSettings().setRunning(true);
        }

        switch(current_state) {
            case INITIAL: // set up equipment, attack style etc. decide which state to enter first.
                log("Beginning initial case.");
                do_initial();
                break;

            case IDLE:
                log("Beginning idle case.");
                do_idle();
                break;

            case RUNNING_TO_COWS:
                log("Beginning running to cows case.");
                do_running_to_cows();
                break;

            case PASSING_GATE:
                log("Beginning passing gate case.");
                do_passing_gate();
                break;

            case MOVING_TO_TARGET:
                log("Beginning moving to target case.");
                do_moving_to_target();
                break;

            case IN_COMBAT:
                log("Beginning attacking case.");
                do_in_combat();
                break;

            case LOOTING:
                log("Beginning looting case.");
                do_looting();
                break;

            case BURYING_BONES:
                log("Beginning burying bones case.");
                do_burying_bones();
                break;
        }

        return random(300, 500);
    }

    @Override
    public void onPaint(java.awt.Graphics2D g2d) {
        super.onPaint(g2d);
        g2d.setColor(Color.YELLOW);

        g2d.setFont(titleFont);
        g2d.drawString(SCRIPT_TITLE, 10, 30 + (int)(titleFontSize / 2));

        g2d.setFont(smallFont);
        int y = 100;
        for (String line : getProgressReport().split("\n")) {
            g2d.drawString(line, 10, y);
            y += smallFontSize;
        }

        if (DEBUG && targetPosition != null) {
            g2d.fillPolygon(targetPosition.getPolygon(bot));
        }
    }

    @Override
    public void log(String msg) {
        if (DEBUG) {
            super.log(msg);
        }
    }

    private void do_initial() {
        getExperienceTracker().start(statToTrain);

        if (shouldBuryBones) {
            getExperienceTracker().start(Skill.PRAYER);
        }

        equipItems();

        if (CowConstants.WEST_FIELD_AREA.contains(myPosition())) {
            currentCowField = CowConstants.CowField.WEST;
            current_state = State.IDLE;
        }
        else if (CowConstants.EAST_FIELD_AREA.contains(myPosition())) {
            currentCowField = CowConstants.CowField.EAST;
            current_state = State.IDLE;
        }
        else {
            // Assume we're in Lumbridge. Randomly select a field.
            currentCowField = getRandomCowField();
            current_state = State.RUNNING_TO_COWS;
        }
    }

    private void do_idle() {
        if (!inCorrectField(myPosition())) {
            // Either I have died and am back in Lumbridge, or have just wandered out of the field.
            if (inLumbridge()) {
                death_count++;
                currentCowField = getRandomCowField();
                current_state = State.RUNNING_TO_COWS;
            }

            current_state = State.RUNNING_TO_COWS;
            return;
        }

        java.util.List<NPC> nearby_npcs = getNpcs().getAll();
        log("Number of NPCs detected: " + Integer.toString(nearby_npcs.size()));

        java.util.List<NPC> out_of_combat_cows = new java.util.ArrayList<>();
        for (NPC npc : nearby_npcs) {
            boolean is_cow = npc.getName().equals("Cow") || npc.getName().equals("Cow calf");
            boolean out_of_combat = !npc.isUnderAttack();
            boolean in_field = inCorrectField(npc.getPosition());

            if (is_cow && out_of_combat && in_field) {
                out_of_combat_cows.add(npc);
            }
        }

        log("Number of out of combat cows detected: " + Integer.toString(out_of_combat_cows.size()));
        if(out_of_combat_cows.size() == 0) {
            log("ERROR: No out of combat cows found. This is probably a bug.");
            return;
        }

        // Target the cow that is closest to the player.
        NPC closest_cow = out_of_combat_cows.get(0);
        int closest_distance = 10000;
        for (NPC cow : out_of_combat_cows) {
            int distance_to_player = myPosition().distance(cow.getPosition());
            if (distance_to_player < closest_distance) {
                closest_cow = cow;
                closest_distance = distance_to_player;
            }
        }
        log("Closest cow found.");

        target = closest_cow;
        targetPosition = closest_cow.getPosition();
        if (!target.isVisible()) {
            camera.toEntity(target);
        }

        log("Attacking closest cow.");
        target.interact("Attack");
        current_state = State.MOVING_TO_TARGET;
    }

    private void do_running_to_cows() {
        equipItems();

        // Choose a random path to the current field and walk it.
        Position[][] random_path_choices;
        if (currentCowField == CowConstants.CowField.WEST) {
            random_path_choices = CowConstants.PATHS_TO_WEST_FIELD;
        }
        else {
            random_path_choices = CowConstants.PATHS_TO_EAST_FIELD;
        }

        Position[] random_path = random_path_choices[randomGen.nextInt(3)];
        getLocalWalker().walkPath(random_path);
        log("Finished walking path.");
        current_state = State.PASSING_GATE;
    }

    private void do_passing_gate() throws InterruptedException {
        log("Attempting to pass gate.");

        RS2Object gate;
        // Keep trying to open until it is open.
        while(!isGateOpen(gate = getObjects().closest("Gate"))) {
            gate.interact("Open");
            sleep(random(100, 200));
        }
        log("Finished gate open sleep.");

        // Walk into the field
        if (currentCowField == CowConstants.CowField.WEST) {
            log("Entering western field.");
            getLocalWalker().walk(CowConstants.WEST_FIELD_INNER_POS);

            if (inWestField(myPosition())) {
                current_state = State.IDLE;
            }
        }
        else {
            log("Entering eastern field.");
            getLocalWalker().walk(CowConstants.EAST_FIELD_INNER_POS);

            if (inEastField(myPosition())) {
                current_state = State.IDLE;
            }
        }
    }

    private void do_moving_to_target() {
        assert(target != null);
        if (!target.isVisible()) {
            camera.toEntity(target);
        }

        // Sleep until the target is in combat. If it gets in combat with someone else - go back to idle, else go to in_combat.
        new ConditionalSleep(5000, 100) {
            @Override
            public boolean condition() throws InterruptedException {
                return !target.exists() || target.isUnderAttack() ;
            }
        }.sleep();

        // Target should now be either non-existent or under attack.
        if (!target.exists()) {
            log("Combat aborted. Target no longer exists.");
            target = null;
            current_state = State.IDLE;
            return;
        }

        boolean am_i_attacking = myPlayer().getInteracting() == target && myPlayer().isAnimating();
        if (!am_i_attacking) {
            log("Combat aborted. Someone else is probably attacking the target.");
            target = null;
            current_state = State.IDLE;
            return;
        }
        else {
            log("Entering combat with target.");
            current_state = State.IN_COMBAT;
        }

    }

    private void do_in_combat() throws InterruptedException {
        if (!target.isVisible()) {
            camera.toEntity(target);
        }

        // Check for death:
        if (inLumbridge()) {
            death_count++;
            currentCowField = getRandomCowField();
            current_state = State.RUNNING_TO_COWS;
            return;
        }

        boolean am_i_attacking = myPlayer().getInteracting() == target;
        if (!target.exists() || !am_i_attacking) {
            log("Killed a cow.");

            if (shouldBuryBones) {
                current_state = State.LOOTING;
            }
            else {
                current_state = State.IDLE;
            }
            kill_count++;
        }
        else { // We're still fighting
            targetPosition = target.getPosition();
        }
    }

    private void do_looting() throws InterruptedException {
        // Check for death:
        if (inLumbridge()) {
            death_count++;
            currentCowField = getRandomCowField();
            current_state = State.RUNNING_TO_COWS;
            return;
        }

        sleep(2000); // wait for loot to appear
        GroundItem bones = groundItems.closest("Bones");

        if (bones != null) {
            log("Bones were found among the cow loot.");
            if (!inventory.isFull()) {
                log("Inventory not full. Attempting to pick bones up.");
                long oldNumBones = inventory.getAmount("Bones");
                long newNumBones;
                boolean pickedUpBones = false;

                for(int attempts=0; attempts < 5; attempts++) {
                    bones.interact("Take");
                    sleep(120);
                    newNumBones = inventory.getAmount("Bones");

                    if (newNumBones > oldNumBones) {
                        pickedUpBones = true;
                        break;
                    }
                }

                if (pickedUpBones) {
                    if (inventory.isFull()) {
                        log("Picked up bones. Inventory now full. Transitioning to burying bones state.");
                        current_state = State.BURYING_BONES;
                    } else {
                        log("Picked up bones. There is still space in my inventory.");
                        current_state = State.IDLE;
                    }
                }
                else {
                    log("Attempt to pick up bones timed out.");
                }
            }
            else {
                log("Did not pick bones up because inventory full. Transitioning to burying bones state.");
                current_state = State.BURYING_BONES;
            }
        }
        else {
            log("Didn't find any bones among the cow loot.");
            current_state = State.IDLE;
        }
    }

    private void do_burying_bones() {
        if (inventory.contains("Bones")) {
            log("There are still bones left in the inventory. Burying them.");
            Item bones = inventory.getItem("Bones");
            bones.interact("Bury");
        }
        else {
            // Sometimes these are picked up accidentally.
            inventory.dropAll("Raw beef");
            inventory.dropAll("Cowhide");
            current_state = State.IDLE;
        }
    }

    private void equipItems() {
        getTabs().open(Tab.INVENTORY);
        Item[] items = getInventory().getItems();

        log("Equipping items.");
        for (Item i : items) {
            if (i == null) {
                continue;
            }

            if (i.hasAction("Wear")) {
                i.interact("Wear");
            } else if (i.hasAction("Wield")) {
                i.interact("Wield");
            } else if (i.hasAction("Equip")) {
                i.interact("Equip");
            }
        }
    }

    private boolean inWestField(Position p) {
        return CowConstants.WEST_FIELD_AREA.contains(p);
    }

    private boolean inEastField(Position p) {
        return CowConstants.EAST_FIELD_AREA.contains(p);
    }

    private boolean inCorrectField(Position p) {
        if (currentCowField == CowConstants.CowField.WEST) {
            return inWestField(p);
        }
        else {
            return inEastField(p);
        }
    }

    private boolean inLumbridge() {
        return myPosition().distance(CowConstants.LUMBRIDGE_RESPAWN_POSITION) < 10);
    }

    private boolean isGateOpen(RS2Object gate) {
         return !Arrays.asList(gate.getDefinition().getActions()).contains("Open");
    }

    private String getProgressReport() {
        String s = "";
        s += "State: " + current_state + "\n";
        s += "Elapsed seconds: " + (int) ((System.currentTimeMillis() - startTime) / 1000) + "\n";
        s += "Current cow field: " + currentCowField.toString() + "\n";
        s += "Kill count: " + kill_count + "\n";
        s += "Death count: " + death_count + "\n";
        s += "Skill being trained: " + statToTrain.toString() + "\n";

        s += "Skill level goal: " + statGoal + "\n";
        s += "XP gained: " + getExperienceTracker().getGainedXP(statToTrain) + "\n";
        s += "Levels gained: " + getExperienceTracker().getGainedLevels(statToTrain) + "\n";

        s += "Burying bones: " + shouldBuryBones + "\n";
        s += "Prayer XP gained: " + getExperienceTracker().getGainedXP(Skill.PRAYER) + "\n";

        int time_to_level_up = (int)(getExperienceTracker().getTimeToLevel(statToTrain) / 1000);
        int mins_to_level_up = (int)Math.floor(time_to_level_up / 60);
        int sec_to_level_up = (int)(time_to_level_up % 60);
        s += String.format("Time to level up: %d mins, %d secs.", mins_to_level_up, sec_to_level_up) + "\n";

        if (hasEmailEnabled) {
            s += "Time til next email update: " + emailManager.getTimeTilNextEmail() + "\n";
        }

        if (hasConversationsEnabled) {
            if (conversationManager.isCurrentlyConversing) {
                s += "Conversation target: " + conversationManager.getCurrentConversationTargetName() + "\n";
            } else {
                s += "Conversation target: -\n";

                int timeTilNextConversation = (int)Math.floor((conversationManager.getNextConversationTime() - System.currentTimeMillis()) / 1000);
                s += "Time til next conversation: " + timeTilNextConversation + "\n";
            }
        }

        return s;
    }

    private CowConstants.CowField getRandomCowField() {
        boolean fieldChoice = randomGen.nextBoolean();

        if (fieldChoice) {
            return CowConstants.CowField.WEST;
        }
        else {
            return CowConstants.CowField.EAST;
        }
    }
}
