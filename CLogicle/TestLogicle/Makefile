CXXFLAGS += -I$(GTEST)/include -I../Logicle

LDLIBS += -lm -lpthread

TestLogicle: TestLogicle.o ../Logicle/Logicle.o ../Logicle/FastLogicle.o ../PureCLogicle/logicle.o $(GBUILD)/libgtest.a
	$(LINK.cpp) $^ $(LOADLIBS) $(LDLIBS) -o $@

test: TestLogicle
	./TestLogicle

clean:
	$(RM) TestLogicle.o
	$(RM) TestLogicle

