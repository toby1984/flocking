package de.codesourcery.flocking;

public class Vec2
{
    private static final float RAD_TO_DEG = (float) ( 180.0d / Math.PI );

    public static final Vec2 ORIGIN = new Vec2(0,0);

    public final int x;
    public final int y;

    public Vec2(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
    
    public Vec2 wrapIfNecessary(int max) {
        
        int newX = x;
        int newY = y;
        
        if ( newX < 0 ) 
        {
            newX = max+x;
        } else if ( newX >= max ) {
            newX = newX - max;
        }
        if ( newY < 0 ) {
            newY = max+y;
        }
        if ( newY >= max) {
            newY = newY - max;
        }        
        if ( newX != x || newY != y ) {
            return new Vec2( newX, newY );
        }
        return this;
    }

    public Vec2 add(Vec2 o) {
        return new Vec2(this.x+o.x,this.y+o.y);
    }

    public Vec2 multiply(float factor) {
        return new Vec2( Math.round( factor*x ) , Math.round( factor*y ) );
    }

    public Vec2 minus(int x1,int y1) {
        return new Vec2(this.x-x1,this.y-y1);
    }      

    public Vec2 minus(Vec2 o) {
        return new Vec2(this.x-o.x,this.y-o.y);
    }    

    public double length() {
        return Math.sqrt( x*x + y*y );
    }

    public int dotProduct(Vec2 b) 
    {
        return this.x*b.x + this.y*b.y;
    }    

    public float angleInRad(Vec2 other) {
        double f = dotProduct(other) / ( this.length() * other.length() );
        System.out.println("f="+f);
        return (float) Math.acos(f);
    }

    public float angleInDeg(Vec2 other) {
        return angleInRad(other)*RAD_TO_DEG;
    }    

    public double cosine() {
        return x / Math.sqrt( x*x + y*y ); 
    }
    
    public double cosineInDeg() {
        return Math.acos( x / Math.sqrt( x*x + y*y ) )*RAD_TO_DEG; 
    }    

    @Override
    public int hashCode()
    {
        return 31 * (31 + x) + y;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof Vec2) ) {
            return false;
        }
        final Vec2 other = (Vec2) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString()
    {
        return "("+x+","+y+")";
    }
}
