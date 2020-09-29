package com.jayfella.pixels.physics;

import com.jayfella.pixels.physics.shape.BoxCollisionShape;
import org.dyn4j.dynamics.BodyFixture;

import java.util.Comparator;

/**
 *
 * @author nickidebruyn
 */
public class BoxCollisionShapeSorter implements Comparator<BodyFixture> {

    @Override
    public int compare(BodyFixture b1, BodyFixture b2) {
        BoxCollisionShape bcs1 = (BoxCollisionShape)b1.getUserData();
        BoxCollisionShape bcs2 = (BoxCollisionShape)b2.getUserData();
        
        if (bcs1.getLocation().y == bcs2.getLocation().y) {
            if (bcs1.getLocation().x > bcs2.getLocation().x) {
                return 1;
            } else if (bcs1.getLocation().x < bcs2.getLocation().x) {
                return -1;
            }            
            
        } else
        if (bcs1.getLocation().y < bcs2.getLocation().y) {
            return 1;            
            
        } else
        
        if (bcs1.getLocation().y > bcs2.getLocation().y) {
            return -1;
        }
        
//        if (bcs1.getLocation().x > bcs2.getLocation().x) {
//            if (bcs1.getLocation().y == bcs2.getLocation().y) {
//                return 1;
//            } else {
//                return -1;
//            }
//            
//            
//        }
//        if (bcs1.getLocation().x < bcs2.getLocation().x) {
//            if (bcs1.getLocation().y == bcs2.getLocation().y) {
//                return 1;
//            } else {
//                return -1;
//            }
//        }
        
        return 0;
    }
    
}
