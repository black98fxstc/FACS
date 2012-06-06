package edu.stanford.facs.compensation;

public class KSDist {
	private static double ooe = 0.367879441171442322;
	
	double invxlogx(double y) {
		
		double t,u,to=0.;
		if (y >= 0. || y <= -ooe){
			throw new IllegalArgumentException("KSDist.invlogx() no such inverse value");
		}
	 	if (y < -0.2) 
	 		u = Math.log(ooe-Math.sqrt(2*ooe*(y+ooe)));
		else u = -10.;
		do {
			u += (t=(Math.log(y/u)-u)*(u/(1.+u)));
			if (t < 1.e-8 && (Math.abs(t+to)<0.01*Math.abs(t))) 
					break;
			to = t;
		} while (Math.abs(t/u) > 1.e-15);	
		return Math.exp(u);
	}
	
	double pks(double z) {
		if (z < 0.) {
			throw new IllegalArgumentException("KSDist.pks z is less than 0.");
		}
		if (z == 0.) return 0.;
		if (z < 1.18) {
			double y = Math.exp(-1.23370055013616983/(z*z));
			return 2.25675833419102515*Math.sqrt(-Math.log(y))
				*(y + Math.pow(y,9) + Math.pow(y,25) + Math.pow(y,49));
		} else {
			double x = Math.exp(-2.*(z*z));
			return 1. - 2.*(x - Math.pow(x,4) + Math.pow(x,9));
		}
	}
	
	double qks(double z) {
		if (z < 0.) {
			throw new IllegalArgumentException("KSDist.qks z is less than 0."); 
		}
		if (z == 0.) return 1.;
		if (z < 1.18) return 1.-pks(z);
		double x = Math.exp(-2.*(z*z));
		return 2.*(x - Math.pow(x,4) + Math.pow(x,9));
	}
	
	double invqks(double q) {
		double y,logy,yp,x,xp,f,ff,u,t;
		if (q <= 0. || q > 1.){
			throw new IllegalArgumentException("KSDist.invqks z is less than 0."); 
		}
		if (q == 1.) return 0.;
		if (q > 0.3) {
			f = -0.392699081698724155*(1.-q)*(1.-q);
			y = invxlogx(f);
			do {
				yp = y;
				logy = Math.log(y);
				double p = 1.+ Math.pow(y,4)+ Math.pow(y,12);
				ff = f/(p*p);
				u = (y*logy-ff)/(1.+logy);
				y = y - (t=u/Math.max(0.5,1.-0.5*u/(y*(1.+logy))));
			} while (Math.abs(t/y)>1.e-15);
			return 1.57079632679489662/Math.sqrt(-Math.log(y));
		} 
		else {
			x = 0.03;
			do {
				xp = x;
				x = 0.5*q+Math.pow(x,4)-Math.pow(x,9);
				if (x > 0.06) x += Math.pow(x,16)-Math.pow(x,25);
			} while (Math.abs((xp-x)/x)>1.e-15);
			return Math.sqrt(-0.5*Math.log(x));
		}
	}
	double invpks(double p) {
		return invqks(1.-p);
		}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
