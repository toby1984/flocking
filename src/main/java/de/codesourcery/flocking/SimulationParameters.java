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

/**
 * Immutable value object that holds the set of simulation parameters.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class SimulationParameters
{
    private static final SimulationParameters DEFAULTS = new SimulationParameters(10000,2000,5,10,0.33,0.4,0.33,1,20,100, 5000 * 0.1 );
    
    // number of boids to simulate
    public final int populationSize;    
    
    // model coordinates maximum
    // X/Y coordinates are (0,...maximum[
    public final double modelMax;
    
    // max. force 
    public final double maxSteeringForce;
    public final double maxSpeed;
    
    public final double cohesionWeight;
    public final double separationWeight;
    public final double alignmentWeight;
    public final double borderForceWeight;
    
    public final double separationRadius;
    public final double neighbourRadius;
    public final double borderRadius;
    
    public static SimulationParameters getDefaultParameters() {
        return DEFAULTS;
    }

    public SimulationParameters(int populationSize, double modelMax, double maxForce, double maxSpeed,
            double cohesionWeight, double separationWeight, double alignmentWeight, double borderForceWeight,
            double separationRadius, double neightbourRadius, double borderRadius)
    {
        this.populationSize = populationSize;
        this.modelMax = modelMax;
        this.maxSteeringForce = maxForce;
        this.maxSpeed = maxSpeed;
        this.cohesionWeight = cohesionWeight;
        this.separationWeight = separationWeight;
        this.alignmentWeight = alignmentWeight;
        this.borderForceWeight = borderForceWeight;
        this.separationRadius = separationRadius;
        this.neighbourRadius = neightbourRadius;
        this.borderRadius = borderRadius;
    }
}
