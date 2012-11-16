package de.codesourcery.flocking;

import java.util.ArrayList;
import java.util.List;

public final class World
{
    private final KDTree<Boid> tree = new KDTree<Boid>();
    private final List<Boid> allBoids = new ArrayList<>();
    
    private final SimulationParameters simulationParameters;
    
    public World(SimulationParameters simulationParameters) {
        this.simulationParameters = simulationParameters;
    } 
    
    public SimulationParameters getSimulationParameters()
    {
        return simulationParameters;
    }
    
    public void add(Boid boid) 
    {
        synchronized( allBoids ) {
        	allBoids.add( boid );
        }
        
        final Vec2d loc = boid.getLocation();        
        tree.add( loc.x , loc.y , boid );
    }
        
    public interface IBoidVisitor 
    {
        public void visit(Boid boid);
    }
        
    public void visitAllBoids(IBoidVisitor visitor) 
    {
        for ( Boid b : allBoids ) {
            visitor.visit( b );
        }
    }
    
    public void visitBoids(double x , double y , double maxRadius,IBoidVisitor visitor) 
    {
        final List<Boid> closestNeighbours = tree.findApproxNearestNeighbours( x , y , maxRadius , 10 );
        for ( Boid b : closestNeighbours ) {
            visitor.visit( b );
        }
    }
    
    public List<Boid> getAllBoids()
    {
        return allBoids;
    } 
    
    public int getPopulation() {
        return this.allBoids.size();
    }    
    
}
