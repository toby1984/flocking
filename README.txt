A Java implementation of the famous flocking algorithm by Craig Reynolds (http://www.red3d.com/cwr/).

I spent quite some time performance-tweaking, my i5 2400K (4 cores at 3.3 Ghz) runs this at 30fps with 25.000 boids on Linux , JDK 1.7 (64 bit) 
while rendering using the (really slow) Java2D API. The UI allows you to choose between Java2D and OpenGL for rendering , OpenGL is way faster than plain Java2D.

To run:

mvn compile exec:exec
