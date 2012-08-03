package edu.stanford.facs.delaunay;

public class MyPoint {
	
	static final int X=0;
	static final int Y=1;
	private float[] point= new float[2];
	private int[] screen = new int[2];
	protected final static int UND=-1;
	protected final static int OUT=0;
	protected final static int ON = 1;
	protected final static int IN=2;
	protected int  loc = UND;
	protected int frequency=1;
	protected int level=0;
	protected Long id;
	private String name="";
	
	public MyPoint (float x, float y){
		point[X] = x;
		point[Y] = y;
		id = new Long( hashCode());
	}
	public void addOne(){
		frequency++;
	}
	
	public boolean equals (MyPoint pt){
    	if (point[X] == pt.getX()){
    		if (point[Y] == pt.getY()){
    			
				return true;	
    		}
    	}
    	return false;
    }
	
	protected void setName(String n){
		name = n;
	}
	
	protected String getName(){
		return name;
	}
	
	public String toString(){
		
		return ""+name + "-" +point[X]+", "+ point[Y] ;
	}
	
 	
	public int getFreq(){
		return frequency;
	}
	
	public Long getId() {
		return id;
	}
	
	public int getLevel(){
		return level;
	}
	
	public int getLoc (){
		return loc;
	}
	
	
	public float getX(){
		return point[X];
	}
	
	public float getY(){
		return point[Y];
	}
	
	public int getSX(){
		return screen[X];
	}
	
	public int getSY(){
		return screen[Y];
	}

	public int hashCode(){
		 int hash;   
	     hash = java.lang.Float.floatToIntBits (point[X]) ^ java.lang.Float.floatToIntBits(point[Y]);
	     id = new Long(hash);
	    
	     return hash;
		}
	
	
	protected void setLevel(int l){
		level = l;
		System.out.println (toString() + "  "+ level);
	}
	
	public void setLoc (int loc){
		this.loc = loc;
	}
	

	public void setSX(int x){
		screen[X] = x;
	}
	public void setSY(int y){
		screen[Y] = y;
	}
	public void setX(int x){
		screen[X] = x;
	}
	
	public void setY(int y){
		screen[Y]=y;
	}
	
	public void setX (float x){
		point[X]= x;
	}
	
	public void setY (float y){
		point[Y]= y;
	}

}
