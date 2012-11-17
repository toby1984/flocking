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

import java.util.ArrayList;
import java.util.List;

import de.codesourcery.flocking.KDTree.ValueVisitor;

/**
 * Simulation state.
 *
 * <p>This class holds a simulation state along with the simulation
 * parameters that were used when creating it.</p>
 * 
 * <p>This class is <b>not</b> thread-safe except for the {@link #add(Boid)} method.</p>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public final class World
{
    private final KDTree<Boid> tree = new KDTree<Boid>();
    
    // separate list to keep track of all boids that have been added to
    // the kd-tree , required because traversing the tree to collect
    // all boids is too slow
    private final List<Boid> allBoids = new ArrayList<>();
    
    private final SimulationParameters simulationParameters;
    
    public World(SimulationParameters simulationParameters) {
        this.simulationParameters = simulationParameters;
    } 
    
    public SimulationParameters getSimulationParameters()
    {
        return simulationParameters;
    }
    
    /**
     * Add a boid to this world.
     * 
     * <p>This method is thread-safe</p>.
     * 
     * @param boid
     */
    public void add(Boid boid) 
    {
        synchronized( allBoids ) {
        	allBoids.add( boid );
        }
        
        final Vec2d loc = boid.getLocation();        
        tree.add( loc.x , loc.y , boid );
    }
        
    public interface IBoidVisitor extends ValueVisitor<Boid>
    {
        public void visit(Boid boid);
    }
    
    /**
     * Visits all boids in this simulation state.
     * 
     * @param visitor
     */
    public void visitAllBoids(IBoidVisitor visitor) 
    {
        for ( Boid b : allBoids ) {
            visitor.visit( b );
        }
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param maxRadius
     * @param visitor
     */
    public void visitBoids(double x , double y , double maxRadius,IBoidVisitor visitor) 
    {
        tree.visitApproxNearestNeighbours( x , y , maxRadius , 10 , visitor );
    }
    
    /**
     * Returns all boids in this simulation state.
     * 
     * @return
     */
    public List<Boid> getAllBoids()
    {
        return allBoids;
    } 
    
    /**
     * Returns the number of boids in this simulations state.
     * 
     * @return
     */
    public int getPopulationCount() {
        return this.allBoids.size();
    }    
}