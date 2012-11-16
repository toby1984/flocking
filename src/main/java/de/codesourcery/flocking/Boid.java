package de.codesourcery.flocking;

import de.codesourcery.flocking.Simulation.NeighborAggregator;

public final class Boid
{
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
