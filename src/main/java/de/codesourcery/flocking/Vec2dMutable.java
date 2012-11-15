package de.codesourcery.flocking;

public class Vec2dMutable
{
    private static final float RAD_TO_DEG = (float) ( 180.0d / Math.PI );

    public double x;
    public double y;

    public Vec2dMutable() {
    }
    
    public Vec2dMutable(Vec2d v)
    {
        this.x = v.x;
        this.y = v.y;
    }
    
    public Vec2dMutable(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public Vec2dMutable  rotate90DegreesCW() {
        // The right-hand normal of vector (x, y) is (y, -x), and the left-hand normal is (-y, x)
    	double tmp = x;
    	x= y;
    	y = -tmp;
    	return this;
    }
    
    public Vec2dMutable  rotate90DegreesCCW() {
        // The right-hand normal of vector (x, y) is (y, -x), and the left-hand normal is (-y, x)
    	double tmp = x;
    	x = -y;
    	y = tmp;
    	return this;
    }    
    
    public Vec2dMutable  limit(double maxValue) 
    {
        final double speed = length();
        if ( speed < maxValue ) {
            return this;
        }
        normalize();
        multiply( maxValue );
        return this;
    }
    
    public Vec2dMutable  plus(Vec2d o) {
    	x = x +o.x;
    	y = y +o.y;
    	return this;
    }
    
    public Vec2dMutable plus(Vec2dMutable o) {
    	x = x + o.x;
    	y = y + o.y;
    	return this;
    }
    
    public Vec2dMutable normalize() 
    {
        double l = length();
        if ( Math.abs( l ) < 0.00001 ) {
            return this;
        }
        x = x / l;
        y = y / l;
        return this;
    }
    
    public Vec2dMutable wrapIfNecessary(double maxValue)
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
        
        if ( newX != x || newY != y ) 
        {
        	x = newX;
        	y = newY;
        }
        return this;
    }

    public Vec2dMutable  divide(double factor) 
    {
    	x = x / factor;
    	y = y / factor;
    	return this;
    }
    
    public Vec2dMutable  multiply(double factor) {
    	x = x * factor;
    	y = y * factor;
    	return this;
    }

    public Vec2dMutable  minus(double x1,double y1) 
    {
    	x = x - x1;
    	y = y - y1;
    	return this;
    }    
    
    public Vec2dMutable  minus(Vec2d o) 
    {
    	x = x - o.x;
    	y = y - o.y;
    	return this;
    }    

    public Vec2dMutable  minus(Vec2dMutable o) 
    {
    	x = x - o.x;
    	y = y - o.y;
    	return this;
    }    

    public double length() {
        return Math.sqrt( x*x + y*y );
    }

    public double dotProduct(Vec2dMutable b) 
    {
        return this.x*b.x + this.y*b.y;
    }    

    public float angleInRad(Vec2dMutable other) 
    {
        double f = dotProduct(other) / ( this.length() * other.length() );
        System.out.println("f="+f);
        return (float) Math.acos(f);
    }

    public float angleInDeg(Vec2dMutable other) {
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
        if (! (obj instanceof Vec2dMutable) ) {
            return false;
        }
        final Vec2dMutable other = (Vec2dMutable) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString()
    {
        return "("+x+","+y+")";
    }
}