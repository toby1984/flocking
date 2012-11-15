package de.codesourcery.flocking;


public abstract class AbstractRenderer implements IRenderer {

    protected final double modelMax;

    protected final boolean debug;
    protected final boolean debugPerformance;
    protected final double neighborRadius;
    protected final double separationRadius;
    
    public static final double ARROW_WIDTH=10;
    public static final double ARROW_LENGTH=ARROW_WIDTH*3;    
    
    public AbstractRenderer(double modelMax,boolean debug,boolean debugPerformance,double neighborRadius, double separationRadius) 
    {
        this.modelMax = modelMax;
        this.debug = debug;
        this.debugPerformance = debugPerformance;
        this.neighborRadius = neighborRadius;
        this.separationRadius = separationRadius;   
    }

    protected static final int round(double d) {
        return (int) Math.round(d);
    }

}