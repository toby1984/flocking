package de.codesourcery.flocking;

public interface IRenderer
{
    public static final int TARGET_FPS=30;
    
    public void start(IWorldCallback callback);
}
