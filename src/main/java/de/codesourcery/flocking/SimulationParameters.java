package de.codesourcery.flocking;

public final class SimulationParameters
{
    private static final SimulationParameters DEFAULTS = new SimulationParameters(10000,5000,5,10,0.33,0.4,0.33,1,20,100, 5000 * 0.1 );
    
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
