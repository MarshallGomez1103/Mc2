package presentation.game;

/** Pure part poses shared by the renderer and tests; coordinates relative to zombie feet. */
public final class ZombiePose {
    public enum Part {
        HEAD("head", 0, 1.575f, 0, .2f, 2.8f, .25f),
        TORSO("torso", 0, 1.025f, 0, -.4f, 1.9f, -.5f),
        LEFT_ARM("leftArm", -.33f, 1.27f, 0, -1.7f, 2.3f, .7f),
        RIGHT_ARM("rightArm", .33f, 1.27f, 0, 1.7f, 2.1f, -.7f),
        LEFT_LEG("leftLeg", -.12f, .70f, 0, -.9f, 1.4f, -1.2f),
        RIGHT_LEG("rightLeg", .12f, .70f, 0, 1.1f, 1.5f, 1.2f);
        public final String node;
        final float x, y, z, vx, vy, vz;
        Part(String node, float x, float y, float z, float vx, float vy, float vz) {
            this.node=node; this.x=x; this.y=y; this.z=z;
            this.vx=vx; this.vy=vy; this.vz=vz;
        }
    }
    public record Pose(float x, float y, float z, float rotationX, float rotationZ, float scale) {}
    private ZombiePose() {}

    public static Pose part(Part part, boolean crawler, boolean shattered, double deadSeconds, float swing) {
        float x=part.x, y=part.y, z=part.z, rotation=0, scale=1;
        if (crawler) {
            switch (part) {
                case HEAD -> { y=.5f; z=.03f; }
                case TORSO -> { y=.38f; z=0; rotation=90; scale=.9f; }
                case LEFT_ARM -> { y=.3f; z=.2f; rotation=-swing*.35f; }
                case RIGHT_ARM -> { y=.3f; z=.2f; rotation=swing*.35f; }
                case LEFT_LEG, RIGHT_LEG -> scale=0;
            }
        } else {
            rotation=switch (part) {
                case LEFT_LEG -> swing;
                case RIGHT_LEG -> -swing;
                case LEFT_ARM -> -swing*.25f;
                case RIGHT_ARM -> swing*.25f;
                default -> 0;
            };
        }
        float tumble=0;
        if (shattered) {
            float time=(float)Math.max(0, Math.min(.8, deadSeconds));
            x += part.vx*time;
            y += part.vy*time - 4.9f*time*time;
            z += part.vz*time;
            rotation += time*180*(part.ordinal()%2==0?1:-1);
            tumble=time*120*(part.vx>0?1:-1);
        }
        return new Pose(x,y,z,rotation,tumble,scale);
    }
}
