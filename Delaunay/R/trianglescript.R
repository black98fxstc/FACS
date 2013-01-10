file="/Users/cate2/Eclipse/workspace/Newone/Delaunay/trianglesAsLines.r"
data<-c(read.table(file, header=TRUE, stringsAsFactors=FALSE))
xx<-c(min(data$x1, data$x2),max(data$x1,data$x2))
yy<-c(min(data$y1, data$y2),max(data$y1,data$y2))
len<-length(data$x1)


