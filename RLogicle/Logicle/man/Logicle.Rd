\docType{package}
\name{Logicle}
\alias{Logicle}
\alias{Logicle-package}
\title{Logicle Data Scale}
\description{
	An implementation of the Logicle data scale.
}

\references{ Logicle Manuscript. 2007. Logicle Tech Note. 2010 }
\author{Wayne A. Moore, Genetics Department, Stanford University}
\keyword{vector}
\examples{
	l = Logicle::create(1000,1)
	x = 1 : 10
	y = Logicle::scale(l, x)
	z = Logicle::inverse(l, y)
	Logicle::destroy(l)
}
