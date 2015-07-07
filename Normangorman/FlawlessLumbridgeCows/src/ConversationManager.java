/**
 * Created by Ben on 06/07/2015.
 */

import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Player;
import org.osbot.rs07.script.Script;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class ConversationManager {
    private Script script;

    private String[] usernameAliases = {"normangorman", "norman", "norm", "gorm", "gorman", "normy", "gormy"};

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

    public void loop() {
        if (!isCurrentlyConversing) {
            lookForConversationTarget();
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

    public void enableEmail(EmailManager e) {
        hasEmailEnabled = true;
        emailManager = e;
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

        // If someone is directly talking to us, then respond to them and engage in conversation.
        java.util.List<Player> talkingPlayers =
                script.players.getAll().stream().
                        filter(p -> p.getHeadMessage() != null).
                        collect(Collectors.toList());

        boolean foundTargetPlayer = false;
        Player targetPlayer = script.myPlayer(); // targetPlayer must be non-null for compilation to succeed.
        // Check if anyone has mentioned one of our username aliases in a chat message.
        for (Player p : talkingPlayers) {
            String msg = p.getHeadMessage().toLowerCase(); // username aliases should all be lower case.
            boolean refersToPlayer = false;

            for (String alias : usernameAliases) {
                if (msg.contains(alias)) {
                    refersToPlayer = true;
                    break;
                }
            }

            if (refersToPlayer) {
                foundTargetPlayer = true;
                targetPlayer = p;
                break;
            }
        }

        if (foundTargetPlayer) {
            script.log("Someone is talking to me! Initiating conversation with them.");
            startConversation(targetPlayer, Optional.of(targetPlayer.getHeadMessage()));
            return true;
        }


        // If no one is talking to us and the time of the next conversation has been reached, look for someone to talk to.
        if (System.currentTimeMillis() >= nextConversationTime) {
            String myName = script.myPlayer().getName();
            Position myPosition = script.myPosition();

            // Get the nearest player (provided he is within 10 squares of us)
            List<Player> nearbyPlayers =
                    script.players.getAll().stream().
                            filter(p -> p.getName() != myName && myPosition.distance(p.getPosition()) <= 10).
                            sorted(Comparator.comparingInt(p -> myPosition.distance(p.getPosition()))).
                            collect(Collectors.toList());


            if (nearbyPlayers.size() > 0) {
                targetPlayer = nearbyPlayers.get(0);

                script.log("Initiating a conversation with someone.");
                startConversation(targetPlayer, Optional.empty());
                return true;
            }
        }

        return false;
    }
}
