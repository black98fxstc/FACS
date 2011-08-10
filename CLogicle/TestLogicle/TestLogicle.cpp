#include "logicle.h"
#include "gtest/gtest.h"
#include <memory.h>
#include <limits>

// unit tests based on GoogleTest framework

class TestLogicle : public ::testing::Test 
{
protected:
	const double EPSILON;
	static const int NUMBER_OF_VALUES = 1000000;

	TestLogicle () : EPSILON(std::numeric_limits<double>::epsilon())
	{	};

	// these random number generators are from Numerical Recipies 3rd Edition
	class Random
	{
	private:
		unsigned long long int u, v, w;

	public:
		Random (unsigned long long int j)
			: v(4101842887655102017ULL), w(1)
		{
			u = j ^ v; next();
			v = u; next();
			w = v; next();
		};

		unsigned long long int next ()
		{
			u = u * 2862933555777941757ULL + 7046029254385353087ULL;
			v ^= v >> 17; v ^= v << 31; v ^= v >> 8;
			w = 4294957665ULL * (w & 0xffffffff) + (w >> 32);
			unsigned long long int x = u ^ (u << 21); x ^= x >> 35; x ^= x << 4;
			return (x + v) ^ w;
		};

		double nextDouble ()
		{
			return 5.42101086242752217E-20 * next();
		};

		double nextGaussian ()
		{
			double u, v, x, y, q;
			do
			{
				u = nextDouble();
				v = 1.7156 * (nextDouble() - .5);
				x = u - .449871;
				y = fabs(v) + .386595;
				q = x * x + y * (.196 * y - .25472 * x);
			} while (q > .27597 && (q > 0.27846 || v * v > -4. * log(u) * (u * u)));
			return v/u;
		};
	};

	class Distribution
	{
	protected:
		Random * const random;

	public:
		Distribution (Random * random) : random(random)
		{	};

		virtual double sample () = 0;
	};

	class Uniform : public Distribution
	{
		const double min, max;

	public:
		double sample ()
		{
			return min + (max - min) * random->nextDouble();
		};

		Uniform (Random * random, double min, double max) : Distribution(random), min(min), max(max)
		{
		};
	};

	class Normal : public Distribution
	{
		const double mean, sd;

	public:
		double sample ()
		{
			return mean + sd * random->nextGaussian();
		};

		Normal (Random * random, double mean, double sd) : Distribution(random), mean(mean), sd(sd)
		{	};
	};

	class LogNormal : public Distribution
	{
		const double mean, sd;

	public:
		double sample ()
		{
			return exp(mean + sd * random->nextGaussian());
		};

		LogNormal (Random * random, double mean, double sd) : Distribution(random), mean(mean), sd(sd)
		{	};
	};

	Random * random;

	virtual void SetUp() 
	{
	    // use a random number generator with a fixed seed for reproducibility
		random = new Random(31415926);
	}

	virtual void TearDown() 
	{
		delete random;
	}

	static void testLogicleScale (Logicle & logicle, Distribution & distribution,
		double precision)
	{
		for (int i = 0; i < NUMBER_OF_VALUES; ++i)
		{
			double trueScale = distribution.sample();
			double dataValue = logicle.inverse(trueScale);
			double testScale = logicle.scale(dataValue);
			EXPECT_LE(fabs(trueScale - testScale), precision);
		}
	}

	static void testLogicleInverse (Logicle & logicle, Distribution & distribution,
		double tolerance)
	{
		for (int i = 0; i < NUMBER_OF_VALUES; ++i)
		{
			double trueData = distribution.sample();
			double scale = logicle.scale(trueData);
			double testData = logicle.inverse(scale);
			// data can't be better than slope times tolerance of argument
			EXPECT_LE(fabs(trueData - testData), tolerance * logicle.slope(scale));
		}
	}

	static void testLabels (std::vector<double> actual, double expected[], int count)
	{
		double float_epsilon = std::numeric_limits<float>::epsilon();
		EXPECT_EQ(actual.size(), count);
	  	for (int i = 0; i < count; ++i)
		{
			double delta = actual[i] - expected[i];
			if (expected[i] != 0)
				delta /= expected[i];
			EXPECT_LE(fabs(delta), float_epsilon);
		}
	}

	void testSolve (Random & random)
	{
		// use standard 4.5 decade logicle scale
		double b = 4.5 * Logicle::LN_10;
		// since w <= .5 this is precision of about 2ulp
		// seems to be the highest precision that works
		// probably because of the constant 2 in the formula
		double tolerance = EPSILON;

		for (int i = 0; i < NUMBER_OF_VALUES; ++i)
		{
		  double true_w = .5 * random.nextDouble();
		  double d = Logicle::solve(b, true_w);
		  double test_w = -2 * (log(d) - log(b)) / (b + d);
		  EXPECT_LE(fabs(true_w - test_w), tolerance);
		}
	}

	static void testPureCScale (Logicle & logicle, Distribution & distribution)
	{
		const struct logicle_params * params = logicle_create(logicle.T(), logicle.W(), logicle.M(), logicle.A(), 0);

		for (int i = 0; i < NUMBER_OF_VALUES; ++i)
		{
			double trueScale = distribution.sample();
			double dataValue = logicle.inverse(trueScale);
			double cppScale = logicle.scale(dataValue);
			double cScale = logicle_scale(params, dataValue);
			EXPECT_EQ(cppScale, cScale);
		}
	}

	static void testPureCInverse (Logicle & logicle, Distribution & distribution)
	{
		const struct logicle_params * params = logicle_create(logicle.T(), logicle.W(), logicle.M(), logicle.A(), 0);

		for (int i = 0; i < NUMBER_OF_VALUES; ++i)
		{
			double trueData = distribution.sample();
			double scale = logicle.scale(trueData);
			double cppData = logicle.inverse(scale);
			double cData = logicle_inverse(params, scale);
			EXPECT_EQ(cppData, cData);
		}
	}

	static void testPureCFastScale (FastLogicle & logicle, Distribution & distribution)
	{
		const struct logicle_params * params = logicle_create(logicle.T(), logicle.W(), logicle.M(), logicle.A(), logicle.bins());

		for (int i = 0; i < NUMBER_OF_VALUES; ++i)
		{
			double trueScale = distribution.sample();
			double dataValue = logicle.inverse(trueScale);
			double cppScale = logicle.scale(dataValue);
			double cScale = logicle_scale(params, dataValue);
			EXPECT_EQ(cppScale, cScale);
		}
	}

	static double * getLookup (FastLogicle & fast)
	{
		return fast.p->lookup;
	}
};

TEST_F(TestLogicle, TestScale) 
{
	Uniform normal_range(random, 0, 1);
	Uniform extended_range(random, -1, 4);

	{
		Logicle logicle(10000, 1);
		testLogicleScale(logicle, normal_range, EPSILON);
		testLogicleScale(logicle, extended_range, 2 * EPSILON);
	}

	{
		Logicle logicle(1000, 2, 4);
		testLogicleScale(logicle, normal_range, 3 * EPSILON);
		testLogicleScale(logicle, extended_range, 2 * EPSILON);
	}
}

TEST_F(TestLogicle, TestInverse) 
{
	{
		Logicle worst_case(10000, 2.5, 5.0);
		Normal normal(random, 0, 5);
		testLogicleInverse(worst_case, normal, EPSILON);
	}

	{
		Logicle typical(10000, 1);
		Normal normal(random, 0, 5);
		testLogicleInverse(typical, normal, EPSILON);

		LogNormal log_normal(random, typical.b() / 2, typical.b() / 6);
		testLogicleInverse(typical, log_normal, 2 * EPSILON );
	}

	{
		Logicle josef(1000, 2, 4);
		Normal normal(random, 0, 5);
		testLogicleInverse(josef, normal, 2 * EPSILON);

		LogNormal log_normal(random, josef.b() / 2, josef.b() / 6);
		testLogicleInverse(josef, log_normal, 3 * EPSILON );
	}
}

TEST_F(TestLogicle, TestAxisLabels) 
{
	Logicle * logicle;
	std::vector<double> label;

	// typical scale
	logicle = new Logicle(10000, 1);
	double typical[] = { 0, 100, 1000, 10000 };
	logicle->axisLabels(label);
	testLabels(label, typical, sizeof(typical)/sizeof(double));

	// typical Diva
	logicle = new Logicle(300000, .5);
	double typical_diva[] = { -100, 0, 100, 1000, 10000, 100000 };
	logicle->axisLabels(label);
	testLabels(label, typical_diva, sizeof(typical_diva)/sizeof(double));

	// typical EDesk
	logicle = new Logicle(1000, .5);
	double typical_desk[] = { 0, 1, 10, 100 };
	logicle->axisLabels(label);
	testLabels(label, typical_desk, sizeof(typical_desk)/sizeof(double));

	// unit full scale
	logicle = new Logicle(1, .5);
	double unit_full_scale[] = { 0, .001, .01, .1, 1 };
	logicle->axisLabels(label);
	testLabels(label, unit_full_scale, sizeof(unit_full_scale)/sizeof(double));

	// arcsinh scale
	logicle = new Logicle(10000, 0, Logicle::DEFAULT_DECADES, .5);
	double arcsinh[] = { 0, 1, 10, 100, 1000, 10000 };
	logicle->axisLabels(label);
	testLabels(label, arcsinh, sizeof(arcsinh)/sizeof(double));

	// no negative region
	logicle = new Logicle(10000, 1, Logicle::DEFAULT_DECADES, -1);
	double no_negative[] = { 0, 100, 1000, 10000 };
	logicle->axisLabels(label);
	testLabels(label, no_negative, sizeof(no_negative)/sizeof(double));

	// worst case
	logicle = new Logicle(10000, 2.5, 5.0);
	double worst_case[] = { -10000, 0, 10000 };
	logicle->axisLabels(label);
	testLabels(label, worst_case, sizeof(worst_case)/sizeof(double));
}

TEST_F(TestLogicle, TestSolve)
{
	TestLogicle::testSolve(*random);
}

TEST_F(TestLogicle, TestIntScale)
{
    FastLogicle fast(10000, 1);
    for (int i = 0; i < NUMBER_OF_VALUES; ++i)
    {
      double trueScale = random->nextDouble();
      double dataValue = fast.inverse(trueScale);
      int trueInt = (int)floor(fast.bins() * trueScale);
      int testInt = fast.intScale(dataValue);
	  EXPECT_EQ(trueInt, testInt);
    }
}

TEST_F(TestLogicle, TestFastScaleLowerBound)
{
    FastLogicle fast(10000, 1);
	double bound = getLookup(fast)[0];
	bound *= (1 + EPSILON);

	EXPECT_THROW({fast.scale(bound);}, Logicle::IllegalArgument);
}

TEST_F(TestLogicle, TestFastScaleUpperBoundEqual)
{
    FastLogicle fast(10000, 1);
	double bound = getLookup(fast)[fast.bins()];

	EXPECT_THROW({fast.scale(bound);}, Logicle::IllegalArgument);
}

TEST_F(TestLogicle, TestFastScaleUpperBoundGreater)
{
    FastLogicle fast(10000, 1);
	double bound = getLookup(fast)[fast.bins()];
	bound *= (1 + EPSILON);

	EXPECT_THROW({fast.scale(bound);}, Logicle::IllegalArgument);
}

TEST_F(TestLogicle, TestFastScale)
{
    FastLogicle binary_scale(100000, 1);
    Uniform normal_range(random, 0, 1);
    testLogicleScale(binary_scale, normal_range, 1. / (double)binary_scale.bins());
    
    // normal range, decimal scale
	FastLogicle decimal_scale(10000, 1, 1000);
    testLogicleScale(decimal_scale, normal_range, 1. / (double)decimal_scale.bins());
}

TEST_F(TestLogicle, TestPureCScale)
{
	Uniform normal_range(random, 0, 1);
	Uniform extended_range(random, -1, 4);

	{
		Logicle logicle(10000, 1);
		testPureCScale(logicle, normal_range);
		testPureCScale(logicle, extended_range);
	}

	{
		Logicle logicle(1000, 2, 4);
		testPureCScale(logicle, normal_range);
		testPureCScale(logicle, extended_range);
	}
}

TEST_F(TestLogicle, TestPureCInverse)
{
	Uniform normal_range(random, 0, 1);
	Uniform extended_range(random, -1, 4);

	{
		Logicle logicle(10000, 1);
		testPureCScale(logicle, normal_range);
		testPureCScale(logicle, extended_range);
	}

	{
		Logicle logicle(1000, 2, 4);
		testPureCScale(logicle, normal_range);
		testPureCScale(logicle, extended_range);
	}
}

TEST_F(TestLogicle, TestPureCIntScale)
{
	const struct logicle_params * params = logicle_create(10000, 1, 4.5, 0, 4096);
    for (int i = 0; i < NUMBER_OF_VALUES; ++i)
    {
      double trueScale = random->nextDouble();
      double dataValue = logicle_inverse(params, trueScale);
	  int trueInt = (int)floor(params->bins * trueScale);
      int testInt = logicle_int_scale(params, dataValue);
	  EXPECT_EQ(trueInt, testInt);
    }
}

int main(int argc, char **argv) 
{
	::testing::InitGoogleTest(&argc, argv);
	return RUN_ALL_TESTS();
}

// Visual C++ hack!
static int dummy = PullInMyLibrary();