package presentation.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ZombiePoseTest {
    @Test void crawlerHidesLegsAndPlacesHeadAndTorsoBelowItsRealHeight() {
        for (ZombiePose.Part leg : new ZombiePose.Part[] {ZombiePose.Part.LEFT_LEG, ZombiePose.Part.RIGHT_LEG}) {
            assertEquals(0, ZombiePose.part(leg,true,false,0,25).scale());
        }
        var head=ZombiePose.part(ZombiePose.Part.HEAD,true,false,0,0);
        assertTrue(head.y()+.225f <= .8f);
        assertTrue(Math.abs(head.z())+.225f <= domain.enemy.Zombie.WIDTH/2);
        var torso=ZombiePose.part(ZombiePose.Part.TORSO,true,false,0,0);
        assertEquals(90,torso.rotationX());
        assertTrue(Math.abs(torso.z())+.325f*torso.scale() <= domain.enemy.Zombie.WIDTH/2);
        assertTrue(torso.y()+.14f <= .8f);
        var left=ZombiePose.part(ZombiePose.Part.LEFT_ARM,true,false,0,25);
        var right=ZombiePose.part(ZombiePose.Part.RIGHT_ARM,true,false,0,25);
        assertEquals(-left.rotationX(),right.rotationX());
    }
    @Test void shatteredPartsMoveApartAndTumbleInsteadOfScalingWholeBody() {
        var left=ZombiePose.part(ZombiePose.Part.LEFT_ARM,false,true,.4,0);
        var right=ZombiePose.part(ZombiePose.Part.RIGHT_ARM,false,true,.4,0);
        assertTrue(right.x()-left.x()>1.5f);
        assertNotEquals(0,left.rotationZ());
        assertEquals(1,left.scale());
        assertEquals(1,right.scale());
        var start=ZombiePose.part(ZombiePose.Part.HEAD,false,true,0,0);
        var moved=ZombiePose.part(ZombiePose.Part.HEAD,false,true,.4,0);
        assertNotEquals(start.y(),moved.y());
        assertNotEquals(start.z(),moved.z());
    }
    @Test void ordinaryPoseRemainsFullHeightAndArticulated() {
        var head=ZombiePose.part(ZombiePose.Part.HEAD,false,false,0,25);
        assertEquals(1.575f,head.y());
        assertEquals(1,head.scale());
        assertEquals(25,ZombiePose.part(ZombiePose.Part.LEFT_LEG,false,false,0,25).rotationX());
        assertEquals(-25,ZombiePose.part(ZombiePose.Part.RIGHT_LEG,false,false,0,25).rotationX());
    }
}
