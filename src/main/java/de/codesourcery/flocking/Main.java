package de.codesourcery.flocking;

import java.text.DecimalFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import de.codesourcery.flocking.ui.ControllerWindow;

public class Main 
{
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PERFORMANCE = false;   

    private static final int TARGET_FPS = 61;
    private final AtomicBoolean mayRender = new AtomicBoolean(true);

    private volatile boolean vsync = true;
    
    // FPS counter stuff
    final AtomicLong count = new AtomicLong(0);
    final AtomicLong start = new AtomicLong(System.currentTimeMillis());    

    private final Object RENDERER_LOCK  = new Object();

    // GuardedBy( RENDERER_LOCK )
    private IRenderer renderer=new SoftwareRenderer(DEBUG,DEBUG_PERFORMANCE);

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
    
    private void resetFPSCounter() {
        count.set(0);
        start.set( System.currentTimeMillis() );
    }

    private void run() throws Exception 
    {
        renderer.setup();

        final SimulationParameters parameters = SimulationParameters.getDefaultParameters();
        final ISimulation simulation = new Simulation( createWorld( parameters ) );

        new ControllerWindow( parameters ) 
        {
            @Override
            protected void parametersChanged(SimulationParameters newParams)
            {
                simulation.setSimulationParameters( newParams );
                resetFPSCounter();
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
                        IRenderer tmp = new LWJGLRenderer(DEBUG,DEBUG_PERFORMANCE);
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
                        IRenderer tmp = new SoftwareRenderer(DEBUG,DEBUG_PERFORMANCE);
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

        final DecimalFormat DF = new DecimalFormat("####0.0#");

        while( true ) 
        {
            if ( ! vsync || mayRender.compareAndSet( true , false ) ) 
            {
                long time1 = -System.currentTimeMillis();
                World world = simulation.advance();
                time1 += System.currentTimeMillis();

                synchronized ( RENDERER_LOCK ) {
                    renderer.render( world );
                }

                if ( (count.incrementAndGet() % 100 ) == 0 ) 
                {
                    System.out.println("Simulation time: "+time1+" ms");
                    final double deltaInSeconds = (System.currentTimeMillis()-start.get())/1000.0d;
                    final double avgFps = count.get() / deltaInSeconds;
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