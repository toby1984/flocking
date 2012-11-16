package de.codesourcery.flocking;

public interface ISimulation
{
    public World advance();
    
    public void setSimulationParameters(SimulationParameters parameters);
    
}
