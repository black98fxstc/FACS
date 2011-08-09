\name{create}
\alias{create}
\title{Create Logicle Scale}
\description{
	Creates an opaque object that can be used to convert data to and from a Logicle scale.
}

\usage{
	create(T, W, M = 4.5, A = 0, bins = 0)
}
\arguments{
	\item{T}{Value at the top of the data scale}
	\item{W}{Width in decades of the linearization region}
	\item{M}{Number of decades in a pure logarithmic plot}
	\item{A}{Additional decades of negative data range}
	\item{bins}{Number of bins in lookup table or 0}
}

\references{ Logicle Manuscript. 2007. Logicle Tech Note. 2011 }
\author{Wayne A. Moore, Genetics Department, Stanford University}

\keyword{vector}

\examples{
	l = Logicle::create(1000,1)
	x = 1 : 10
	y = Logicle::scale(l, x)
	z = Logicle::inverse(l, y)
}
