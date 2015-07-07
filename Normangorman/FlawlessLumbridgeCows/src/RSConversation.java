import org.osbot.rs07.api.model.Player;
import org.osbot.rs07.api.ui.HeadMessage;
import org.osbot.rs07.script.Script;
import com.google.code.chatterbotapi.*;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class RSConversation {
    Script script;

    int targetPlayerIndex;
    String targetPlayerName;
    String lastTargetMessage;
    long lastMessageTime;
    ChatterBot chatBot;
    ChatterBotSession currentChatSession;

    String chatLog;

    private boolean startedConversation;
    private boolean finishedConversation;


    public RSConversation(Script script, Player targetPlayer) {
        this.script = script;
        targetPlayerIndex = targetPlayer.getIndex();
        targetPlayerName = targetPlayer.getName();
        lastTargetMessage = "";
        chatLog = "";

        startedConversation = false;
        finishedConversation = false;
    }

    public void startConversation(Optional<String> msg) {
        // If msg is given, then we assume that we are initiating conversation in response to a player.
        // If no msg given, then we are starting the conversation with a greeting.
        try {
            chatBot = new ChatterBotFactory().create(ChatterBotType.CLEVERBOT);
            currentChatSession = chatBot.createSession();
        }
        catch (Exception e) {
            script.log("Error in creating chat session.");
            finishedConversation = true;
            throw new RuntimeException(e);
        }

        if (!msg.isPresent()) { // we are starting the conversation
            String[] possibleGreetings = {
                    "Hi " + targetPlayerName + ".",
                    "Hey " + targetPlayerName + ", what's up?",
                    "Yo " + targetPlayerName + ".",
                    "What's up " + targetPlayerName + "?",
                    "Alright there " + targetPlayerName + "?",
                    "How are you " + targetPlayerName + "?"
            };

            String greeting = possibleGreetings[new Random().nextInt(possibleGreetings.length)];
            sendMessage(greeting);
        }
        else { // we are initiating conversation in response to a player.
            respondToMessage(msg.get());
        }

        script.log("Initiating conversation with: " + targetPlayerName);
        startedConversation = true;
        lastMessageTime = System.currentTimeMillis();
    }

    public boolean isFinished() {
        return finishedConversation;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public String getChatLog() {
        return chatLog;
    }

    public void loop() {
        if (!startedConversation) {
            script.warn("CONVERSATION ERROR: called loop on RSConversation before calling startConversation.");
        }

        if (finishedConversation) { return; }

        long timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime;
        if (timeSinceLastMessage > 90 * 1000) { // exit conversation if no response received for 90 seconds.
            finishedConversation = true;
            script.log("CONVERSATION Exiting because target did not respond for a long time.");
            return;
        }

        Player targetPlayer = script.players.getLocalPlayer(targetPlayerIndex);
        if (targetPlayer == null || targetPlayer.getPosition().distance(script.myPosition()) > 25) {
            finishedConversation = true;
        }
        else {
            List<HeadMessage> recentMessages = targetPlayer.getRecentMessages();

            if (recentMessages.size() > 0) {
                String lastMsg = recentMessages.get(recentMessages.size() - 1).getMessage();

                if (lastMsg != lastTargetMessage) {
                    respondToMessage(lastMsg);
                }
            }
        }
    }

    private void sendMessage(String msg) {
        script.log("CONVERSATION - Sending Message: " + msg);
        script.keyboard.typeString(msg);
        chatLog += "Me: " + msg + "\n";
    }

    private void respondToMessage(String msg) {
        lastMessageTime = System.currentTimeMillis();
        lastTargetMessage = msg;
        chatLog += targetPlayerName + ": " + msg + "\n";
        script.log("CONVERSATION - Got response: " + msg);

        String response;
        try {
            response = currentChatSession.think(msg);
        } catch (Exception e) {
            response = "";
            finishedConversation = true;
            script.warn("CONVERSATION ERROR Getting response from CleverBot.");
            throw new RuntimeException(e);
        }

        sendMessage(response);
    }
}

