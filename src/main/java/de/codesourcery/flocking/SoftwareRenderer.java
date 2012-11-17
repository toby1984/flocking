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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.flocking.Simulation.NeighborAggregator;
import de.codesourcery.flocking.World.IBoidVisitor;

/**
 * Simulation renderer that uses Java2D for rendering.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class SoftwareRenderer implements IRenderer {

    private static final Color BOID_COLOR = new Color(0.5f, 0.5f, 1.0f);

    private final JFrame frame = new JFrame();

    private final MyPanel panel = new MyPanel();
    
    private final Object WORLD_LOCK=new Object();
    
    // @GuardedBy( WORLD_LOCK )
    private World worldToRender;
    
    private final boolean debug;

    public SoftwareRenderer(boolean debug) 
    {
    	this.debug = debug;
    }

    @Override
    public void setup()
    {
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        panel.setBackground(Color.BLACK);
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
    }   

    @Override
    public void displayTitle(String title)
    {
        frame.setTitle( title );
    }
    
    protected final class MyPanel extends JPanel {

        private double xInc=1.0;
        private double yInc=1.0;
        
        private World currentWorld;

        public void paint(Graphics g) {

            super.paint(g);
            final Graphics2D graphics = (Graphics2D) g;

            synchronized( WORLD_LOCK ) 
            {
                this.currentWorld = worldToRender;
                
                if ( currentWorld == null ) {
                    return;
                }
            }

            final SimulationParameters params = currentWorld.getSimulationParameters();            
            final double modelMax = params.modelMax;

            xInc = getWidth() / modelMax;
            yInc = getHeight() / modelMax;

            final IBoidVisitor visitor = new IBoidVisitor() {

                private int count = 0;

                @Override
                public void visit(Boid boid)
                {
                    drawBoid( boid ,count == 0 , params , graphics );
                    count++;
                }
            };

            g.setColor( BOID_COLOR );
            currentWorld.visitAllBoids( visitor );                    
        }

        private void drawBoid(Boid boid, boolean firstBoid , final SimulationParameters params , Graphics2D g)
        {
            drawBoid(boid,firstBoid,Color.BLUE,true , params , g);
        }

        private void drawBoid(final Boid boid, boolean isDebugBoid , Color color , boolean fill , final SimulationParameters params, final Graphics2D g)
        {
            if ( debug && isDebugBoid ) 
            {
                // draw neighbor radius
                g.setColor(Color.GREEN );
                drawCircle( boid.getNeighbourCenter() , params.neighbourRadius , g );

                // draw separation radius
                g.setColor(Color.RED);
                drawCircle( boid.getNeighbourCenter() , params.separationRadius , g );  

                // mark neighbors
                final NeighborAggregator visitor = new NeighborAggregator(boid,params.separationRadius) {

                    @Override
                    public void visit(Boid other)
                    {
                        if ( other != boid ) {
                            super.visit(other);  

                            final double distance = other.getLocation().minus( boid.getNeighbourCenter() ).length();

                            if ( distance > params.neighbourRadius ) {
                                return;
                            }                            
                            drawBoid( other , false , Color.PINK , true, params , g );
                        }
                    }
                };
                boid.visitNeighbors( currentWorld , params.neighbourRadius , visitor );

                // cohesion
                Vec2dMutable cohesionVec = Simulation.steerTo( params , boid , visitor.getAverageLocation() );

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
    }

    @Override
    public void render(World world) throws Exception
    {
        synchronized (WORLD_LOCK) {
            worldToRender = world;
        }
        panel.repaint();
    }

    @Override
    public void destroy()
    {
        frame.dispose();
    }
    
    protected static final int round(double d) {
        return (int) Math.round(d);
    }    
}