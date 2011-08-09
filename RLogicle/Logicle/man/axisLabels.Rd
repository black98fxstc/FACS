\name{axisLabels}
\alias{axisLabels}
\title{Select suitable data values for a Logicle scale axis}
\description{
	Overrides graphics::axis so that if a logicle scale argument is present
	a suitable axis is drawn and otherwise falls back to the normal axis behavior.
}

\usage{
	axisLables(logicle)
}
\arguments{
	\item{logicle}{Logicle scale that defines the axis}
}
\value{
	a vector of data values suitable for the Logicle scale
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
	
	# transform to Logicle scale
	u = Logicle::scale(l,x)
	
	# Logicle histogram
	graphics::plot(u, v, xaxt='n')
	coordinates = Logicle::axisLabels(l)
	positions = Logicle::scale(l,coordinates)
	graphics::axis(side=1,at=positions,labels=coordinates)
}
