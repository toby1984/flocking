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

import java.text.DecimalFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import de.codesourcery.flocking.ui.ControllerWindow;

/**
 * Simulation entry point (main() class).
 * 
 * <p>This class sets up the UI and then
 * enters an infinite loop that advances the simulation
 * by one step and then renders the simulation state (aka 'the world')
 * using the currently active {@link IRenderer}.</p>
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class Main 
{
	// approx. target framerate to be used when vsync is enabled
	// (I use a ScheduledThreadPoolExecutor to generate fixed rate
	// updates but the timer is rather imprecise)
	private static final int TARGET_FPS = 61;

	// flag is only used when vsync is enabled ; indicates that
	// the next frame may be rendered
	private final AtomicBoolean mayRender = new AtomicBoolean(true);

	// indicates whether the simulation loop may run at full speed
	// or only at TARGET_FPS iterations per second
	private volatile boolean vsync = true;

	// tells the main loop/application to terminate
	private volatile boolean terminate = false;

	// current frame number
	final AtomicLong fpsCount = new AtomicLong(0);
	// avg. FPS calculation start time
	final AtomicLong fpsStartTime = new AtomicLong(System.currentTimeMillis());    

	private final Object RENDERER_LOCK  = new Object();

	// GuardedBy( RENDERER_LOCK )
	private IRenderer renderer=new SoftwareRenderer(false); // currently active renderer

	public static void main(String[] args) throws Exception
	{
		new Main().run();
	}

	public Main() {

		final ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r)
			{
				final Thread t = new Thread(r);
				t.setDaemon( true );
				return t;
			}
		};

		// setup VSYNC timer
		final ScheduledExecutorService ex = new ScheduledThreadPoolExecutor(1, threadFactory );
		final Runnable r = new Runnable() {

			@Override
			public void run()
			{
				mayRender.set( true );
			}
		};
		ex.scheduleAtFixedRate( r , 0 , (int) Math.round(1000.0d / TARGET_FPS) , TimeUnit.MILLISECONDS );
	}

	private void run() throws Exception 
	{
		// initialize renderer
		renderer.setup();

		// setup simulation with default parameters
		final SimulationParameters parameters = SimulationParameters.getDefaultParameters();
		final ISimulation simulation = new Simulation( createWorld( parameters ) );

		// show window for adjusting simulation parameters
		final ControllerWindow window = new ControllerWindow( parameters ) 
		{
			@Override
			protected void parametersChanged(SimulationParameters newParams)
			{
				simulation.setSimulationParameters( newParams );
				resetFPSCounter();
			}
			
			private void resetFPSCounter() 
			{
				fpsCount.set(0);
				fpsStartTime.set( System.currentTimeMillis() );
			}			

			@Override
			protected void onDispose() 
			{
				terminate = true;
			}

			@Override
			protected void rendererChanged(boolean useOpenGL)
			{
				synchronized (RENDERER_LOCK) 
				{
					renderer.destroy();

					IRenderer newRenderer = null;
					if ( useOpenGL ) 
					{
						IRenderer tmp = new LWJGLRenderer();
						try {
							tmp.setup();
							System.out.println("Using OpenGL renderer.");
							newRenderer = tmp;
						}
						catch(Exception e) {
							e.printStackTrace();
							System.err.println("Failed to initialize OpenGL renderer.");
						}
					}

					if ( ! useOpenGL || newRenderer == null ) 
					{
						IRenderer tmp = new SoftwareRenderer(false);
						try {
							tmp.setup();
							System.out.println("Using Java2D renderer.");
						} 
						catch (Exception e) {
							e.printStackTrace();
							System.err.println("Failed to initialize Java2D renderer ?");
							System.exit(1);
						}
						newRenderer = tmp;
					}
					renderer = newRenderer;
				}
				resetFPSCounter();
			}

			@Override
			protected void vsyncChanged(boolean vsyncEnabled)
			{
				vsync = vsyncEnabled;
				resetFPSCounter();
			}
		};

		window.setVisible( true );
		
		// enter main loop (does not return until terminate == true )
		mainLoop(simulation);

		// dispose renderer
		renderer.destroy();

		// exiting after using the OpenGL renderer will most likely 
		// trigger a SegFault on Linux .... 
		// see: http://forum.jogamp.org/SIGSEGV-when-closing-JOGL-applications-td895912.html
		// TL;DR It's a JDK bug , AWT/X11 integration 
		System.exit(0);		
	}

	private void mainLoop(final ISimulation simulation) throws Exception 
	{
		final DecimalFormat DF = new DecimalFormat("####0.0#");

		while( ! terminate ) 
		{
			if ( ! vsync || mayRender.compareAndSet( true , false ) ) 
			{
				long time1 = -System.currentTimeMillis();
				World world = simulation.advance();
				time1 += System.currentTimeMillis();


				synchronized ( RENDERER_LOCK ) 
				{
					if ( ! terminate ) 
					{  						
						renderer.render( world );
					}
				}

				if ( ! terminate && (fpsCount.incrementAndGet() % 100 ) == 0 ) 
				{
					System.out.println("Simulation time: "+time1+" ms");
					final double deltaInSeconds = (System.currentTimeMillis()-fpsStartTime.get())/1000.0d;
					final double avgFps = fpsCount.get() / deltaInSeconds;
					synchronized ( RENDERER_LOCK ) {
						renderer.displayTitle( "Avg. FPS: "+DF.format( avgFps ) );
					}
				}
			}
		}
	}

	private World createWorld(SimulationParameters parameters) 
	{
		final World world = new World(parameters); 

		for ( int i = 0 ; i < parameters.populationSize ; i++ ) 
		{
			world.add( Simulation.createRandomBoid( parameters ) );
		}
		return world;
	}
}