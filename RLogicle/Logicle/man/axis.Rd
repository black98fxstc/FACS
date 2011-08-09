\name{axis}
\alias{axis}
\title{Draw Logicle scale axis}
\description{
	Overrides graphics::axis so that if a logicle scale argument is present
	a suitable axis is drawn and otherwise falls back to the normal axis behavior.
}

\usage{
	axis(side, logicle)
}
\arguments{
	\item{side}{an integer specifying which side of the plot the axis is to be drawn on.
		Identical in behavior to graphics::axis}
	\item{logicle}{Logicle scale that defines the axis}
}

\references{ Logicle Manuscript. 2007. Logicle Tech Note. 2011 }
\author{Wayne A. Moore, Genetics Department, Stanford University}

\keyword{axis}

\examples{
	# reasonable Diva default
	l <- Logicle::create(300000,.5)
	
	# equal mixture of normal and log normal populations
	N = 1000;
	x = c( 1000 * exp(rnorm(N)), 10 * rnorm(N) );
	y = c( 4000 * exp(rnorm(N)), 40 * rnorm(N) );
	
	# transform to Logicle scale
	u = Logicle::scale(l,x)
	v = Logicle::scale(l,y)
	
	# Logicle dot plot
	graphics::plot(u, v, xaxt='n', yaxt='n')
	Logicle::axis(side=1,logicle=l)
	Logicle::axis(side=2,logicle=l)
}
