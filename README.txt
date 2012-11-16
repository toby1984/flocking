A Java implementation of the famous flocking algorithm by Craig Reynolds (http://www.red3d.com/cwr/).

I spent quite some time performance-tweaking, my i5 2400K (4 cores at 3.3 Ghz) runs this at 60fps with 18.000 boids on Linux , JDK 1.7 (64 bit) 
using OpenGL rendering.

To run:

mvn compile exec:exec
