./compile.sh

java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrain 0
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTest 0

python python/scorer.py gold.keys.0 baseline.keys.0

java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrain 1
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTest 1

python python/scorer.py gold.keys.1 baseline.keys.1

java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrain 2
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTest 2

python python/scorer.py gold.keys.2 baseline.keys.2

java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrain 3
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTest 3

python python/scorer.py gold.keys.3 baseline.keys.3

java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrain 4
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTest 4

python python/scorer.py gold.keys.4 baseline.keys.4

cat gold.keys.0 gold.keys.1 gold.keys.2 gold.keys.3 gold.keys.4 > gold.keys.all
cat baseline.keys.0 baseline.keys.1 baseline.keys.2 baseline.keys.3 baseline.keys.4 > baseline.keys.all

python python/scorer.py gold.keys.all baseline.keys.all



#cd /users/yzcchen/tool/Scorer8.01/reference-coreference-scorers/v8.01
#perl scorer.pl muc /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/gold.keys.all /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/baseline.keys.all

