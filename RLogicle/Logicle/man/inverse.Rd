\name{inverse}
\alias{inverse}
\title{Data value for Logicle scale position}
\description{
	Returns the data value corresponding to Logicle scale position.
}

\usage{
	inverse(p, y)
}
\arguments{
	\item{p}{Parameters of the Logicle scale to be used}
	\item{y}{Logicle scale value}
}
\value(
	Data values
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
