A Java implementation of the famous flocking algorithm by Craig Reynolds (http://www.red3d.com/cwr/).

I spent quite some time performance-tweaking, my i5 2500K (4 cores at 3.3 Ghz) runs this at 60fps with 18.000 boids on Linux , JDK 1.7 (64 bit) 
using OpenGL rendering.

![screenshot](https://github.com/toby1984/flocking/blob/master/screenshot.png)

# Running

mvn compile exec:exec
