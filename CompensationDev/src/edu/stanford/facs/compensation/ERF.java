package edu.stanford.facs.compensation;

public class ERF {

	/**
	 * @param args
	 */
	public final static int NCOF=28;
	public final static double coefficients[]= {-1.3026537197817094, 6.4196979235649026e-1,
		1.9476473204185836e-2,-9.561514786808631e-3,-9.46595344482036e-4,
		3.66839497852761e-4,4.2523324806907e-5,-2.0278578112534e-5,
		-1.624290004647e-6,1.303655835580e-6,1.5626441722e-8,-8.5238095915e-8,
		6.529054439e-9,5.059343495e-9,-9.91364156e-10,-2.27365122e-10,
		9.6467911e-11, 2.394038e-12,-6.886027e-12,8.94487e-13, 3.13092e-13,
		-1.12708e-13,3.81e-16,7.106e-15,-1.523e-15,-9.4e-17,1.21e-16,-2.8e-17};
	
	double mu, sig;

	public ERF(){
		mu=0;
		sig=1.0;
		
	}
	public ERF (double x, double y){
		mu=x;
		sig = y;
	}
	
	protected double func (double p){
		return erf(p);
	}
	
	
	protected static double erf (double x){
		if (x>=0.)
			return 1.0 - erfccheb(x);
		else 
			return erfccheb(-x) - 1.0;
	}
	
	protected static double erfc (double x){
		if (x >= 0.) 
			return erfccheb(x);
		else 
			return 2.0 - erfccheb(-x);

	}
	
	protected static double erfccheb (double z){
		int j;
		double t,ty,tmp,d=0.,dd=0.;
		if (z < 0.) 
			throw new IllegalArgumentException(); //throw("erfccheb requires nonnegative argument");
		t = 2./(2.+z);
		ty = 4.*t - 2.;
		for (j=NCOF-1;j>0;j--) {
			tmp = d;
			d = ty*d - dd + coefficients[j];
			dd = tmp;
		}
		return t*Math.exp(-z*z + 0.5*(coefficients[0] + ty*d) - dd);
	}
	
	protected static double  inverfc(double p) {
		double x,err,t,pp;
		if (p >= 2.0) return -100.;
		if (p <= 0.0) return 100.;
		pp = (p < 1.0)? p : 2. - p;
		t = Math.sqrt(-2.*Math.log(pp/2.));
		x = -0.70711*((2.30753+t*0.27061)/(1.+t*(0.99229+t*0.04481)) - t);
		for (int j=0;j<2;j++) {
			err = erfc(x) - pp;
			x += err/(1.12837916709551257*Math.exp(-(x*x))-x*err);
		}
		return (p < 1.0? x : -x);
	}

	protected static double inverf(double p) {
		return inverfc(1.-p);
		}

	protected static double erfcc(double x){
		double t,z=Math.abs(x),ans;
		t=2./(2.+z);
		ans=t*Math.exp(-z*z-1.26551223+t*(1.00002368+t*(0.37409196+t*(0.09678418+
			t*(-0.18628806+t*(0.27886807+t*(-1.13520398+t*(1.48851587+
			t*(-0.82215223+t*0.17087277)))))))));
		return (x >= 0.0 ? ans : 2.0-ans);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public class Normaldist extends ERF{
		
		// Normaldist(Doub mmu = 0., Doub ssig = 1.) : mu(mmu), sig(ssig) {
		Normaldist(double mmu, double ssig)  {
			super (mmu, ssig);
			//mu=mmu;
			//sig=ssig;
			if (sig <= 0.) 
				throw new IllegalArgumentException("ERF.Normaldistr sig is less than 0.");
		}
		double p(double x) {
			double xmusig = (x-mu)/sig;
			return (0.398942280401432678/sig)*Math.exp(-0.5*(xmusig*xmusig));
		}
		double cdf(double x) {
			return 0.5*erfc(-0.707106781186547524*(x-mu)/sig);
		}
		double invcdf(double p) {
			if (p <= 0. || p >= 1.) {
				throw new IllegalArgumentException("ERF.Normaldist.invcdf() p is <=0 or p>=1");
			}
			return -1.41421356237309505*sig*inverfc(2.*p)+mu;
		}

	}

	
	class Lognormaldist extends ERF{
		
		Lognormaldist(double mmu, double ssig) {
			//double mmu = 0., double ssig = 1.) : mu(mmu), sig(ssig)
		    super (mmu,  ssig);
		  //  mu=mmu;
		 //   sig=ssig;
			if (sig <= 0.){
				throw new IllegalArgumentException("ERF.LogNormaldistr sig <= 0.");
			}
		}
		double p(double x) {
			if (x < 0.) {
				throw new IllegalArgumentException("ERF.LogNormaldistr.p() x < 0.");
			}
			if (x == 0.) return 0.;
			double xmusig = (Math.log(x)-mu)/sig;
			return (0.398942280401432678/(sig*x))*Math.exp(-0.5*xmusig*xmusig);
		}
		double cdf(double x) {
			if (x < 0.){
				throw new IllegalArgumentException("ERF.LogNormaldistr.cdf() x < 0.");
			}
			if (x == 0.) return 0.;
			return 0.5*erfc(-0.707106781186547524*(Math.log(x)-mu)/sig);
		}
		double invcdf(double p) {
			if (p <= 0. || p >= 1.){
				throw new IllegalArgumentException("ERF.LogNormaldistr.invcdf() p <= 0 or p>=1.");
			}
			return Math.exp(-1.41421356237309505*sig*inverfc(2.*p)+mu);
		}
	}
}


