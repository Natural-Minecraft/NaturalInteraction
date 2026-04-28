package id.naturalsmp.naturalinteraction.cinematic;

import java.util.ArrayList;
import java.util.List;

/**
 * A cinematic sequence — a named set of camera points that play in order.
 * Can be triggered via /ni cinematic start <id> [player].
 */
public class CinematicSequence {

    private final String id;
    private final List<CameraPoint> points;
    private boolean loop = false;           // Loop back to start after last point
    private boolean lockPlayerMovement = true;
    private boolean hideHUD = true;

    public CinematicSequence(String id) {
        this.id = id;
        this.points = new ArrayList<>();
    }

    public CinematicSequence(String id, List<CameraPoint> points) {
        this.id = id;
        this.points = new ArrayList<>(points);
    }

    public String getId()                       { return id; }
    public List<CameraPoint> getPoints()        { return points; }
    public boolean isLoop()                     { return loop; }
    public boolean isLockPlayerMovement()       { return lockPlayerMovement; }
    public boolean isHideHUD()                  { return hideHUD; }

    public void setLoop(boolean loop)                         { this.loop = loop; }
    public void setLockPlayerMovement(boolean lock)           { this.lockPlayerMovement = lock; }
    public void setHideHUD(boolean hideHUD)                   { this.hideHUD = hideHUD; }

    public void addPoint(CameraPoint point) { points.add(point); }

    /** Total duration of the sequence in ticks. */
    public int getTotalDurationTicks() {
        return points.stream().mapToInt(CameraPoint::getDurationTicks).sum();
    }
}
