# Logicle Data Transform
#
# R wrapper code for pure C implementation of the logicle scale
# 
# Author: wmoore
###############################################################################

local({
			info <- loadingNamespaceInfo()
			ns <- .Internal(getRegisteredNamespace(as.name(info$pkgname)))
			if (is.null(ns))
				stop("cannot find name space environment");
			barepackage <- sub("([^-]+)_.*", "\\1", info$pkgname)
#			dbbase <- file.path(info$libname, info$pkgname, "R", barepackage)
#			lazyLoad(dbbase, ns, filter = function(n) n != ".__NAMESPACE__.")
		})



create <- function(T, W, M=4.5, A=0.0, bins=0)
	.Call(R_create, as.double(T), as.double(W), as.double(M), as.double(A), as.integer(bins))

destroy <- function(p)
	.C(R_destroy, p)

scale <- function(p, x)
	.C(R_scale, p, as.integer(length(x)), as.double(x), y = double(length(x)))$y

intScale <- function(p, x)
	.C(R_intScale, p, as.integer(length(x)), as.double(x), y = integer(length(x)))$y

inverse <- function(p, y)
	.C(R_inverse, p, as.integer(length(y)), as.double(y), x = double(length(y)))$x

intInverse <- function(p, y)
	.C(R_intInverse, p, as.integer(length(y)), as.integer(y), x = double(length(y)))$x
		
T <- function(p)
	.Call(R_T, p)
		
W <- function(p)
	.Call(R_W, p)
		
M <- function(p)
	.Call(R_M, p)
		
A <- function(p)
	.Call(R_A, p)
		
bins <- function(p)
	.Call(R_bins, p)

axisLabels <- function(logicle)
{
	# number of decades in the positive logarithmic region
	p = M(logicle) - 2 * W(logicle)
	# smallest power of 10 in the region
	log10x = ceiling(log10(T(logicle)) - p)
	# data value at that point
	x = exp(log(10) * log10x)
	# number of positive labels
	if (x > T(logicle))
	{
		x = T(logicle)
		np = 1
	}
	else
		np = floor(log10(T(logicle)) - log10x) + 1
	# bottom of scale
	B = inverse(logicle, 0)
	# number of negative labels
	if (x > -B)
		nn = 0
	else if (x == T(logicle))
		nn = 1
	else
		nn = floor(log10(-B) - log10x) + 1

	# fill in the axis labels
	label <- numeric(nn + np + 1)
	if (nn > 0)
		for (i in 1:nn)
		{
			label[1 + nn - i] = -x
			label[1 + nn + i] = x
			x = x * 10;
		}
	if (np > nn)
		for (i in (nn + 1):np)
		{
			label[1 + nn + i] = x
			x = x * 10
		}

	label
}

axis <- function(side,logicle,...)
{
	if (missing(logicle))
		graphics::axis(side,...)
	else
	{
		coordinates = Logicle::axisLabels(logicle)
		positions = Logicle::scale(logicle,positions)
		graphics::axis(side,at=positions,labels=coordinates,...)
	}
}
