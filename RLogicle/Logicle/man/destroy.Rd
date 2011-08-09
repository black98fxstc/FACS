\name{destroy}
\alias{destroy}
\title{Destroy Logicle Scale}
\description{
	Destroys the opaque object that can be used to convert data to and from a Logicle scale.
}

\usage{
	destroy(p)
}
\arguments{
	\item{p}{The Logicle object to be destroyed}
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
