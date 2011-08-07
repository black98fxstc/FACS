#include <stdlib.h>
#include <stdio.h>
#include <malloc.h>
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

double solve (double b, double w)
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
	delta;

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

static double double_inverse (const struct logicle_params * p, double scale);
const struct logicle_params * logicle_initialize (double T, double W, double M, double A, int bins)
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
	if (W <= 0)
		throw IllegalParameter("W is not positive");
	if (M <= 0)
		throw IllegalParameter("M is not positive");
	if (2 * W > M)
		throw IllegalParameter("W is too large");
	if (-A > W || A + W > M - W)
		throw IllegalParameter("A is too large");

	// allocate the parameter structure
	p = (struct logicle_params *) malloc(sizeof (struct logicle_params));
	p->taylor = 0;
	p->lookup = 0;

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
	p->taylor = (double *) malloc(TAYLOR_LENGTH * sizeof (double));
	for (i = 0; i < TAYLOR_LENGTH; ++i)
	{
		posCoef *= p->b / (i + 1);
		negCoef *= -p->d / (i + 1);
		(p->taylor)[i] = posCoef + negCoef;
	}
	p->taylor[1] = 0; // exact result of Logicle condition

	p->bins = 0;
	if (bins != 0)
	{
		int i;
		p->lookup = (double *) malloc((bins + 1) * sizeof (double));
		for (i = 0; i <= bins; ++i)
			p->lookup[i] = double_inverse(p, (double)i / (double) bins);
	}
	p->bins = bins;

	return p;
}

void logicle_destroy (const struct logicle_params * params)
{
	free(params->taylor);
	free(params->lookup);
	free((void *)params);
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
			// handle negative arguments
			if (negative)
				return 2 * p->x1 - x;
			else
				return x;
	}

	throw DidNotConverge("scale() didn't converge");
};

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
	inverse;
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

	// find the bin
    x = scale * p->bins;
    index = (int)floor(x);
    if (index < 0 || index >= p->bins)
		throw IllegalArgument(scale);

    // interpolate the table linearly
    delta = x - index;

    return (1 - delta) * p->lookup[index] + delta * p->lookup[index + 1];
}

double logicle_scale (const struct logicle_params * params, double value)
{
	if (params->bins == 0)
		return double_scale(params, value);
	else
		return fast_scale(params, value);
}

double logicle_inverse (const struct logicle_params * params, double scale)
{
	if (params->bins == 0)
		return double_inverse(params, scale);
	else
		return fast_inverse(params, scale);
}
