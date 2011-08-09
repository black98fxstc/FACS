\name{axis}
\alias{axis}
\title{Draw Logicle scale axis}
\description{
	Overrides graphics::axis so that if a logicle scale argument is present
	a suitable axis is drawn and otherwise falls back to the normal axis behavior.
}

\usage{
	axis(side, logicle, ...)
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
	l = Logicle::create(1000,1)
	Logicle::axis(side=0,logicle=l);
}
