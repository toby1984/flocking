package de.codesourcery.flocking;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.flocking.Main.NeighborAggregator;
import de.codesourcery.flocking.World.IBoidVisitor;

public final class SoftwareRenderer extends AbstractRenderer {

    private final Object WORLD_LOCK=new Object();

    private World currentWorld;

    private long frameCounter = 0;
    
    private final JPanel panel = new JPanel() {
        
        private double xInc=1.0;
        private double yInc=1.0;
        
        public void paint(Graphics g) {

            super.paint(g);

            xInc = getWidth() / modelMax;
            yInc = getHeight() / modelMax;

            final Graphics2D graphics = (Graphics2D) g;

            final IBoidVisitor visitor = new IBoidVisitor() {

                private int count = 0;

                @Override
                public void visit(Boid boid)
                {
                    drawBoid( boid ,count == 0 ,graphics );
                    count++;
                }
            };

            g.setColor( Color.BLACK );
            synchronized( WORLD_LOCK ) 
            {
                if ( currentWorld == null ) {
                    return;
                }

                if ( debugPerformance ) 
                {
                    long time = -System.currentTimeMillis();
                    currentWorld.visitAllBoids( visitor );                    
                    time += System.currentTimeMillis();
                    if ( ( frameCounter++ % 60 ) == 0 ) {
                        System.out.println("\n-------------------\nRendering: "+time+" ms\n-------------------");                        
                    }
                } else {
                    currentWorld.visitAllBoids( visitor );
                }
            }
        }
        
        private void drawBoid(Boid boid, boolean firstBoid , Graphics2D g)
        {
            drawBoid(boid,firstBoid,Color.BLUE,true , g);
        }

        private void drawBoid(final Boid boid, boolean isDebugBoid , Color color , boolean fill , final Graphics2D g)
        {
            if ( debug && isDebugBoid ) 
            {
                // draw neighbor radius
                g.setColor(Color.GREEN );
                drawCircle( boid.getNeighbourCenter() , neighborRadius , g );

                // draw separation radius
                g.setColor(Color.RED);
                drawCircle( boid.getNeighbourCenter() , separationRadius , g );  

                // mark neighbors
                final NeighborAggregator visitor = new NeighborAggregator(boid) {

                    @Override
                    public void visit(Boid other)
                    {
                        if ( other != boid ) {
                            super.visit(other);  

                            final double distance = other.getLocation().minus( boid.getNeighbourCenter() ).length();

                            if ( distance > neighborRadius ) {
                                return;
                            }                            
                            drawBoid( other , false , Color.PINK , true, g );
                        }
                    }
                };
                boid.visitNeighbors( currentWorld , neighborRadius , visitor );

                // cohesion
                Vec2dMutable cohesionVec = Main.steerTo( boid , visitor.getAverageLocation() );

                g.setColor(Color.CYAN);
                drawVec( boid.getLocation() , boid.getLocation().plus( cohesionVec ) , g );

                // alignment
                Vec2dMutable alignmentVec = visitor.getAverageVelocity();
                g.setColor(Color.BLUE);
                drawVec( boid.getLocation() , boid.getLocation().plus( alignmentVec ) , g );

                // separation
                Vec2dMutable separationVec = visitor.getAverageSeparationHeading();
                g.setColor(Color.MAGENTA);
                drawVec( boid.getLocation() , boid.getLocation().plus( separationVec ) , g );                
            }

            drawArrow( fill,  boid ,  g );
        }     

        private void drawArrow(boolean fill , Boid b,Graphics2D g) 
        {
            // create vector perpendicular to heading
            double headingNormalizedX = b.getVelocity().x;
            double headingNormalizedY = b.getVelocity().y;

            double d = headingNormalizedX*headingNormalizedX + headingNormalizedY*headingNormalizedY;
            if ( d > 0.00001 ) {
                d = Math.sqrt( d );
                headingNormalizedX = headingNormalizedX / d;
                headingNormalizedY = headingNormalizedY / d;
            }

            // rotate 90 degrees clockwise
            final double rotatedX = headingNormalizedY;
            final double rotatedY = -headingNormalizedX;

            /*      heading
             *        /\
             *        / \
             *       /   \
             *      /     \
             *     /       \
             *    /         \
             * p1 +----+----+ p2
             *        center
             */
            final int[] x=new int[3];
            final int[] y=new int[3];                

            final double centerX = b.getLocation().x;
            final double centerY = b.getLocation().y;

            x[0] = round( (centerX + rotatedX * ARROW_WIDTH) * xInc ); 
            y[0] = round( ( centerY + rotatedY * ARROW_WIDTH ) * yInc );

            x[1]= round( (centerX + headingNormalizedX*ARROW_LENGTH) * xInc );
            y[1] = round( (centerY + headingNormalizedY*ARROW_LENGTH) * yInc );                

            x[2]= round( (centerX + rotatedX * ARROW_WIDTH*-1 )*xInc);
            y[2] = round( (centerY + rotatedY * ARROW_WIDTH*-1 )*yInc);

            if ( fill ) 
            {
                g.fillPolygon( x , y , 3 );
            } else {
                g.drawPolygon( x , y , 3 );
            }                
        }

        private void drawVec(Vec2d src, Vec2d dst, Graphics g) {

            Vec2d direction = dst.minus( src ).normalize();

            Vec2d arrowEnd = src.plus( direction.multiply( 55 ) );
            Vec2d arrowStart = src.plus( direction.multiply( 45 ) );
            drawLine( src , arrowEnd , g );

            // draw arrow head
            Vec2d headBase = direction.multiply(10);
            Vec2d p1 = arrowStart.plus( headBase.rotate90DegreesCCW() );
            Vec2d p2 = arrowStart.minus( headBase.rotate90DegreesCCW() );
            drawPoly( true , g , p1 , arrowEnd , p2 );
        }

        private void drawPoly(boolean fill , Graphics g, Vec2d p1,Vec2d p2,Vec2d p3) 
        {
            final int x[] = new int[3];
            final int y[] = new int[3];

            x[0] = (int) Math.round(p1.x * xInc);
            y[0] = (int) Math.round(p1.y * yInc);

            x[1] = (int) Math.round(p2.x * xInc);
            y[1] = (int) Math.round(p2.y * yInc);

            x[2] = (int) Math.round(p3.x * xInc);
            y[2] = (int) Math.round(p3.y * yInc);            

            if ( fill ) {
                g.fillPolygon( x , y , 3 );
            } else {
                g.drawPolygon( x , y , 3 );
            }
        }         

        private void drawLine(Vec2d p1,Vec2d p2,Graphics g) {

            final int x1 = (int) Math.round(p1.x * xInc);
            final int y1 = (int) Math.round(p1.y * yInc);

            final int x2 = (int) Math.round(p2.x * xInc);
            final int y2 = (int) Math.round(p2.y * yInc);
            g.drawLine( x1,y1,x2,y2);
        }        

        private void drawCircle(Vec2d center, double boidNeightbourRadius, Graphics g)
        {
            final double x1 = (center.x - boidNeightbourRadius)*xInc;
            final double y1 = (center.y - boidNeightbourRadius)*yInc;

            final double x2 = (center.x + boidNeightbourRadius)*xInc;
            final double y2 = (center.y + boidNeightbourRadius)*yInc;            

            g.fillOval( round(x1) , round(y1) , round(x2-x1) , round(y2-y1) ); 
        }        
    };

    public static final double ARROW_WIDTH=10;
    public static final double ARROW_LENGTH=ARROW_WIDTH*3;    
    
    // tick provider
    private final AtomicBoolean mayRender = new AtomicBoolean(true);
    private final ScheduledThreadPoolExecutor vsyncThread;    

    public SoftwareRenderer(double modelMax,boolean debug,
            boolean debugPerformance,
            double neighborRadius, double separationRadius) 
    {
        super(modelMax , debug , debugPerformance , neighborRadius , separationRadius );

        // VSync stuff
        final Runnable r = new Runnable() 
        {
            private int dropCount;
            private int frameCount;
            private int previouslyDroppedFrame=-1;
            
            private final long start = System.currentTimeMillis();
            
            @Override
            public void run()
            {
                frameCount++;
                
                if ( (frameCount % 200) == 0 ) {
                    long duration = System.currentTimeMillis() - start;
                    double fps = (frameCount - dropCount ) / (duration/1000.0);
                    System.out.println("\n-------------------\nFPS: "+fps+"\n-------------------");
                }
                
                if ( ! mayRender.compareAndSet( false , true ) ) 
                {
                    dropCount++;                    
                    if ( previouslyDroppedFrame != frameCount-1 ) 
                    {
                        System.out.println("*** Frames dropped: "+dropCount);                   
                    }
                    previouslyDroppedFrame=frameCount;
                }
            }
        };
        
        vsyncThread = new ScheduledThreadPoolExecutor(1); 
        vsyncThread.scheduleAtFixedRate( r , 0 , 1000 / TARGET_FPS  , TimeUnit.MILLISECONDS );        
    }

    private void repaint(World world) 
    {
        synchronized (WORLD_LOCK) {
            this.currentWorld = world;
        }
        panel.repaint();
    }

    @Override
    public void start(IWorldCallback callback)
    {
        JFrame frame = new JFrame();
        
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(800,600));

        frame.getContentPane().setLayout( new GridBagLayout() );
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.weightx=1.0;
        cnstrs.weighty=1.0;
        cnstrs.gridheight=GridBagConstraints.REMAINDER;
        cnstrs.gridwidth=GridBagConstraints.REMAINDER;
        cnstrs.fill = GridBagConstraints.BOTH;
        
        frame.getContentPane().add( panel , cnstrs );
        frame.pack();

        frame.setVisible(true);   
        
        while(true) 
        {
            if ( mayRender.compareAndSet(true, false ) ) 
            {
                World world;
                try {
                    world = callback.tick();
                    repaint( world );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }    
}