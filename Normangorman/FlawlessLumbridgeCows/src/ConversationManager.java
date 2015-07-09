/**
 * Created by Ben on 06/07/2015.
 */

import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Player;
import org.osbot.rs07.api.ui.Message;
import org.osbot.rs07.script.Script;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class ConversationManager {
    private Script script;

    private String[] usernameAliases;

    public boolean isCurrentlyConversing = false;
    private RSConversation currentConversation;

    private long nextConversationTime = System.currentTimeMillis() + 60000; // 1 minute after script starts.
    private double conversationBreakAverage = 20; // minutes between initiating conversations.
    private double conversationBreakDeviation = 5; // minutes deviation in the average.
    private Random randomGen = new Random();

    private boolean hasEmailEnabled;
    private EmailManager emailManager;

    public ConversationManager(Script s, String[] usernameAliases) {
        script = s;
        this.usernameAliases = usernameAliases;
    }

    public void enableEmail(EmailManager e) {
        hasEmailEnabled = true;
        emailManager = e;
    }

    public void loop() {
        if (!isCurrentlyConversing) {
            // If no one is talking to us and the time of the next conversation has been reached, look for someone to talk to.
            if (System.currentTimeMillis() >= nextConversationTime) {
                lookForConversationTarget();
            }
        }
        else {
            if (currentConversation.isFinished()) {
                script.log("Current conversation is finished.");

                if (hasEmailEnabled) {
                    emailManager.sendEmail(currentConversation.getChatLog());
                }

                isCurrentlyConversing = false;
                currentConversation = null;
                updateNextConversationTime();
            }
            else {
                currentConversation.loop();
            }
        }
    }

    public void handleMessage(Message msg) {
        // Will handle player or trade messages.

        if (msg.getType() == Message.MessageType.RECEIVE_TRADE) {
            script.log("Declining trade request.") ;
            String userName = msg.getUsername();

            String[] possibleResponses = new String[] {
                    "No thanks. I don't want to trade.",
                    "nty, don't want to trade",
                    "don't want to trade thanks",
                    "Sorry " + userName + ", I don't want to trade.",
                    "I'd rather not trade at the minute " + userName + ".",
                    userName + " I don't want to trade with you"
            };

            String response = possibleResponses[new Random().nextInt(possibleResponses.length)];
            script.keyboard.typeString(response);
        }
        else if (msg.getType() == Message.MessageType.PLAYER) {
            if (isCurrentlyConversing) {
                if (msg.getUsername().equals(currentConversation.getTargetPlayerName())) {
                    currentConversation.getMessageFromTarget(msg.getMessage());
                }
            }
            else {
                for (String alias : usernameAliases) {
                    if (msg.getMessage().contains(alias)) {
                        // They are talking to us
                        // Get the player object so a new conversation can be created.
                        String senderName = msg.getUsername();
                        Player sender = script.myPlayer(); // must be non-null for compilation
                        for (Player p : script.players.getAll()) {
                            if (p.getName().equals(senderName)) {
                                sender = p;
                                break;
                            }
                        }

                        if (sender != script.myPlayer()) {
                            startConversation(sender, Optional.of(msg.getMessage()));
                            return;
                        }
                    }
                }
            }
        }
    }

    public String getCurrentConversationTargetName() {
        if (!isCurrentlyConversing) {
            return "";
        }
        else {
            return currentConversation.getTargetPlayerName();
        }
    }

    public long getNextConversationTime() {
        return nextConversationTime;
    }

    private void updateNextConversationTime() {
        double nextConversationDelayMins = conversationBreakAverage + (randomGen.nextDouble() * 2 - 1) * conversationBreakDeviation;
        long nextConversationDelayMillis = (long)Math.floor(nextConversationDelayMins * 60 * 1000);
        nextConversationTime = System.currentTimeMillis() + nextConversationDelayMillis;
    }

    private void startConversation(Player targetPlayer, Optional<String> msg) {
        currentConversation = new RSConversation(script, targetPlayer);
        currentConversation.startConversation(msg);
        isCurrentlyConversing = true;
    }

    // Returns true if a conversation was started, false if not.
    private boolean lookForConversationTarget() {
        String myName = script.myPlayer().getName();
        Position myPosition = script.myPosition();

        // Get the nearest player (provided he is within 10 squares of us)
        List<Player> nearbyPlayers =
                script.players.getAll().stream().
                        filter(p -> p.getName() != myName && myPosition.distance(p.getPosition()) <= 10).
                        sorted(Comparator.comparingInt(p -> myPosition.distance(p.getPosition()))).
                        collect(Collectors.toList());


        if (nearbyPlayers.size() > 0) {
            Player targetPlayer = nearbyPlayers.get(0);

            script.log("Initiating a conversation with someone.");
            startConversation(targetPlayer, Optional.empty());
            return true;
        }

        return false;
    }
}
