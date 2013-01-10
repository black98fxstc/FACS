# read the data from triangesAsLines.r 
# Called first
triangles <- function (filename){
	data<-c(read.table(filename, header=T, stringsAsFactors=F)); 
	return(data);
	}
	
xminmax <-function (data){
	xx<-c(min(data$x1, data$x2), max(data$x1, data$x2)); 
	return (xx);
}

yminmax <-function(data){
	yy<-c(min(data$y1, data$y2), max(data$y1, data$y2));
	return (yy);
}

#Arguments are the data, xx is a vector of min and max x values,
# yy is a vector of min and max y values
# max is how many lines are in data
myplot<-function (data,xx,yy, max){ 
	plot.new(); 
	plot(xx,yy,type="n"); 
	i=1; 
	repeat{
		if (i>max) break 
		else {
			lines(x=c(data$x1[i], data$x2[i]), y=c(data$y1[i], data$y2[i]));
			i<-i+1;
			}
	}
}

#read the point data  called ./R/clusters.txt
clusterData <-function (filename){
	cdata<- c(read.table(filename, header=T, stringsAsFactors=F));
	
	return(cdata)
}

# using the data returned from clusterData, draw color dots on the
# points signifying a cluster.  maxx is the number of points.
bplot<- function(data, maxx){
	#plot(xx,yy,type="n");
	library("RColorBrewer")
	usr.col<-rep(brewer.pal(9,"Set1"))
			i=1;
	repeat{
		if(i > maxx) break
		else{
			acolor=(data$cluster[i]+1) %% length(usr.col)
			if (0 ==acolor){
				acolor=length(usr.col)	
				}			  

			#if (data$boundary[i] == "true"){
			#	lines(data$x[i], data$y[i], type="p", pch=20, col="black")
			#	}
		    #else{
			 	lines(data$x[i], data$y[i], type="p", pch=20, col=usr.col[acolor])
			 #	}
	
			i<-i+1
			}
	}
}
