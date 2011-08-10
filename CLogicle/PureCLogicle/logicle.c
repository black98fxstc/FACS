#include <stdlib.h>
#include <stdio.h>
#ifndef R_LOGICLE
#include <malloc.h>
#endif
#include <math.h>
#include <float.h>
#include "logicle.h"

#define NaN				(not_a_number.d);
#define LN_10			log(10.0)
#define EPSILON			DBL_EPSILON
#define TAYLOR_LENGTH	16

#define throw
#define IllegalParameter(string)	\
	{	\
		error_string = string;	\
		return 0;	\
	}
#define DidNotConverge(string)	\
	{	\
		error_string = string;	\
		return NaN;	\
	}

static const char * error_string;
static char buffer[256];
static const union
{
	long long int i;
	double d;
} not_a_number = {0x7ff8000000000000};

const char * logicle_error ()
{
	return error_string;
};

static double solve (double b, double w)
{
	double tolerance;
	double d_lo, d_hi, d, last_delta, delta;
	double f_b, f, last_f, df, t;
	int i;

	// w == 0 means its really arcsinh
	if (w == 0)
		return b;

	// precision is the same as that of b
	tolerance = 2 * b * EPSILON;

	// based on RTSAFE from Numerical Recipes 1st Edition
	// bracket the root
	d_lo = 0;
	d_hi = b;

	// bisection first step
	d = (d_lo + d_hi) / 2;
	last_delta = d_hi - d_lo;

	// evaluate the f(w,b) = 2 * (ln(d) - ln(b)) + w * (b + d)
	// and its derivative
	f_b = -2 * log(b) + w * b;
	f = 2 * log(d) + w * d + f_b;
	last_f = NaN;

	for (i = 1; i < 20; ++i)
	{
		// compute the derivative
		df = 2 / d + w;

		// if Newton's method would step outside the bracket
		// or if it isn't converging quickly enough
		if (((d - d_hi) * df - f) * ((d - d_lo) * df - f) >= 0
			|| fabs(1.9 * f) > fabs(last_delta * df))
		{
			// take a bisection step
			delta = (d_hi - d_lo) / 2;
			d = d_lo + delta;
			if (d == d_lo)
				return d; // nothing changed, we're done
		}
		else
		{
			// otherwise take a Newton's method step
			delta = f / df;
			t = d;
			d -= delta;
			if (d == t)
				return d; // nothing changed, we're done
		}
		// if we've reached the desired precision we're done
		if (fabs(delta) < tolerance)
			return d;
		last_delta = delta;

		// recompute the function
		f = 2 * log(d) + w * d + f_b;
		if (f == 0 || f == last_f)
			return d; // found the root or are not going to get any closer
		last_f = f;

		// update the bracketing interval
		if (f < 0)
			d_lo = d;
		else
			d_hi = d;
	}

	throw DidNotConverge("exceeded maximum iterations in solve()");
}

static double seriesBiexponential (const struct logicle_params * p, double scale)
{
	double x, sum;
	int i;

	// Taylor series is around x1
	x = scale - p->x1;
	// note that taylor[1] should be identically zero according
	// to the Logicle condition so skip it here
	sum = p->taylor[TAYLOR_LENGTH - 1] * x;
	for (i = TAYLOR_LENGTH - 2; i >= 2; --i)
		sum = (sum + p->taylor[i]) * x;
	return (sum * x + p->taylor[0]) * x;
}

static double double_scale (const struct logicle_params * p, double value)
{
	int negative, i;
	double x, tolerance, y, dy, ddy, delta;
	double ae2bx, ce2mdx, abe2bx, cde2mdx;

	// handle true zero separately
	if (value == 0)
		return p->x1;

	// reflect negative values
	negative = value < 0;
	if (negative)
		value = -value;

	// initial guess at solution
	if (value < p->f)
		// use linear approximation in the quasi linear region
		x = p->x1 + value / p->taylor[0];
	else
		// otherwise use ordinary logarithm
		x = log(value / p->a) / p->b;

	// try for double precision unless in extended range
	tolerance = 3 * EPSILON;
	if (x > 1)
		tolerance = 3 * x * EPSILON;

	for (i = 0; i < 10; ++i)
	{
		// compute the function and its first two derivatives
		ae2bx = p->a * exp(p->b * x);
		ce2mdx = p->c / exp(p->d * x);
		if (x < p->xTaylor)
			// near zero use the Taylor series
			y = seriesBiexponential(p, x) - value;
		else
			// this formulation has better roundoff behavior
			y = (ae2bx + p->f) - (ce2mdx + value);
		abe2bx = p->b * ae2bx;
		cde2mdx = p->d * ce2mdx;
		dy = abe2bx + cde2mdx;
		ddy = p->b * abe2bx - p->d * cde2mdx;

		// this is Halley's method with cubic convergence
		delta = y / (dy * (1 - y * ddy / (2 * dy * dy)));
		x -= delta;

		// if we've reached the desired precision we're done
		if (fabs(delta) < tolerance)
		{
			// handle negative arguments
			if (negative)
				return 2 * p->x1 - x;
			else
				return x;
		}
	}

	throw DidNotConverge("scale() didn't converge");
};

static double fast_scale (const struct logicle_params * p, double value)
{
	int index;
	double delta;

	// lookup the nearest value
    index = logicle_int_scale(p, value);
	if (index < 0)
		return NaN;

    // inverse interpolate the table linearly
    delta = (value - p->lookup[index])
      / (p->lookup[index + 1] - p->lookup[index]);

    return (index + delta) / (double)p->bins;
}

static double double_inverse (const struct logicle_params * p, double scale)
{
	int negative;
	double inverse;

	// reflect negative scale regions
	negative = scale < p->x1;
	if (negative)
		scale = 2 * p->x1 - scale;

	// compute the biexponential
	if (scale < p->xTaylor)
		// near x1, i.e., data zero use the series expansion
		inverse = seriesBiexponential(p, scale);
	else
		// this formulation has better roundoff behavior
		inverse = (p->a * exp(p->b * scale) + p->f) - p->c / exp(p->d * scale);

	// handle scale for negative values
	if (negative)
		return -inverse;
	else
		return inverse;
}

#undef IllegalArgument
#define IllegalArgument(value)	\
	{	\
		sprintf(buffer, "Illegal argument value %.17g", value);	\
		error_string = buffer;	\
		return NaN;	\
	}

static double fast_inverse (const struct logicle_params * p, double scale)
{
	int index;
	double x, delta;

	if (scale < 0)
		return p->lookup[0];
	if (scale >= 1)
		return p->lookup[p->bins];

	// find the bin
    x = scale * p->bins;
    index = (int)floor(x);

    // interpolate the table linearly
    delta = x - index;

    return (1 - delta) * p->lookup[index] + delta * p->lookup[index + 1];
}

static struct logicle_params * logicle_allocate(int bins)
{
#ifdef R_LOGICLE
	struct logicle_params * p = (struct logicle_params *)Calloc(1, struct logicle_params);
	p->taylor = Calloc(TAYLOR_LENGTH, double);
	p->bins = bins;
	if (bins > 0)
		p->lookup = (double *)Calloc(bins + 1, double);
#else
	struct logicle_params * p = (struct logicle_params *)malloc(sizeof (struct logicle_params));
	p->taylor = malloc(TAYLOR_LENGTH * sizeof(double));
	p->bins = bins;
	if (bins > 0)
		p->lookup = (double *)malloc((bins + 1) * sizeof(double));
#endif
	return p;
}

void logicle_destroy(const struct logicle_params * p)
{
#ifdef R_LOGICLE
	struct logicle_params * q = (struct logicle_params *) p;
	if (q->bins > 0)
		Free(q->lookup);
	Free(q->taylor);
	Free((struct logicle_paramse *)q);
#else
	if (p->bins > 0)
		free(p->lookup);
	free(p->taylor);
	free((struct logicle_paramse *)p);
#endif
}

const struct logicle_params * logicle_create (double T, double W, double M, double A, int bins)
{
	struct logicle_params * p;
	double c_a;
	double mf_a;
	double posCoef;
	double negCoef;
	int i;

	error_string = (char *)0;
	if (T <= 0)
		throw IllegalParameter("T is not positive");
	if (W < 0)
		throw IllegalParameter("W is negative");
	if (M <= 0)
		throw IllegalParameter("M is not positive");
	if (2 * W > M)
		throw IllegalParameter("W is too large");
	if (-A > W || A + W > M - W)
		throw IllegalParameter("A is too large");

	// allocate the parameter structure
	p = logicle_allocate(bins);

	// if we're going to bin the data make sure that
	// zero is on a bin boundary by adjusting A
	if (bins > 0)
	{
		double zero = (W + A) / (M + A);
		zero = floor(zero * bins + .5) / bins;
		A = (M * zero - W) / (1 - zero);
	}

	// standard parameters
	p->T = T;
	p->M = M;
	p->W = W;
	p->A = A;

	// actual parameters
	// formulas from biexponential paper
	p->w = W / (M + A);
	p->x2 = A / (M + A);
	p->x1 = p->x2 + p->w;
	p->x0 = p->x2 + 2 * p->w;
	p->b = (M + A) * LN_10;
	p->d = solve(p->b, p->w);
	c_a = exp(p->x0 * (p->b + p->d));
	mf_a = exp(p->b * p->x1) - c_a / exp(p->d * p->x1);
	p->a = T / ((exp(p->b) - mf_a) - c_a / exp(p->d));
	p->c = c_a * p->a;
	p->f = -mf_a * p->a;

	// use Taylor series near x1, i.e., data zero to
	// avoid round off problems of formal definition
	p->xTaylor = p->x1 + p->w / 4;
	// compute coefficients of the Taylor series
	posCoef = p->a * exp(p->b * p->x1);
	negCoef = -p->c / exp(p->d * p->x1);
	// 16 is enough for full precision of typical scales
	for (i = 0; i < TAYLOR_LENGTH; ++i)
	{
		posCoef *= p->b / (i + 1);
		negCoef *= -p->d / (i + 1);
		(p->taylor)[i] = posCoef + negCoef;
	}
	p->taylor[1] = 0; // exact result of Logicle condition

	if (bins > 0)
		for (i = 0; i <= bins; ++i)
			p->lookup[i] = double_inverse(p, (double)i / (double) bins);

	return p;
}

double logicle_scale (const struct logicle_params * params, double value)
{
	if (params->bins == 0)
		return double_scale(params, value);
	else
		return fast_scale(params, value);
}

#undef IllegalArgument
#define IllegalArgument(value)	\
	{	\
		sprintf(buffer, "Illegal argument value %.17g", value);	\
		error_string = buffer;	\
		return -1;	\
	}

int logicle_int_scale (const struct logicle_params * p, double value)
{
	int lo, mid, hi;
	double key;

    // binary search for the appropriate bin
    lo = 0;
    hi = p->bins;
    while (lo <= hi)
    {
      mid = (lo + hi) >> 1;
      key = p->lookup[mid];
      if (value < key)
        hi = mid - 1;
      else if (value > key)
        lo = mid + 1;
      else if (mid < p->bins)
        return mid;
      else
        // equal to table[bins] which is for interpolation only
		throw IllegalArgument(value);
	}

    // check for out of range
    if (hi < 0 || lo > p->bins)
		throw IllegalArgument(value);

    return lo - 1;
}

double logicle_inverse (const struct logicle_params * params, double scale)
{
	if (params->bins == 0)
		return double_inverse(params, scale);
	else
		return fast_inverse(params, scale);
}

double logicle_int_inverse (const struct logicle_params * params, int scale)
{
	if (!(params->bins > 0))
		return NaN;
	if (scale < 0)
		return params->lookup[0];
	if (scale >= params->bins)
		return params->lookup[params->bins];
	return params->lookup[scale];
}

#ifdef R_LOGICLE
R_NativePrimitiveArgType scale_args[] = {
		EXTPTRSXP, INTSXP, REALSXP, REALSXP };
R_NativePrimitiveArgType int_scale_args[] = {
		EXTPTRSXP, INTSXP, REALSXP, INTSXP };
R_NativePrimitiveArgType int_inverse_args[] = {
		EXTPTRSXP, INTSXP, INTSXP, REALSXP };
R_NativePrimitiveArgType unary_args[] = {
		EXTPTRSXP };
R_NativeArgStyle scale_style[] = {
		R_ARG_IN, R_ARG_IN, R_ARG_IN, R_ARG_OUT };

R_CallMethodDef callMethods[] = {
		{ "R_create", (DL_FUNC) &R_create, 5 },
		{ "R_T", (DL_FUNC) &R_T, 1 },
		{ "R_W", (DL_FUNC) &R_W, 1 },
		{ "R_M", (DL_FUNC) &R_M, 1 },
		{ "R_A", (DL_FUNC) &R_A, 1 },
		{ "R_bins", (DL_FUNC) &R_bins, 1 },
		{ NULL, NULL, 0 } };

R_CMethodDef cMethods[] = {
		{ "R_destroy", (DL_FUNC) &R_destroy, 1, unary_args },
		{ "R_scale", (DL_FUNC) &R_scale, 4, scale_args, scale_style },
		{ "R_intScale", (DL_FUNC) &R_intScale, 4, int_scale_args, scale_style },
		{ "R_inverse", (DL_FUNC) &R_inverse, 4, scale_args, scale_style },
		{ "R_intInverse", (DL_FUNC) &R_intInverse, 4, int_inverse_args, scale_style },
		{ NULL, NULL, 0 } };

void R_init_Logicle(DllInfo *info)
{
	/* Register routines, allocate resources. */
	R_registerRoutines(info, cMethods, callMethods, NULL, NULL);
}

void R_unload_Logicle(DllInfo *info)
{
	/* Release resources. */
}

#define AS_LOGICLE(p,pp) { \
	int is_logicle = TRUE; \
	if (TYPEOF(pp) != EXTPTRSXP) \
		is_logicle = FALSE; \
	else if (R_ExternalPtrTag(pp) != install("logicle_params")) \
		is_logicle = FALSE; \
	if (!is_logicle) \
		error("not logicle"); \
	p = (struct logicle_params *)R_ExternalPtrAddr(pp); }

SEXP R_create(SEXP T, SEXP W, SEXP M, SEXP A, SEXP bins)
{
	SEXP pp = NULL;
	const struct logicle_params * p =
			logicle_create(*REAL(T), *REAL(W), *REAL(M), *REAL(A), *INTEGER(bins));

	PROTECT(pp = R_MakeExternalPtr((void *)p, install("logicle_params"), R_NilValue));
	R_RegisterCFinalizerEx(pp, R_destroy, TRUE);
	UNPROTECT(1);

	return pp;
}

void R_destroy(SEXP pp)
{
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	if (p != NULL)
		logicle_destroy(p);
	R_ClearExternalPtr(pp);
}

void R_scale(SEXP pp, int* n, double* x, double* y)
{
	int i;
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	if (p->bins > 0)
		for(i = 0; i < *n; ++i)
			y[i] = fast_scale(p, x[i]);
	else
		for(i = 0; i < *n; ++i)
			y[i] = double_scale(p, x[i]);
}

void R_intScale(SEXP pp, int* n, double* x, int* y)
{
	int i;
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	if (p->bins > 0)
		for(i = 0; i < *n; ++i)
			y[i] = logicle_int_scale(p, x[i]);
	else
		error("not a fast scale");
}

void R_inverse(SEXP pp, int* n, double* y, double* x)
{
	int i;
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	if (p->bins > 0)
		for(i = 0; i < *n; ++i)
			x[i] = fast_inverse(p, y[i]);
	else
		for(i = 0; i < *n; ++i)
			x[i] = double_inverse(p, y[i]);
}

void R_intInverse(SEXP pp, int* n, int* y, double* x)
{
	int i;
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	if (p->bins > 0)
		for(i = 0; i < *n; ++i)
			x[i] = logicle_int_inverse(p, y[i]);
	else
		error("not a fast scale");
}

SEXP R_T(SEXP pp)
{
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	return ScalarReal(p->T);
}

SEXP R_W(SEXP pp)
{
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	return ScalarReal(p->W);
}

SEXP R_M(SEXP pp)
{
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	return ScalarReal(p->M);
}

SEXP R_A(SEXP pp)
{
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	return ScalarReal(p->A);
}

SEXP R_bins(SEXP pp)
{
	struct logicle_params * p;

	AS_LOGICLE(p,pp);
	return ScalarInteger(p->bins);
}

#endif /* R_LOGICLE */
