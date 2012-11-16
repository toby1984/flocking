package de.codesourcery.flocking;

public interface IRenderer
{
    public static final double ARROW_WIDTH=10; // arrow width in MODEL coordinates
    public static final double ARROW_LENGTH=ARROW_WIDTH*3; // arrow length in MODEL coordinates      
    
    public void setup() throws Exception;
    
    public void render(World world) throws Exception;
    
    public void displayTitle(String title);
    
    public void destroy();
}
