package de.codesourcery.flocking;

import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import de.codesourcery.flocking.World.IBoidVisitor;

public class LWJGLRenderer extends AbstractRenderer {

    private double xInc;
    private double yInc;

    // VBOs
    private MyIntBuffer vertexBuffer;
    private MyIntBuffer indexBuffer;
    
    private final Object WORLD_LOCK = new Object();
    
    // @GuardedBy( WORLD_LOCK )
    private World currentWorld = null;
    
    private volatile boolean destroy = false;
    
    private final CountDownLatch destroyLatch = new CountDownLatch(1);

    public LWJGLRenderer(boolean debug,boolean debugPerformance) 
    {
        super(debug , debugPerformance );
    }

    public void setup() throws LWJGLException
    {
        final Thread thread = new Thread() {
            @Override
            public void run()
            {
                try {
                    internalSetup();
                } 
                catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        };
        
        thread.setDaemon( true );
        thread.start();        
    }
    
    private void internalSetup() throws LWJGLException 
    {
        Display.setDisplayMode(new DisplayMode(800, 600));
        Display.setResizable( true );
        Display.create();

        initGL(); 
        
        while (!Display.isCloseRequested() && ! destroy ) 
        {
            if ( Display.wasResized() ) {
                initGL();
            }

            synchronized ( WORLD_LOCK ) 
            {
                if ( currentWorld != null ) {
                    renderWorld( currentWorld );
                }
            }

            Display.update();
            Display.sync( 60 );
        }

        System.out.println("Deleting VBOs");
    	if ( vertexBuffer != null ) {
    		vertexBuffer.deleteBuffer();
    	}
    	
    	if ( indexBuffer != null ) {
    		indexBuffer.deleteBuffer();
    	}    	
    	
        System.out.println("Destroying OpenGL rendering context.");
        Display.destroy();
        
        destroyLatch.countDown();
    }
    
    @Override
    public void displayTitle(String title)
    {
        Display.setTitle( title );
    }

    private void initGL() 
    {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glViewport( 0 , 0 , Display.getWidth() , Display.getHeight() );
        GL11.glOrtho(0, Display.getWidth(), 0, Display.getHeight() , 1, -1);
//        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void renderWorld(World world) 
    {
        final double modelMax = world.getSimulationParameters().modelMax;
        xInc = Display.getWidth() / modelMax;
        yInc = Display.getHeight() / modelMax;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        
        GL11.glColor3f(0.5f, 0.5f, 1.0f);

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);  
        
        final int triangleCount = world.getPopulation();

        // setup vertex data       
        final int vertexArrayLen = triangleCount * 3 * 2 ; // one triangle = 3 vertices * 2 int's per vertex
        final MyIntBuffer vertexBuffer = getVertexBuffer( vertexArrayLen ); 
        
        final IntBuffer vertexIntBuffer = vertexBuffer.getBuffer();
        final IBoidVisitor visitor = new IBoidVisitor() {

            @Override
            public void visit(Boid boid)
            {
                drawBoid( boid , vertexIntBuffer );
            }
        };
        world.visitAllBoids( visitor );

        vertexBuffer.rewind();      
      
        GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER , vertexBuffer.getHandle() );
        GL15.glBufferData( GL15.GL_ARRAY_BUFFER , vertexBuffer.getBuffer() , GL15.GL_DYNAMIC_DRAW);
        GL11.glVertexPointer(2, GL11.GL_INT, 0, 0);        

        // setup index data
        MyIntBuffer indexBuffer = getIndexBuffer( triangleCount*3 );      
        GL15.glBindBuffer( GL15.GL_ELEMENT_ARRAY_BUFFER , indexBuffer.getHandle() );
        GL15.glBufferData( GL15.GL_ELEMENT_ARRAY_BUFFER , indexBuffer.getBuffer() , GL15.GL_STATIC_DRAW );
        
        GL11.glDrawElements(GL11.GL_TRIANGLES, triangleCount*3 , GL11.GL_UNSIGNED_INT, 0);
        
        GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER , 0 );
        GL15.glBindBuffer( GL15.GL_ELEMENT_ARRAY_BUFFER , 0 );
        
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);        
    }

    private MyIntBuffer getVertexBuffer(int elementCount) 
    {
        if ( vertexBuffer == null || vertexBuffer.getSize() != elementCount ) 
        {
        	if ( vertexBuffer != null ) {
        		vertexBuffer.deleteBuffer();
        	}
            vertexBuffer = new MyIntBuffer( elementCount );
        }
        vertexBuffer.rewind();
        return vertexBuffer;
    }
    
    private MyIntBuffer getIndexBuffer(int vertexCount) 
    {
        if ( indexBuffer == null || indexBuffer.getSize() != vertexCount ) 
        {
        	if ( indexBuffer != null ) {
        		indexBuffer.deleteBuffer();
        	}        	
            indexBuffer = new MyIntBuffer( vertexCount );
            IntBuffer buffer = indexBuffer.getBuffer();
            for ( int i = 0 ; i < vertexCount ; i+= 3 ) {
                buffer.put( i );
                buffer.put( i+1 );
                buffer.put( i+2 );
            }
        } 
        indexBuffer.rewind();
        return indexBuffer;
    }

    private void drawBoid(Boid b,IntBuffer buffer)
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

        buffer.put(x1);
        buffer.put(y1);
        
        buffer.put(x2);
        buffer.put(y2);
        
        buffer.put(x3);
        buffer.put(y3);
    }
    
    protected static final class MyIntBuffer {
        
        private final int size;
        private final IntBuffer buffer;
        private final int handle;
        
        public MyIntBuffer(int elementCount) 
        {
            this.size = elementCount;
            buffer = BufferUtils.createIntBuffer( elementCount );
            
            final IntBuffer buffer = BufferUtils.createIntBuffer(1);
            GL15.glGenBuffers( buffer );
            handle = buffer.get(0);            
        }
        
        public int getSize()
        {
            return size;
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
        
        public void deleteBuffer() {
        	GL15.glDeleteBuffers( handle );
        }
    }

    @Override
    public void render(World world) throws Exception
    {
        synchronized(WORLD_LOCK) {
            currentWorld = world;
        }
    }

    @Override
    public void destroy()
    {
        destroy = true;
        try {
            destroyLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}