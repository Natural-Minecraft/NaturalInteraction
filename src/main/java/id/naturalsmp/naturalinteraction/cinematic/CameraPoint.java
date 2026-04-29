package id.naturalsmp.naturalinteraction.cinematic;

import org.bukkit.Location;

/**
 * A single point in a cinematic camera path.
 * Defines position, rotation, easing, and duration to stay at this point.
 */
public class CameraPoint {

    private final Location location;
    private final float yaw;
    private final float pitch;
    private final int durationTicks;   // How long the camera stays at this point
    private final EasingType easing;   // How camera moves FROM this point TO the next
    private final String text;         // Optional subtitle text during this segment

    public CameraPoint(Location location, float yaw, float pitch, int durationTicks, EasingType easing, String text) {
        this.location = location;
        this.yaw = yaw;
        this.pitch = pitch;
        this.durationTicks = durationTicks;
        this.easing = easing;
        this.text = text;
    }

    public CameraPoint(Location location, float yaw, float pitch, int durationTicks, EasingType easing) {
        this(location, yaw, pitch, durationTicks, easing, null);
    }

    public Location getLocation()  { return location; }
    public float getYaw()          { return yaw; }
    public float getPitch()        { return pitch; }
    public int getDurationTicks()  { return durationTicks; }
    public EasingType getEasing()  { return easing; }
    public String getText()        { return text; }

    /** Easing types for camera transitions. */
    public enum EasingType {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        SMOOTH,         // Catmull-Rom interpolation
        INSTANT         // Teleport, no transition
    }
}
