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
 * Simulation renderer.
 *
 * <p>Note that the name 'renderer' is a bit mis-leading because 
 * implementations do the actual rendering <b>and</b> also provide
 * the UI element ('window') where the rendered graphics are displayed.</p>
 * <p>This is mostly because of the OpenGL renderer , that does not
 * necessarily require a Swing/AWT window (think: fullscreen-mode).</p>
 * 
 * <p>A renderer's life-cycle has the following three steps:</p>
 * 
 * <ol>
 *   <li>{@link #setup()} - Sets up the renderer and displays the window where output will be rendered.</li>
 *   <li>{@link #render(World)} - Renders the world's state.</li>
 *   <li>{@link #destroy()} - Disposes the renderer's window and any resources it may have aquired. After this method
 *   returns the renderer is no longer in a usable state.</li>
 * </ol>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IRenderer
{
	/**
	 * Boids are rendered as arrows - this constant holds the arrow's width
	 * in model coordinates.
	 */
    public static final double ARROW_WIDTH=10; // arrow width in MODEL coordinates
    
    /**
	 * Boids are rendered as arrows - this constant holds the arrow's length
	 * in model coordinates.     
     */
    public static final double ARROW_LENGTH=ARROW_WIDTH*3; // arrow length in MODEL coordinates      
 
    /**
     * Setup this renderer and show the window graphics will be output to.
     * @throws Exception
     */
    public void setup() throws Exception;
    
    /**
     * Render the simulation's (world's) state.
     * 
     * <p>Note that the {@link World} instance passed to this method
     * must <b>never</b> be changed because the renderer may refer to until 
     * the next call to  {@link #render(World)} or even until {@link #destroy()} is called.</p>
     * 
     * @param world
     * @throws Exception
     */
    public void render(World world) throws Exception;
    
    /**
     * Set the window title.
     * 
     * <p>This method may not have any effect when the renderer uses full-screen mode.</p>
     * 
     * @param title
     */
    public void displayTitle(String title);
    
    /**
     * Dispose this renderer, releasing all aquired resources and closing the associated window (if any).
     * 
     * <p>After this method has been called the renderer is no longer in a usable state.</p>
     */
    public void destroy();
}