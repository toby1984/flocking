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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import de.codesourcery.flocking.World.IBoidVisitor;

public final class Simulation implements ISimulation
{
    private static final Random rnd = new Random(System.currentTimeMillis());
    
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int WORK_UNITS_PER_THREAD = 32;

    private static final boolean DEBUG_TREE_DEPTH = true;

    private long generationCounter=0;
    
    private final Object WORLD_LOCK = new Object();

    // @GuardedBy( WORLD_LOCK )
    private World currentWorld;
    
    // @GuardedBy( WORLD_LOCK )
    private SimulationParameters simulationParameters;

    private final ExecutorService threadPool;
    
    public Simulation(World initialWorld) 
    {
        System.out.println("Using "+THREAD_COUNT+" CPUs.");

        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( THREAD_COUNT*(WORK_UNITS_PER_THREAD+1) );

        final ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r)
            {
                final Thread t= new Thread(r);
                t.setDaemon( true );
                return t;
            }
        };
        this.threadPool = new ThreadPoolExecutor( THREAD_COUNT , THREAD_COUNT , 1 , TimeUnit.MINUTES , queue,threadFactory, new CallerRunsPolicy() );
        
        this.currentWorld = initialWorld;
        this.simulationParameters = initialWorld.getSimulationParameters();
    }

    @Override
    public void setSimulationParameters(SimulationParameters parameters)
    {
        synchronized(WORLD_LOCK) 
        {
            /*
             * Do NOT modify the currentWorld since this reference may have been
             * passed to external code by advance()
             */
            if ( this.simulationParameters.populationSize > parameters.populationSize ) 
            {
                System.out.println("Changing simulation size: "+this.simulationParameters.populationSize+" -> "+parameters.populationSize);
                World newWorld = new World(parameters);
                
                int i = parameters.populationSize;
                for ( Boid b : currentWorld.getAllBoids() ) {
                    newWorld.add( b );
                    i--;
                    if ( i <= 0 ) {
                        break;
                    }
                }
                this.currentWorld = newWorld;
            } 
            else if ( this.simulationParameters.populationSize < parameters.populationSize ) 
            {
                System.out.println("Changing simulation size: "+this.simulationParameters.populationSize+" -> "+parameters.populationSize);
                final int toAdd = parameters.populationSize - this.simulationParameters.populationSize;
                
                final World newWorld = new World(parameters);
                
                for ( Boid b : currentWorld.getAllBoids() ) {
                    newWorld.add( b );
                }
                
                for ( int i = 0 ; i < toAdd ; i++ ) {
                    newWorld.add( createRandomBoid( parameters ) );
                }
                this.currentWorld = newWorld;
            } 
            this.simulationParameters = parameters;
        }
    }
    
    @Override
    public World advance()
    {
        synchronized(WORLD_LOCK) 
        {
            final World newWorld = new World( simulationParameters );

            final int unitCount = THREAD_COUNT*WORK_UNITS_PER_THREAD;
            final CountDownLatch workerThreads = new CountDownLatch( unitCount );        

            for ( final ArrayList<Boid> inputList : slice( currentWorld.getAllBoids() , unitCount ) ) 
            {
                threadPool.submit( new Runnable() 
                {
                    public void run() 
                    {
                        try 
                        {
                            final SimulationParameters parameters = simulationParameters;
                            for ( Boid boid : inputList ) 
                            {
                                final Vec2dMutable newAcceleration = flock(boid,parameters); 

                                final Vec2d newVelocity = boid.getVelocity().plus( newAcceleration ).limit( parameters.maxSpeed );
                                final Vec2d newLocation = boid.getLocation().plus( newVelocity ).wrapIfNecessary( parameters.modelMax );

                                newWorld.add( new Boid( newLocation , new Vec2d( newAcceleration ) , newVelocity ) );
                            }
                        } finally {
                            workerThreads.countDown();
                        }                   
                    };
                } );
            }

            // wait for worker threads to finish
            try {
                workerThreads.await();
            } 
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if ( DEBUG_TREE_DEPTH ) {
        		if ( (generationCounter++ % 30 ) == 0 ) 
        		{
        			newWorld.printTreeDepthDistribution();
        		}
            }
            currentWorld = newWorld;
            return newWorld;
        }
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

    protected Vec2dMutable flock(Boid boid,final SimulationParameters parameters)
    {
        final NeighborAggregator visitor =new NeighborAggregator( boid , parameters.separationRadius);
        boid.visitNeighbors(currentWorld , parameters.neighbourRadius , visitor );

        // cohesion
        Vec2dMutable cohesionVec = steerTo( parameters , boid , visitor.getAverageLocation() );

        // alignment
        Vec2dMutable alignmentVec = visitor.getAverageVelocity();

        // separation
        Vec2dMutable separationVec = visitor.getAverageSeparationHeading();

        // border force
        final Vec2d pos = boid.getLocation();

        Vec2dMutable borderForce = new Vec2dMutable();
        if ( pos.x < parameters.borderRadius ) 
        {
            final double delta = (parameters.borderRadius-pos.x) / parameters.borderRadius;
            borderForce.x = delta*delta;
        } else if ( pos.x > ( parameters.modelMax - parameters.borderRadius ) ) 
        {
            final double delta = (parameters.borderRadius -( parameters.modelMax - pos.x )) / parameters.borderRadius;
            borderForce.x = -(delta*delta);
        }

        if ( pos.y < parameters.borderRadius ) 
        {
            final double delta = (parameters.borderRadius-pos.y) / parameters.borderRadius;
            borderForce.y = delta*delta;
        } else if ( pos.y > ( parameters.modelMax - parameters.borderRadius ) ) 
        {
            final double delta = (parameters.borderRadius -( parameters.modelMax - pos.y )) / parameters.borderRadius;
            borderForce.y = -(delta*delta);
        }        

        Vec2dMutable mean = new Vec2dMutable();

        mean.plus( cohesionVec.normalize().multiply( parameters.cohesionWeight ) );        
        mean.plus( alignmentVec.normalize().multiply( parameters.alignmentWeight ) );        
        mean.plus( separationVec.normalize().multiply( parameters.separationWeight ) );
        mean.plus( borderForce.multiply( parameters.borderForceWeight ) );

        return mean;
    }

    public static Vec2dMutable steerTo(SimulationParameters params, Boid boid , Vec2dMutable target) 
    {
        Vec2dMutable desiredDirection = target.minus( boid.getLocation() );
        final double distance = desiredDirection.length();

        if ( distance > 0 ) 
        {
            desiredDirection.normalize();
            if ( distance < 100 ) 
            {
                desiredDirection.multiply( params.maxSpeed * ( distance/100.0) );
            } else {
                desiredDirection.multiply(  params.maxSpeed );
            }

            desiredDirection.minus( boid.getVelocity() );
            desiredDirection.limit( params.maxSteeringForce );
            return desiredDirection;
        }
        return new Vec2dMutable();
    }

    public static class NeighborAggregator implements IBoidVisitor 
    {
        private final double separationRadius;
        private final Boid boid;

        private double locationSumX = 0;
        private double locationSumY = 0;

        private double velocitySumX = 0;
        private double velocitySumY = 0;

        private double separationSumX = 0;
        private double separationSumY = 0;

        private int neighbourCount=0;
        private int separationNeighbourCount=0;

        public NeighborAggregator(Boid b,double separationRadius) {
            this.boid = b;
            this.separationRadius = separationRadius;
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

            if ( distance > 0 && distance < separationRadius ) 
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
    
    public static Boid createRandomBoid(SimulationParameters parameters) {
        return new Boid(createRandomPosition(parameters) , createRandomAcceleration(parameters), createRandomVelocity(parameters));
    }

    private static Vec2d createRandomPosition(SimulationParameters parameters) 
    {
        final double x = rnd.nextDouble()* parameters.modelMax;
        final double y = rnd.nextDouble()* parameters.modelMax;
        return new Vec2d(x,y);
    }

    private static Vec2d createRandomAcceleration(SimulationParameters parameters) {

        final double x = (rnd.nextDouble()-0.5)*parameters.maxSteeringForce;
        final double y = (rnd.nextDouble()-0.5)*parameters.maxSteeringForce;
        return new Vec2d(x,y);
    }

    private static Vec2d createRandomVelocity(SimulationParameters parameters) {
        final double x = (rnd.nextDouble()-0.5)*parameters.maxSpeed;
        final double y = (rnd.nextDouble()-0.5)*parameters.maxSpeed;
        return new Vec2d(x,y);
    }    
}