package de.codesourcery.flocking;


public abstract class AbstractRenderer implements IRenderer {

    protected final boolean debug;
    protected final boolean debugPerformance;
    
    public AbstractRenderer(boolean debug,boolean debugPerformance) 
    {
        this.debug = debug;
        this.debugPerformance = debugPerformance;
    }
    
    protected static final int round(double d) {
        return (int) Math.round(d);
    }
}