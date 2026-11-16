package presentation.game;

import application.PlayerFrameInput;

/** Retains one-shot controls across render frames until the next network tick. */
public final class NetworkInputBuffer {
    private PlayerFrameInput latest = PlayerFrameInput.NEUTRAL;
    private boolean jump, primary, secondary, toggle;
    private float yaw, pitch;
    private domain.block.BlockType selected;
    public void add(PlayerFrameInput frame) {
        latest = frame; jump |= frame.jumpRequested(); primary |= frame.primaryAction(); secondary |= frame.secondaryAction();
        toggle ^= frame.togglePistol(); yaw += frame.deltaYaw(); pitch += frame.deltaPitch();
        if (frame.selectedType()!=null) selected = frame.selectedType();
    }
    public PlayerFrameInput take() {
        float dy = Math.max(-360, Math.min(360,yaw)), dp = Math.max(-180, Math.min(180,pitch));
        var result = new PlayerFrameInput(latest.movement(),jump,dy,dp,primary,secondary,toggle,selected);
        yaw -= dy; pitch -= dp; jump = primary = secondary = toggle = false; selected = null;
        return result;
    }
    public void clear() { latest=PlayerFrameInput.NEUTRAL; jump=primary=secondary=toggle=false; yaw=pitch=0; selected=null; }
}
