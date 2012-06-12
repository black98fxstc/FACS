package edu.stanford.facs.compensation;
import java.util.Arrays;
import java.util.Random;
import edu.stanford.facs.compensation.ERF.Normaldist;

public class KSTest {
    
	double[] gaussData;
	double d=0, prob=0;
	/**
	 * @param args
	 */
	public KSTest(){
		gaussData = generateGaussianData();
       ERF erf = new ERF();		
		ERF.Normaldist  nerf = erf.new Normaldist(0.0, 1.0);
		ksone (gaussData, nerf);
		System.out.println ("Gaussian data against Normal dist d=  "+d + ", prob= " + prob);
		double[] randata = generateRandomData();
		ksone (randata, nerf);
		System.out.println ("Random data against Normal dist  d="+d + ", prob  " + prob);

		
	}
	
	private double[] generateRandomData(){
		double[] mydata = new double[10000];
		Random random = new Random();
		for (int i=0; i < mydata.length; i++){
			mydata[i] = random.nextFloat();
			//if (i %1000 == 0)
			//	System.out.println (mydata[i]);
		}
		return mydata;
	}
	
	private double[] generateGaussianData(){
		
		double[] mydata = new double[10000];
		Random random = new Random();
		for (int i=0; i < mydata.length; i++){
			mydata[i] = random.nextGaussian();
			//if (i %1000 == 0)
			//	System.out.println (mydata[i]);
		}
		return mydata;
		
	}
	//void ksone(double[] data, double func(const double), double d, double prob)
//	void ksone(double[] data,  CumulativeDistFunction func, double d, double prob)

	void ksone(double[] data,  Normaldist func)
	{
		int j,n=data.length;
		double dt,en,ff,fn,fo=0.0;
		KSDist ks= new KSDist();;
		Arrays.sort(data);
		en=n;
		d=0.0;
		for (j=0; j<n; j++) {
			fn=(j+1)/en;
			ff=func.cdf(data[j]);  
			dt=Math.max(Math.abs(fo-ff),Math.abs(fn-ff));
			if (dt > d) d=dt;
			fo=fn;
		}
		en=Math.sqrt(en);
		prob=ks.qks((en+0.12+0.11/en)*d);
		System.out.println("\t--------" +d + " "+ prob);
	}
	
	
	void kstwo(double[] data1, double[] data2, double d, double prob)
	{
		int j1=0,j2=0,n1=data1.length,n2=data2.length;
		double d1,d2,dt,en1,en2,en,fn1=0.0,fn2=0.0;
		KSDist ks= new KSDist();
		Arrays.sort(data1);
		Arrays.sort(data2);
		en1=n1;
		en2=n2;
		d=0.0;
		while (j1 < n1 && j2 < n2) {
			if ((d1=data1[j1]) <= (d2=data2[j2]))
				do
					fn1=++j1/en1;
				while (j1 < n1 && d1 == data1[j1]);
			if (d2 <= d1)
				do
					fn2=++j2/en2;
				while (j2 < n2 && d2 == data2[j2]);
			if ((dt=Math.abs(fn2-fn1)) > d) d=dt;
		}
		en=Math.sqrt(en1*en2/(en1+en2));
		prob=ks.qks((en+0.12+0.11/en)*d);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		KSTest tryit = new KSTest();

	}

}
