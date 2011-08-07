# TODO: Add comment
# 
# Author: wmoore
###############################################################################


library(Logicle)

# more or less Diva default import
l <- Logicle::create(300000,.5)
beta <- function(x) Logicle::inverse(l,x)
lambda <- function(x) Logicle::scale(l,x)

# basic functionality
graphics::plot(beta,xaxt='n')
Logicle::axis(side=1,logicle=l)

# equal mixture of normal and log normal populations
N = 1000;
x = c( 1000 * exp(rnorm(N)), 10 * rnorm(N) );
y = c( 4000 * exp(rnorm(N)), 40 * rnorm(N) );

# transform to logicle scale
u = lambda(x)
v = lambda(y)

# default histogram
graphics::hist(x)

# Logicle histogram
graphics::hist(u, xaxt='n')
Logicle::axis(side=1,logicle=l)

# Logicle dot plot
graphics::plot(u, v, xaxt='n', yaxt='n')
Logicle::axis(side=1,logicle=l)
Logicle::axis(side=2,logicle=l)


