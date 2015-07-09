import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.model.NPC;

/**
 * Created by Ben on 08/07/2015.
 */
public class LootingState implements State {
    CowKillerScript script;
    NPC target;

    public LootingState(CowKillerScript s, NPC target) {
        script = s;
        this.target = target;
    }

    public String getDescription() {
        return "Looting items";
    }

    public void loop() {
        GroundItem bones = script.groundItems.closest("Bones");

        if (bones == null) {
            script.log("No bones found. Going back to idle.");
            script.changeState(new IdleState(script));
            return;
        }

        script.log("Bones were found.");
        if (script.inventory.isFull()) {
            script.log("Did not pick bones up because inventory full. Transitioning to burying bones state.");
            script.changeState(new BuryingBonesState(script));
            return;
        }
        else {
            script.log("Inventory not full. Attempting to pick bones up.");
            long oldNumBones = script.inventory.getAmount("Bones");
            long newNumBones;

            bones.interact("Take");

            try {
                script.sleep(1500);
            } catch (InterruptedException e) {
                script.log(e.getMessage());
            }

            newNumBones = script.inventory.getAmount("Bones");

            if (newNumBones > oldNumBones) {
                if (script.inventory.isFull()) {
                    script.log("Picked up bones. Inventory now full. Transitioning to burying bones state.");
                    script.changeState(new BuryingBonesState(script));
                    return;
                } else {
                    script.log("Picked up bones. There is still space in my inventory.");
                    script.changeState(new IdleState(script));
                    return;
                }
            }
            else {
                script.log("Attempt to pick up bones timed out.");
                script.changeState(new IdleState(script));
                return;
            }
        }
    }
}
