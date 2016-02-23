# LocationApproximation
Application tries to determine if the phone is located on your chest, leg (pocket), or in your hand

This was a concept to test the spread of energy across the accelerometer axis, and if we can use it to determine the location
of the cellphone based off the ESD (energy spectral density) spread characteristics.

While it does work as a whole, but it's not a practical application largely due the fact that the ESD spread will vary based
off how fast the person will walk.  I midigated for this fact by playing a song that has 120 bpm (2 steps per second) so the user
can walk at a consistent pace.  

One could theoretically find the three eigen vectors for the ESD spread (for chest, hand, and leg) and then compare those to
the windowd sample of accelerometer values, but given time contraints I chose to go with the simpler alternative of using 
closest eucladian distance to a set of "truth" values.

Gravity is accounted for by removing the lag 0 autocorrelation output for each accelerometer axis.  
