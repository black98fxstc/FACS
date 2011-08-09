\name{intScale}
\alias{intScale}
\title{Integer valued Logicle scale for data value}
\description{
	Returns the Logicle lookup table indecies of a data vector
}

\usage{
	scale(p, x)
}
\arguments{
	\item{p}{Parameters of the Logicle scale to be used}
	\item{x}{Data values}
}
\value{Scale values}

\references{ Logicle Manuscript. 2007. Logicle Tech Note. 2011 }
\author{Wayne A. Moore, Genetics Department, Stanford University}

\keyword{vector}

\examples{
	l = Logicle::create(1000,1)
	x = 1 : 10
	y = Logicle::intScale(l, x)
	z = Logicle::inverse(l, y)
}
