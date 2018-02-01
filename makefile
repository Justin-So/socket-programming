JFLAGS = -g
JC = javac
RM = rm -rf

sources = $(wildcard *.java)
classes = $(sources:.java=.class)

all: $(classes)

%.class: %.java
	$(JC) $(JFLAGS) $^

clean:
	$(RM) *.class
