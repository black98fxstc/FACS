\name{intInverse}
\alias{intInverse}
\title{Data value for Logicle scale lookup table index}
\description{
	Returns the data value corresponding to Logicle scale position.
}

\usage{
	intInverse(p, y)
}
\arguments{
	\item{p}{Parameters of the Logicle scale to be used}
	\item{y}{Integer Logicle scale value}
}
\value{
	Scale values
}

\references{ Logicle Manuscript. 2007. Logicle Tech Note. 2011 }
\author{Wayne A. Moore, Genetics Department, Stanford University}

\keyword{vector}

\examples{
	l = Logicle::create(1000,1,bins=4096)
	x = 1 : 10
	y = Logicle::scale(l, x)
	z = Logicle::intInverse(l, y)
}
