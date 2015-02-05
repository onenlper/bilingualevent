./compile.sh

java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/lpsolve55j.jar:. -Djava.library.path=../lib/ ilp/ILP 0 $1

python python/scorer.py gold.keys.0 event.ilp.0

java -cp ../lib/edu.mit.jwi_2.2.3.jar:. event/postProcess/CrossValidation ilp svm 0
