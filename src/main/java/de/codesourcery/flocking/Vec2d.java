package de.codesourcery.flocking;

public class Vec2d
{
    private static final float RAD_TO_DEG = (float) ( 180.0d / Math.PI );

    public static final Vec2d ORIGIN = new Vec2d(0,0);

    public final double x;
    public final double y;

    public Vec2d(Vec2dMutable v)
    {
    	this.x = v.x;
    	this.y = v.y;
    }
    
    public Vec2d(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public Vec2d rotate90DegreesCW() {
        // The right-hand normal of vector (x, y) is (y, -x), and the left-hand normal is (-y, x)
        return new Vec2d( y , -x );
    }
    
    public Vec2d rotate90DegreesCCW() {
        // The right-hand normal of vector (x, y) is (y, -x), and the left-hand normal is (-y, x)
        return new Vec2d( -y , x );
    }    
    
    public Vec2d limit(double maxValue) 
    {
        final double speed = length();
        if ( speed < maxValue ) {
            return this;
        }
        return normalize().multiply( maxValue );
    }
    
    public Vec2d plus(Vec2dMutable o) {
        return new Vec2d(this.x+o.x,this.y+o.y);
    }
    
    public Vec2dMutable plusInPlace(Vec2dMutable o) {
        return new Vec2dMutable(this.x+o.x,this.y+o.y);
    }    
    
    public Vec2d plus(Vec2d o) {
        return new Vec2d(this.x+o.x,this.y+o.y);
    }
    
    public Vec2d normalize() 
    {
        double l = length();
        if ( Math.abs( l ) < 0.00001 ) {
            return this;
        }
        return new Vec2d( x/l , y/l );
    }
    
    public Vec2d wrapIfNecessary(double maxValue)
    {
        double newX = x;
        double newY = y;
        
        if ( newX < 0 ) {
            newX = maxValue+newX;
        } else if ( newX >= maxValue ) {
            newX = newX-maxValue;
        }
        if ( newY < 0 ) {
            newY = maxValue+newY;
        } else if ( newY >= maxValue ) {
            newY = newY-maxValue;
        }       
        
        if ( newX != x || newY != y ) {
            return new Vec2d(newX,newY);
        }
        return this; 
    }

    public Vec2d divide(double factor) {
        return new Vec2d( x/factor , y/factor );
    }
    
    public Vec2d multiply(double factor) {
        return new Vec2d( factor*x , factor*y );
    }

    public Vec2d minus(double x1,double y1) {
        return new Vec2d(this.x-x1,this.y-y1);
    }      

    public Vec2d minus(Vec2d o) {
        return new Vec2d(this.x-o.x,this.y-o.y);
    }    
    
    public double distanceTo(Vec2d other) 
    {
    	final double dx = other.x - this.x;
    	final double dy = other.y - this.y;
    	return Math.sqrt( dx*dx + dy*dy);
    }

    public double length() {
        return Math.sqrt( x*x + y*y );
    }

    public double dotProduct(Vec2d b) 
    {
        return this.x*b.x + this.y*b.y;
    }    

    public float angleInRad(Vec2d other) {
        double f = dotProduct(other) / ( this.length() * other.length() );
        return (float) Math.acos(f);
    }

    public float angleInDeg(Vec2d other) {
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
        return 31 * (31 + Double.valueOf( x ).hashCode() ) + Double.valueOf( y ).hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof Vec2d) ) {
            return false;
        }
        final Vec2d other = (Vec2d) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString()
    {
        return "("+x+","+y+")";
    }
}
