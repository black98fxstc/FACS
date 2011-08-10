# You must run this make file as

MSG1 = "You must run this as:"
MSG2 = "GTEST=<where you've installed Google Test> GBUILD=<where you've built libgtest.a> make <target>"

DIRS = Logicle PureCLogicle TestLogicle

all: gtest
	@for DIR in $(DIRS); do (cd $$DIR; $(MAKE) $(MFLAGS) ); done;
 
clean:
	@for DIR in $(DIRS); do (cd $$DIR; $(MAKE) $(MFLAGS) clean); done;
 
test: all
	TestLogicle/TestLogicle

gtest:
    ifndef GTEST
	@echo $(MSG1); echo $(MSG2); exit 1
    else ifndef GBUILD
	@echo $(MSG1); echo $(MSG2); exit 1
    endif
