/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.flocking;

import de.codesourcery.flocking.Simulation.NeighborAggregator;

/**
 * Simulated entity (aka 'boid').
 * 
 * <p>To make multi-threaded programming less error-prone , instances of this class are <b>immutable</b>.</p> 
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class Boid
{
	// class NEEDS to be immutable, other code relies on that fact
	
    public final Vec2d acceleration;
    public final Vec2d location;
    public final Vec2d velocity;    
    
    public Boid(Vec2dMutable location, Vec2dMutable acceleration,Vec2dMutable velocity)
    {
    	this.acceleration = new Vec2d( acceleration );
    	this.location = new Vec2d( location );
    	this.velocity = new Vec2d( velocity );
    }
    
    public Boid(Vec2d position, Vec2d acceleration,Vec2d velocity)
    {
        this.location = position;
        this.acceleration = acceleration;
        this.velocity = velocity;
    }
    
    public Vec2d getVelocity()
    {
        return velocity;
    }
    
    public Vec2d getLocation()
    {
        return location;
    }
    
    public Vec2d getAcceleration()
    {
        return acceleration;
    }

    public Vec2d getNeighbourCenter() 
    {
       return location;
    }
    
    public void visitNeighbors(World world , double neighborRadius,NeighborAggregator visitor) 
    {
        final Vec2d pos = getNeighbourCenter();
        world.visitBoids( pos.x , pos.y , neighborRadius  , visitor );
    }
}
