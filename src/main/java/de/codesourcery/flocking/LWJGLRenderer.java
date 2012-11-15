package de.codesourcery.flocking;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

import de.codesourcery.flocking.World.IBoidVisitor;

public class LWJGLRenderer extends AbstractRenderer {

    /** time at last frame */
    long lastFrame;

    /** frames per second */
    int fps;
    /** last fps time */
    long lastFPS;

    private double xInc;
    private double yInc;

    private World world;

    // VBOs
    private MyIntBuffer vertexBuffer;
    private MyIntBuffer indexBuffer;

    public LWJGLRenderer(double modelMax,boolean debug,
            boolean debugPerformance,
            double neighborRadius, double separationRadius) 
    {
        super(modelMax , debug , debugPerformance , neighborRadius , separationRadius );
    }

    public void start(IWorldCallback callback) 
    {
        try {
            Display.setDisplayMode(new DisplayMode(800, 600));
            Display.setResizable( true );
            Display.create();
        } 
        catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        initGL(); // init OpenGL
        getDelta(); // call once before loop to initialise lastFrame
        lastFPS = getTime(); // call before loop to initialise fps timer

        while (!Display.isCloseRequested()) 
        {
            try {
                world = callback.tick();
            } catch (Exception e) {
                e.printStackTrace();
            }

            updateFPS(); // update FPS Counter

            if ( Display.wasResized() ) {
                System.out.println("--- Display resized ---");
                initGL();
            }

            renderGL();

            Display.update();

            Display.sync(TARGET_FPS); 
        }

        Display.destroy();
    }

    /**
     * Calculate how many milliseconds have passed
     * since last frame.
     *
     * @return milliseconds passed since last frame
     */
    public int getDelta() {
        long time = getTime();
        int delta = (int) (time - lastFrame);
        lastFrame = time;

        return delta;
    }

    /**
     * Get the accurate system time
     *
     * @return The system time in milliseconds
     */
    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    /**
     * Calculate the FPS and set it in the title bar
     */
    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            Display.setTitle("FPS: " + fps);
            fps = 0;
            lastFPS += 1000;
        }
        fps++;
    }

    public void initGL() 
    {
        GL11.glDisable(GL11.GL_DEPTH_TEST);    
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glViewport( 0 , 0 , Display.getWidth() , Display.getHeight() );
        GL11.glOrtho(0, Display.getWidth(), 0, Display.getHeight() , 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public void renderGL() 
    {
        xInc = Display.getWidth() / modelMax;
        yInc = Display.getHeight() / modelMax;

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );

        GL11.glColor3f(0.5f, 0.5f, 1.0f);

        GL11.glPushMatrix();
        
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);  
        
        final int len = world.getPopulation();
        
        final int arrayLen = world.getPopulation() * 3 * 2 ; // one triangle = 3 vertices * 2 int's per vertex

        // setup vertex array

        final int[] array = new int[ arrayLen ];
        
        final IBoidVisitor visitor = new IBoidVisitor() {

            private int offset = 0;
            @Override
            public void visit(Boid boid)
            {
                offset += drawBoid( boid , array , offset );
            }
        };
        world.visitAllBoids( visitor );

        final MyIntBuffer vertexArray = getVertexBuffer( arrayLen );        
        vertexArray.getBuffer().put( array );
        vertexArray.rewind();      
      
        // setup index array
        final int[] indexArray = new int[ len * 3 ];
        for ( int i = 0 ; i < len ; i+= 3 ) {
            indexArray[i] = i;
            indexArray[i+1] = i+1;
            indexArray[i+2] = i+2;
        }
        
        MyIntBuffer indexBuffer = getIndexBuffer( len*3 );
        indexBuffer.getBuffer().put( indexArray );
        indexBuffer.rewind();
        
        GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER , vertexArray.getHandle() );
        GL15.glBufferData( GL15.GL_ARRAY_BUFFER , vertexArray.getBuffer() , GL15.GL_STREAM_DRAW);
        GL11.glVertexPointer(2, GL11.GL_INT, 0, 0);        
        
        GL15.glBindBuffer( GL15.GL_ELEMENT_ARRAY_BUFFER , indexBuffer.getHandle() );
        GL15.glBufferData( GL15.GL_ELEMENT_ARRAY_BUFFER , indexBuffer.getBuffer() , GL15.GL_STREAM_DRAW );
        
        GL12.glDrawRangeElements(GL11.GL_TRIANGLES, 
                0 ,  // start
                1 , // end ; start..end is the number of elements that make up one vertex (2D => 2 , 3D => 3)
                world.getPopulation()*3 , // number of vertices to render
                GL11.GL_UNSIGNED_INT , 0 );

        GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER , 0 );
        GL15.glBindBuffer( GL15.GL_ELEMENT_ARRAY_BUFFER , 0 );
        
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        
        GL11.glPopMatrix();
    }

    private MyIntBuffer getVertexBuffer(int elementCount) 
    {
        if ( vertexBuffer == null ) {
            vertexBuffer = new MyIntBuffer( elementCount );
        }
        vertexBuffer.rewind();
        return vertexBuffer;
    }
    
    private MyIntBuffer getIndexBuffer(int elementCount) 
    {
        if ( indexBuffer == null ) {
            indexBuffer = new MyIntBuffer( elementCount );
        }
        indexBuffer.rewind();
        return indexBuffer;
    }

    private int createBufferId() 
    {
        final IntBuffer buffer = BufferUtils.createIntBuffer(1);
        GL15.glGenBuffers( buffer );
        return buffer.get(0);
    }    

    protected int drawBoid(Boid b,int[] array,int offset)
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
        final double centerX = b.getLocation().x;
        final double centerY = b.getLocation().y;

        int x1 = round( (centerX + rotatedX * ARROW_WIDTH) * xInc ); 
        int y1 = round( ( centerY + rotatedY * ARROW_WIDTH ) * yInc );

        int x2= round( (centerX + headingNormalizedX*ARROW_LENGTH) * xInc );
        int y2 = round( (centerY + headingNormalizedY*ARROW_LENGTH) * yInc );                

        int x3= round( (centerX + rotatedX * ARROW_WIDTH*-1 )*xInc);
        int y3= round( (centerY + rotatedY * ARROW_WIDTH*-1 )*yInc);

        array[offset++] = x1;
        array[offset++] = y1;
        
        array[offset++] = x2;
        array[offset++] = y2;
        
        array[offset++] = x3;
        array[offset++] = y3;

        return 6;
    }
    
    protected static final class MyIntBuffer {
        
        private final IntBuffer buffer;
        private final int handle;
        
        public MyIntBuffer(int elementCount) 
        {
            buffer = BufferUtils.createIntBuffer( elementCount );
            
            final IntBuffer buffer = BufferUtils.createIntBuffer(1);
            GL15.glGenBuffers( buffer );
            handle = buffer.get(0);            
        }
        
        public int getHandle()
        {
            return handle;
        }
        
        public void rewind() {
            buffer.rewind();
        }
        
        public IntBuffer getBuffer()
        {
            return buffer;
        }
    }
}