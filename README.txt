A Java implementation of the famous flocking algorithm by Craig Reynolds.

I spent quite some time performance-tweaking, my i5 2400K (4 cores at 3.3 Ghz) runs this at 30fps with 25.000 boids on Linux , JDK 1.7 (64 bit). 
I took some shortcuts with the nearest-neighbour detection and use an approximation but IMHO this is not noticeable.

Currently there's no UI for tweaking the simulation parameters (todo...) , it your hardware is too slow you need to adjust the POPULATION_SIZE (and/or TARGET_FPS)
parameters in Main.java

To run:

mvn compile exec:java
