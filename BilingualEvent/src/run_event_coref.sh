./compile.sh

java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrainMentionPair 0 svm

cd /users/yzcchen/tool/svm_multiclass
./svm_multiclass_learn -c 10000000 ~/chen3/eventBilingual/BilingualEvent/src/eventTrain0 eventCorefModel0

cd /users/yzcchen/chen3/eventBilingual/BilingualEvent/src
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTestMentionPair 0 prepare

cd /users/yzcchen/tool/svm_multiclass
./svm_multiclass_classify ~/chen3/eventBilingual/BilingualEvent/src/eventTest0 eventCorefModel0 /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/eventPredict0

cd /users/yzcchen/chen3/eventBilingual/BilingualEvent/src
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTestMentionPair 0 load

python python/scorer.py gold.keys.0 baselineMP.keys.0

#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrainMentionPair 1
#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTestMentionPair 1

#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrainMentionPair 2
#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTestMentionPair 2

#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrainMentionPair 3
#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTestMentionPair 3

#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTrainMentionPair 4
#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/ws4j-1.0.1.jar:../lib/stanford-classifier-3.2.0.jar:. event/supercoref/EventCorefTestMentionPair 4

#cat baselineMP.keys.0 baselineMP.keys.1 baselineMP.keys.2 baselineMP.keys.3 baselineMP.keys.4 > baselineMP.keys.all
#python python/scorer.py gold.keys.all baselineMP.keys.all


#cd /users/yzcchen/tool/Scorer8.01/reference-coreference-scorers/v8.01
#perl scorer.pl muc /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/gold.keys.all /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/baseline.keys.all

