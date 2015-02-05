./compile.sh

java -cp ../lib/edu.mit.jwi_2.2.3.jar:.:../lib/ws4j-1.0.1.jar coref/HeursiticBase 0
python python/scorer.py gold.keys.0 base.keys.0

#:<<BLOCK
java -cp ../lib/edu.mit.jwi_2.2.3.jar:.:../lib/ws4j-1.0.1.jar coref/HeursiticBase 1
python python/scorer.py gold.keys.1 base.keys.1

java -cp ../lib/edu.mit.jwi_2.2.3.jar:.:../lib/ws4j-1.0.1.jar coref/HeursiticBase 2
python python/scorer.py gold.keys.2 base.keys.2

java -cp ../lib/edu.mit.jwi_2.2.3.jar:.:../lib/ws4j-1.0.1.jar coref/HeursiticBase 3
python python/scorer.py gold.keys.3 base.keys.3

java -cp ../lib/edu.mit.jwi_2.2.3.jar:.:../lib/ws4j-1.0.1.jar coref/HeursiticBase 4
python python/scorer.py gold.keys.4 base.keys.4

cat base.keys.0 base.keys.1 base.keys.2 base.keys.3 base.keys.4 > base.keys.all
cat gold.keys.0 gold.keys.1 gold.keys.2 gold.keys.3 gold.keys.4 > gold.keys.all

python python/scorer.py gold.keys.all base.keys.all
#BLOCK


#cd /users/yzcchen/tool/Scorer8.01/reference-coreference-scorers/v8.01
#perl scorer.pl muc /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/gold.keys.all /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/sys.keys.all

#cd /users/yzcchen/chen3/eventBilingual/BilingualEvent/src
