package kaptainwutax.tungsten.helpers;

import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.agent.Agent;
import kaptainwutax.tungsten.render.Color;
import kaptainwutax.tungsten.render.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Helper class to easily calculate distance.
 */
public class DistanceCalculator {
	
	/**
     * Calculates the horizontal Euclidean distance from startPos to endPos.
     * 
     * @param startPos start position
     * @param endPos end position
     * @return horizontal Euclidean distance from startPos to endPos.
     */
	public static double getHorizontalEuclideanDistance(BlockPos startPos, BlockPos endPos) {
		return getHorizontalEuclideanDistance(new Vec3(startPos.getX(), startPos.getY(), startPos.getZ()), new Vec3(endPos.getX(), endPos.getY(), endPos.getZ()));
	}
	
	/**
     * Calculates the horizontal Euclidean distance from startPos to endPos.
     * 
     * @param startPos start position
     * @param endPos end position
     * @return horizontal Manhattan distance from startPos to endPos.
     */
	public static double getHorizontalEuclideanDistance(Vec3 startPos, Vec3 endPos) {
		double dx = endPos.x() - startPos.x();
    	double dz = endPos.z() - startPos.z();
    	return Math.sqrt(dx * dx + dz * dz);
	}
	

	public static double getEuclideanDistance(Vec3 startPos, Vec3 endPos) {
		double dx = endPos.x() - startPos.x();
    	double dy = endPos.y() - startPos.y();
    	double dz = endPos.z() - startPos.z();
    	return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	/**
     * Calculates the horizontal Manhattan distance from startPos to endPos.
     * 
     * @param startPos start position
     * @param endPos end position
     * @return horizontal Manhattan distance from startPos to endPos.
     */
	public static double getHorizontalManhattanDistance(BlockPos startPos, BlockPos endPos) {
		return getHorizontalManhattanDistance(new Vec3(startPos.getX(), startPos.getY(), startPos.getZ()), new Vec3(endPos.getX(), endPos.getY(), endPos.getZ()));
	}
	
	/**
     * Calculates the horizontal Manhattan distance from startPos to endPos.
     * 
     * @param startPos start position
     * @param endPos end position
     * @return horizontal Manhattan distance from startPos to endPos.
     */
	public static double getHorizontalManhattanDistance(Vec3 startPos, Vec3 endPos) {
		double dx = endPos.x() - startPos.x();
    	double dz = endPos.z() - startPos.z();
    	// ⛔ FIXED 2026-09-05: Manhattan distance is |dx| + |dz|, not dx + dz. As written this
    	// could go NEGATIVE whenever dx and dz share a sign (endPos both west AND north of
    	// startPos, or both east AND south) -- both live callers use this as a `<=`/`<` threshold
    	// gate (Node.java:149's ClimbALadderMove trigger at <=0.5, BlockSpacePathFinder.java:590's
    	// heuristic dy weighting at <32), so a negative result made the threshold check ALWAYS
    	// pass regardless of true distance in that quadrant, and a same-sign-but-positive result
    	// (impossible here since dx,dz share sign only when at least one is negative) was never
    	// the concern -- the real gap was the sign-cancellation/negative case.
    	return Math.abs(dx) + Math.abs(dz);
	}
	
	/**
     * Calculates the distance to the edge of the block the player is standing on in the direction they're looking.
     * 
     * @param player the player entity
     * @return distance to the edge of the block in the look direction
     */
    public static double getDistanceToEdge(Agent agent) {
        Vec3 position = agent.getPos(); // Get player's position
        BlockPos blockPos = agent.getBlockPos(); // Block the player is standing on
		float f = agent.pitch * (float) (Math.PI / 180.0);
		float g = -agent.yaw * (float) (Math.PI / 180.0);
		float h = Mth.cos(g);
		float i = Mth.sin(g);
		float j = Mth.cos(f);
		float k = Mth.sin(f);
		Vec3 lookDirection =  new Vec3((double)(i * j), (double)(-k), (double)(h * j)); // Direction player is looking

        // Determine block bounds relative to the player's position
        double deltaX = position.x - blockPos.getX();
        double deltaZ = position.z - blockPos.getZ();

        // Calculate edge distance based on facing direction
        double distance = 0;
        if (Math.abs(lookDirection.x) > Math.abs(lookDirection.z)) {
            // Looking more along the X axis
            if (lookDirection.x > 0) {
                distance = 1.0 - deltaX; // Distance to east edge
            } else {
                distance = deltaX; // Distance to west edge
            }
        } else {
            // Looking more along the Z axis
            if (lookDirection.z > 0) {
                distance = 1.0 - deltaZ; // Distance to south edge
            } else {
                distance = deltaZ; // Distance to north edge
            }
        }

        return Mth.clamp(distance, 0.0, 1.0); // Ensure distance is within block bounds
    }
    
    /**
	 * Returns jump height.
	 *
	 * <p>⛔ SIMPLIFIED 2026-09-04, DEAD BRANCHES REMOVED: same redundant pattern found and
	 * fixed in {@code BlockNode.getJumpHeight} (see that method's javadoc for the exhaustive
	 * up/down/equal case analysis) -- {@code diff > 0} is exactly {@code to > from}, so the
	 * {@code diff * -1} arm in each branch can never execute. Zero behavior change.
	 *
	 * @param from
	 * @param to
	 * @return positive is going up and negative is going down
	 */
	public static double getJumpHeight(double from, double to) {
		return to - from;
	}

    /**
	 * Returns jump height.
	 *
	 * <p>⛔ SIMPLIFIED 2026-09-04, DEAD BRANCHES REMOVED: same fix as the {@code double}
	 * overload above.
	 *
	 * @param from
	 * @param to
	 * @return positive is going up and negative is going down
	 */
	public static int getJumpHeight(int from, int to) {
		return to - from;
	}
}
