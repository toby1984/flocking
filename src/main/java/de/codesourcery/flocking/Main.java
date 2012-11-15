package de.codesourcery.flocking;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.flocking.World.IBoidVisitor;

public class Main 
{
    protected static final boolean DEBUG = false;

    protected static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    protected static final AtomicLong TICK_COUNTER = new AtomicLong(0);    

    protected static final boolean DEBUG_PERFORMANCE = false;

    protected static final double MAX_FORCE = 5;
    protected static final double MAX_SPEED = 10;     

    protected static final double COHESION_WEIGHT = 0.33d;
    protected static final double SEPARATION_WEIGHT = 0.4d;
    protected static final double ALIGNMENT_WEIGHT = 0.33d;
    protected static final double BORDER_FORCE_WEIGHT = 1d;

    protected static final double  MODEL_MAX = 5000;

    protected static final double SEPARATION_RADIUS = 20;
    protected static final double NEIGHBOUR_RADIUS = 100;
    protected static final double BORDER_RADIUS = MODEL_MAX*0.1;    

    public static final double ARROW_WIDTH=10;
    public static final double ARROW_LENGTH=ARROW_WIDTH*3;

    protected static final int POPULATION_SIZE = 10000;

    private World world;

    private final Random rnd = new Random( 0xdeadbeef );
    private final IRenderer renderer;
    private final ExecutorService threadPool;
    
    public Main(boolean useOpenGL) 
    {
        System.out.println("Using "+THREAD_COUNT+" CPUs.");

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( 400 );

        final ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r)
            {
                final Thread t= new Thread(r);
                t.setDaemon( true );
                return t;
            }
        };
        threadPool = new ThreadPoolExecutor( THREAD_COUNT , THREAD_COUNT , 1 , TimeUnit.MINUTES , queue,threadFactory, new CallerRunsPolicy() );
        
        if ( useOpenGL ) {
            renderer = new LWJGLRenderer(MODEL_MAX,DEBUG,DEBUG_PERFORMANCE,NEIGHBOUR_RADIUS , SEPARATION_RADIUS );
        } else {
            renderer = new SoftwareRenderer(MODEL_MAX,DEBUG,DEBUG_PERFORMANCE,NEIGHBOUR_RADIUS , SEPARATION_RADIUS );
        }        
    }

    public static void main(String[] args) throws Exception
    {
        System.out.println( System.getProperties() );
        
        final boolean useOpenGL = Boolean.parseBoolean( System.getProperty("use.opengl" , "false") );
        
        System.out.println("Args: "+ArrayUtils.toString( args ) );
        if ( useOpenGL ) 
        {
            System.out.println("Using OpenGL renderer.");
        }
        new Main(useOpenGL).run();
    }

    public void run() throws Exception
    {
        renderer.start( new IWorldCallback() {
            
            @Override
            public World tick() throws Exception
            {
                if ( world == null ) {
                    world = createWorld();
                    return world;
                }
                
                long time = -System.currentTimeMillis();
                world = Main.this.tick();
                if ( DEBUG_PERFORMANCE ) 
                {
                    time += System.currentTimeMillis();
                    if ( ( TICK_COUNTER.incrementAndGet() % 10 ) == 0 ) {
                        System.out.println("Calculation: "+time+" ms");
                    }
                }                
                return world;
            }
        });
    }

    private World tick() throws InterruptedException 
    {
    	final int unitCount = THREAD_COUNT*16;
    	
        final CountDownLatch workerThreads = new CountDownLatch( unitCount );

        final World newWorld = new World();

        for ( final ArrayList<Boid> inputList : slice( world.getAllBoids() , unitCount ) ) 
        {
            threadPool.submit( new Runnable() 
            {
                public void run() 
                {
                    try 
                    {
                        for ( Boid boid : inputList ) 
                        {
                            final Vec2dMutable newAcceleration = flock(boid); 

                            final Vec2d newVelocity = boid.getVelocity().plus( newAcceleration ).limit( MAX_SPEED );
                            final Vec2d newLocation = boid.getLocation().plus( newVelocity ).wrapIfNecessary( MODEL_MAX );

                            newWorld.add( new Boid( newLocation , new Vec2d( newAcceleration ) , newVelocity ) );
                        }
                    } finally {
                        workerThreads.countDown();
                    }					
                };
            } );
        }

        // wait for worker threads to finish
        workerThreads.await();

        return newWorld;
    }

    // divide boids into separate lists, each being processed by a different thread      
    private static ArrayList<Boid>[] slice(List<Boid> allBoids, int listCount)
    {
        @SuppressWarnings("unchecked")
        final ArrayList<Boid>[] toProcess = new ArrayList[listCount ];
        final int boidsPerThread = allBoids.size() / listCount;
        
        for ( int i = 0 ;i < listCount ; i++ ) {
            toProcess[i] = new ArrayList<Boid>( boidsPerThread );
        }

        int currentStart = 0;
        for ( int index = 0 ; index < listCount ; index++ , currentStart += boidsPerThread ) {
        	toProcess[index].addAll( allBoids.subList( currentStart , currentStart + boidsPerThread ) );
        }
        if ( currentStart < allBoids.size() ) {
        	toProcess[listCount-1].addAll( allBoids.subList( currentStart , allBoids.size() ) );
        }
        return toProcess;
    }
    
    protected Vec2dMutable flock(Boid boid)
    {
        final NeighborAggregator visitor =new NeighborAggregator( boid );
        boid.visitNeighbors(world , NEIGHBOUR_RADIUS , visitor );

        // cohesion
        Vec2dMutable cohesionVec = steerTo( boid , visitor.getAverageLocation() );

        // alignment
        Vec2dMutable alignmentVec = visitor.getAverageVelocity();

        // separation
        Vec2dMutable separationVec = visitor.getAverageSeparationHeading();

        // border force
        final Vec2d pos = boid.getLocation();

        Vec2dMutable borderForce = new Vec2dMutable();
        if ( pos.x < BORDER_RADIUS ) 
        {
            final double delta = (BORDER_RADIUS-pos.x) / BORDER_RADIUS;
            borderForce.x = delta*delta;
        } else if ( pos.x > ( MODEL_MAX - BORDER_RADIUS ) ) 
        {
            final double delta = (BORDER_RADIUS -( MODEL_MAX - pos.x )) / BORDER_RADIUS;
            borderForce.x = -(delta*delta);
        }

        if ( pos.y < BORDER_RADIUS ) 
        {
            final double delta = (BORDER_RADIUS-pos.y) / BORDER_RADIUS;
            borderForce.y = delta*delta;
        } else if ( pos.y > ( MODEL_MAX - BORDER_RADIUS ) ) 
        {
            final double delta = (BORDER_RADIUS -( MODEL_MAX - pos.y )) / BORDER_RADIUS;
            borderForce.y = -(delta*delta);
        }        

        Vec2dMutable mean = new Vec2dMutable();

        mean.plus( cohesionVec.normalize().multiply( COHESION_WEIGHT ) );        
        mean.plus( alignmentVec.normalize().multiply( ALIGNMENT_WEIGHT ) );        
        mean.plus( separationVec.normalize().multiply( SEPARATION_WEIGHT ) );
        mean.plus( borderForce.multiply( BORDER_FORCE_WEIGHT ) );

        return mean;
    }

    public static Vec2dMutable steerTo(Boid boid , Vec2dMutable target) 
    {
        Vec2dMutable desiredDirection = target.minus( boid.getLocation() );
        final double distance = desiredDirection.length();

        if ( distance > 0 ) 
        {
            desiredDirection.normalize();
            if ( distance < 100 ) 
            {
                desiredDirection.multiply( MAX_SPEED * ( distance/100.0) );
            } else {
                desiredDirection.multiply( MAX_SPEED );
            }

            desiredDirection.minus( boid.getVelocity() );
            desiredDirection.limit( MAX_FORCE );
            return desiredDirection;
        }
        return new Vec2dMutable();
    }

    public static class NeighborAggregator implements IBoidVisitor {

        private final Boid boid;

        private double locationSumX = 0;
        private double locationSumY = 0;

        private double velocitySumX = 0;
        private double velocitySumY = 0;

        private double separationSumX = 0;
        private double separationSumY = 0;

        private int neighbourCount=0;
        private int separationNeighbourCount=0;

        public NeighborAggregator(Boid b) {
            this.boid = b;
        }

        public int getNeighbourCount()
        {
            return neighbourCount;
        }

        @Override
        public void visit(Boid otherBoid)
        {
            if ( boid == otherBoid ) {
                return;
            }

            final double distance = otherBoid.location.distanceTo( boid.getNeighbourCenter() );

            neighbourCount ++;

            locationSumX += otherBoid.location.x;
            locationSumY += otherBoid.location.y;

            velocitySumX += otherBoid.velocity.x;
            velocitySumY += otherBoid.velocity.y;

            if ( distance > 0 && distance < SEPARATION_RADIUS ) 
            {
                double tmpX = boid.getNeighbourCenter().x;
                double tmpY = boid.getNeighbourCenter().y;

                tmpX -= otherBoid.location.x;
                tmpY -= otherBoid.location.y;

                double len = tmpX*tmpX+tmpY*tmpY;
                if ( len > 0.00001 ) {
                    len = Math.sqrt( len );
                    tmpX /= len;
                    tmpY /= len;
                }

                separationSumX += tmpX;
                separationSumY += tmpY;

                separationNeighbourCount++;
            }
        }

        // separation
        public Vec2dMutable getAverageSeparationHeading() 
        {
            if ( separationNeighbourCount == 0 ) {
                return new Vec2dMutable(0,0);
            }
            return new Vec2dMutable( separationSumX / separationNeighbourCount, separationSumY / separationNeighbourCount);
        }        

        public Vec2dMutable getAverageVelocity()  // alignment
        {
            if ( neighbourCount == 0 ) {
                return new Vec2dMutable(0,0);				
            }
            return new Vec2dMutable( velocitySumX / neighbourCount , velocitySumY / neighbourCount );
        }

        public Vec2dMutable getAverageLocation() // cohesion 
        {
            if ( neighbourCount == 0 ) {
                return new Vec2dMutable(0,0);					
            }
            return new Vec2dMutable( locationSumX / neighbourCount, locationSumY / neighbourCount ); 
        }        
    }

    private World createWorld() 
    {
        World world = new World(); 

        for ( int i = 0 ; i < POPULATION_SIZE ; i++ ) 
        {
            final Boid boid = new Boid(createRandomPosition() , createRandomAcceleration(), createRandomVelocity());
            world.add( boid );
        }
        return world;
    }

    private Vec2d createRandomPosition() 
    {
        if ( 1 != 2 ) {
            return new Vec2d(MODEL_MAX/2,MODEL_MAX/2);
        }
        final double x = rnd.nextDouble()* MODEL_MAX;
        final double y = rnd.nextDouble()* MODEL_MAX;
        return new Vec2d(x,y);
    }

    private Vec2d createRandomAcceleration() {

        final double x = (rnd.nextDouble()-0.5)*MAX_FORCE;
        final double y = (rnd.nextDouble()-0.5)*MAX_FORCE;
        return new Vec2d(x,y);
    }

    private Vec2d createRandomVelocity() {
        final double x = (rnd.nextDouble()-0.5)*MAX_SPEED;
        final double y = (rnd.nextDouble()-0.5)*MAX_SPEED;
        return new Vec2d(x,y);
    }    
}