# TODO: Add comment
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

axis <- function(side,logicle,...)
{
	if (missing(logicle))
		graphics::axis(side,...)
	else
	{
		p = M(logicle) - 2 * W(logicle)
		log10x = ceiling(log10(T(logicle)) - p)
		x = exp(log(10) * log10x)
		
		np = floor(log10(T(logicle) - log10x)) + 1
		n = A(logicle)
		if (n < 0)
			np = 0
		else
			nn = ceiling(n) + 1
		
		ac <- 0
		data_value <- numeric(np + nn + 1)
		scale_value <- numeric(np + nn + 1)
		
		ac <- ac + 1
		data_value[ac] <- 0
		scale_value[ac] <- Logicle::scale(logicle,0)
		if (nn > 0)
			for (i in 1:nn)
			{
				ac <- ac + 1
				data_value[ac] <- x
				scale_value[ac] <- Logicle::scale(logicle,x)
				ac <- ac + 1
				data_value[ac] <- -x
				scale_value[ac] <- Logicle::scale(logicle,-x)
				x <- x * 10
			}
		if (np > nn)
			for (i in (nn + 1):np)
			{
				ac <- ac + 1
				data_value[ac] <- x
				scale_value[ac] <- Logicle::scale(logicle,x)
				x <- x * 10
			}
		
		graphics::axis(side,at=scale_value,labels=data_value,...)
	}
}
