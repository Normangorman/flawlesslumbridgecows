/**
 * Created by Ben on 02/07/2015.
 */
import org.osbot.rs07.api.Chatbox;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.ui.Message;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

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
    private Skill trainingSkill = Skill.STRENGTH;
    private int trainingSkillGoal = 99;

    private boolean shouldBuryBones = false;
    private int bonesBuriedCount = 0;
    private int prayerGoal = 99;

    // Script data:
    private Random randomGen = new Random();
    private long startTime = System.currentTimeMillis();
    private int killCount = 0;
    private int deathCount = 0;
    private CowConstants.CowField currentCowField;

    private State currentState = new InitialState(this);

    public void changeState(State newState) {
        log("Leaving '" + currentState.getDescription() + "' and entering '" + newState.getDescription() + "'");
        currentState = newState;
    }

    @Override
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

        // Settings GUI:
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
                trainingSkill = Skill.ATTACK;
            } else if (statToTrainString.equals("Strength")) {
                trainingSkill = Skill.STRENGTH;
            } else if (statToTrainString.equals("Defence")) {
                trainingSkill = Skill.DEFENCE;
            }

            try {
                trainingSkillGoal = Integer.parseInt(trainingStatLevelGoalField.getText());
                if (trainingSkillGoal < 1 || trainingSkillGoal > 99) { trainingSkillGoal = 99; }
            } catch (NumberFormatException e) {
                trainingSkillGoal = 99;
            }

            log("Stat goal is " + trainingSkillGoal + ". Script will stop upon reaching this level.");

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
        if (!settings_configured) { return 100; }

        if (hasConversationsEnabled) {
            conversationManager.loop();
        }

        if (hasEmailEnabled) {
            emailManager.loop();
        }

        if (getSkills().getDynamic(trainingSkill) >= trainingSkillGoal) {
            log("Stat goal reached!");

            if (hasEmailEnabled) {
                emailManager.sendEmail("Stat goal of " + trainingSkillGoal + " " + trainingSkill.toString() + " reached. Script stopping.");
            }

            CowKillerScript.this.stop();
        }

        if (shouldBuryBones && getSkills().getDynamic(Skill.PRAYER) >= prayerGoal) {
            log("Prayer goal reached.");
            shouldBuryBones = false;
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

        currentState.loop();
        return random(300, 500);
    }

    @Override
    public void onPaint(java.awt.Graphics2D g2d) {
        super.onPaint(g2d);

        if (!settings_configured) { return; }

        g2d.setColor(Color.YELLOW);

        int baseX = 10;
        int titleMaxX = 488;
        int titleY = 30 + (int)(titleFontSize / 2);

        int boxPadding = 2;
        int lineSpacing = 2;
        int boxSpacing = 5;
        int boxWidth = 245;

        int box1Y = titleY + 10;
        int box1TextY = box1Y + boxPadding + smallFontSize;
        int box1Lines = 1;
        int box1Height = 2 * boxPadding + (box1Lines - 1) * lineSpacing + box1Lines * smallFontSize;

        int box2Y = box1Y + box1Height + boxSpacing;
        int box2TextY = box2Y + boxPadding + smallFontSize;
        int box2Lines = 5;
        int box2Height = 2 * boxPadding + (box2Lines - 1) * lineSpacing + box2Lines * smallFontSize;

        int box3Y = box2Y + box2Height + boxSpacing;
        int box3TextY = box3Y + boxPadding + smallFontSize;
        int box3Lines = 2;
        int box3Height = 2 * boxPadding + (box3Lines - 1) * lineSpacing + box3Lines * smallFontSize;

        int box4Y = box3Y + box3Height + boxSpacing;
        int box4TextY = box4Y + boxPadding + smallFontSize;
        int box4Lines = 3;
        int box4Height = 2 * boxPadding + (box4Lines - 1) * lineSpacing + box4Lines * smallFontSize;

        Color textColor = g2d.getColor();
        Color boxBorderColor = g2d.getColor();
        Color boxFillColor = new Color(boxBorderColor.getRed(), boxBorderColor.getGreen(), boxBorderColor.getBlue(), 50);

        g2d.setColor(textColor);
        g2d.setFont(titleFont);
        g2d.drawString(SCRIPT_TITLE, baseX, titleY);

        // Box 1: Current state and time elapsed
        g2d.setColor(boxBorderColor);
        g2d.drawRect(baseX, box1Y, titleMaxX - baseX, box1Height);
        g2d.setColor(boxFillColor);
        g2d.fillRect(baseX, box1Y, titleMaxX - baseX, box1Height);

        g2d.setFont(smallFont);
        g2d.setColor(textColor);
        g2d.drawString("State: " + currentState.getDescription(), baseX + boxPadding, box1TextY);
        long timeElapsed = System.currentTimeMillis() - startTime;
        g2d.drawString("Run time: " + millisDurationToString(timeElapsed), (int)((titleMaxX + baseX)/2) + 15, box1TextY);

        // Box 2: Training skill
        int numLines = 5;
        g2d.setColor(boxBorderColor);
        g2d.drawRect(baseX, box2Y, boxWidth, box2Height);
        g2d.setColor(boxFillColor);
        g2d.fillRect(baseX, box2Y, boxWidth, box2Height);

        g2d.setColor(textColor);
        g2d.drawString("Training: " + trainingSkill.toString(), baseX, box2TextY);
        g2d.drawString("Kill count: " + killCount, baseX + boxPadding, box2TextY + lineSpacing + smallFontSize);
        g2d.drawString("XP gained: " + experienceTracker.getGainedXP(trainingSkill),  baseX + boxPadding, box2TextY + lineSpacing*2 + smallFontSize*2);
        g2d.drawString("Levels gained: " + experienceTracker.getGainedLevels(trainingSkill), baseX + boxPadding, box2TextY + lineSpacing*3+ smallFontSize*3);
        g2d.drawString("Level up in: " + millisDurationToString(experienceTracker.getTimeToLevel(trainingSkill)), baseX + boxPadding, box2TextY + lineSpacing*4+ smallFontSize*4);

        // Box 3: Prayer
        numLines = 2;
        g2d.setColor(boxBorderColor);
        g2d.drawRect(baseX, box3Y, boxWidth, box3Height);
        g2d.setColor(boxFillColor);
        g2d.fillRect(baseX, box3Y, boxWidth, box3Height);

        g2d.setColor(textColor);
        g2d.drawString("Bones buried: " + bonesBuriedCount, baseX + boxPadding, box3TextY);
        g2d.drawString("Prayer XP gained: " + experienceTracker.getGainedXP(Skill.PRAYER), baseX + boxPadding, box3TextY + lineSpacing + smallFontSize);

        // Box 4: Conversations & email
        numLines = 3;
        g2d.setColor(boxBorderColor);
        g2d.drawRect(baseX, box4Y, boxWidth, box4Height);
        g2d.setColor(boxFillColor);
        g2d.fillRect(baseX, box4Y, boxWidth, box4Height);

        g2d.setColor(textColor);
        String conversationTarget = "-";
        String timeTilNextConversation = "-";
        if (hasConversationsEnabled) {
            if (conversationManager.isCurrentlyConversing) {
                conversationTarget = conversationManager.getCurrentConversationTargetName();
            }
            else {
                timeTilNextConversation = millisDurationToString(conversationManager.getNextConversationTime() - System.currentTimeMillis());
            }
        }

        String timeTilNextEmail = "-";
        if (hasEmailEnabled) {
            timeTilNextEmail = millisDurationToString(emailManager.getTimeTilNextEmail());
        }
        g2d.drawString("Chat target: " + conversationTarget, baseX + boxPadding, box4TextY);
        g2d.drawString("Auto chat in: " + timeTilNextConversation, baseX + boxPadding, box4TextY + lineSpacing + smallFontSize);
        g2d.drawString("Next email: " + timeTilNextEmail, baseX + boxPadding, box4TextY + lineSpacing*2 + smallFontSize*2);

        currentState.paint(g2d);
    }

    @Override
    public void onMessage(Message msg) {
        if (!settings_configured || currentState == null) { return; }

        if (msg.getType() == Message.MessageType.GAME && msg.getMessage().equals("Oh dear, you are dead!")) {
            // Handle death!settings_configured || currentState  log("I died!");
            randomlySwapCowFields();
            equipItems();
            deathCount++;
            changeState(new RunningToCowsState(this));
            return;
        }
        else if (msg.getType() == Message.MessageType.PLAYER || msg.getType() == Message.MessageType.RECEIVE_TRADE) {
            conversationManager.handleMessage(msg);
        }
        else {
            currentState.handleMessage(msg);
        }
    }

    @Override
    public void log(String msg) {
        if (DEBUG) {
            super.log(msg);
        }
    }

    // GETTERS / SETTERS:
    public Skill getTrainingSkill() {
        return trainingSkill;
    }

    public int getTrainingSkillXP() {
        return skills.getExperience(trainingSkill);
    }

    public boolean getShouldBuryBones() {
        return shouldBuryBones;
    }

    public CowConstants.CowField getCurrentCowField() {
        return currentCowField;
    }

    public void setCurrentCowField(CowConstants.CowField newField) {
        currentCowField = newField;
    }

    // UTILITY METHODS:
    public void logCowKill() {
        log("A cow was killed.");
        killCount++;
    }

    public void logBonesBuried() {
        log("Bones were buried.");
        bonesBuriedCount++;
    }

    public void equipItems() {
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

    public void randomlySwapCowFields() {
        boolean fieldChoice = randomGen.nextBoolean();

        if (fieldChoice) {
            currentCowField = CowConstants.CowField.WEST;
        }
        else {
            currentCowField = CowConstants.CowField.EAST;
        }
    }

    public boolean isWithinWestField(Position p) {
        return CowConstants.WEST_FIELD_AREA.contains(p);
    }

    public boolean isWithinEastField(Position p) {
        return CowConstants.EAST_FIELD_AREA.contains(p);
    }

    public boolean isWithinCurrentField(Position p) {
        if (currentCowField == CowConstants.CowField.WEST) {
            return isWithinWestField(p);
        }
        else {
            return isWithinEastField(p);
        }
    }

    public String millisDurationToString(long d) {
        int totalTimeElapsedSecs = (int)(d / 1000);
        int timeElapsedHours = (int)(Math.floor(totalTimeElapsedSecs / 3600));
        int timeElapsedMins = (int)(Math.floor((totalTimeElapsedSecs - timeElapsedHours * 3600) / 60));
        int timeElapsedSecs = totalTimeElapsedSecs - timeElapsedHours * 3600 - timeElapsedMins * 60;

        return timeElapsedHours + "h " + timeElapsedMins + "m " + timeElapsedSecs + "s";
    }

    private String getProgressReport() {
        String s = "";
        s += "State: " + currentState.getDescription() + "\n";
        s += "Elapsed seconds: " + millisDurationToString(System.currentTimeMillis() - startTime) + "\n";
        s += "Current cow field: " + currentCowField.toString() + "\n";
        s += "Death count: " + deathCount + "\n";
        s += "Training: " + trainingSkill.toString() + "\n";
        s += "Kill count: " + killCount + "\n";
        s += "XP gained: " + experienceTracker.getGainedXP(trainingSkill) + "\n";
        s += "Levels gained: " + experienceTracker.getGainedLevels(trainingSkill) + "\n";
        s += "Level up in: " + millisDurationToString(experienceTracker.getTimeToLevel(trainingSkill)) + "\n";
        s += "Skill level goal: " + trainingSkillGoal + "\n";
        s += "Bones buried: " + bonesBuriedCount + "\n";
        s += "Prayer XP gained: " + experienceTracker.getGainedXP(Skill.PRAYER) + "\n";
        s += "Prayer level goal: " + prayerGoal + "\n";

        String chatTarget = "-";
        String timeTilNextConversation = "-";
        if (hasConversationsEnabled) {
            if (conversationManager.isCurrentlyConversing) {
                chatTarget = conversationManager.getCurrentConversationTargetName();
            } else {
                timeTilNextConversation = millisDurationToString(conversationManager.getNextConversationTime() - System.currentTimeMillis());
            }
        }
        s += "Chat target: " + chatTarget + "\n";
        s += "Auto chat in: " + timeTilNextConversation + "\n";

        String timeTilNextEmail = "-";
        if (hasEmailEnabled) {
            timeTilNextEmail = millisDurationToString(emailManager.getTimeTilNextEmail());
        }

        s += "Next email: " + timeTilNextEmail;

        return s;
    }
}
