./compile.sh
#java trigger/chinese/discourse/DiscourseConsistent $1
#java event/preProcess/KnownTrigger $1
#java event/preProcess/ChineseTriggerIndent $1

CP=../lib/edu.mit.jwi_2.2.3.jar:.

#java -cp $CP event/trigger/JointTriggerIndent train svm $1
#java -cp $CP event/trigger/JointTriggerIndent test svm $1

cd /users/yzcchen/tool/svm_multiclass
#./svm_multiclass_learn -c 2000000  /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/Joint_triggersFeature_train$1 JointTriggerModel$1 1>&-

echo train trigger identify
#./svm_multiclass_classify /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/Joint_triggersFeature_test$1 JointTriggerModel$1 JointTriggerOutput_test$1 1>&-

cd /users/yzcchen/chen3/eventBilingual/BilingualEvent/src

#java -cp $CP event/postProcess/JointTriggerEval test svm $1

java -cp $CP event/argument/JointArgument train svm $1
java -cp $CP event/argument/JointArgument test svm $1

cd /users/yzcchen/tool/svm_multiclass
echo train joint argument role labeling$1
echo =============================
./svm_multiclass_learn -c 100000 /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/Joint_argument_trainsvm$1 coling2012/argumentJointModel$1 1>&-

./svm_multiclass_classify /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/Joint_argument_testsvm$1 coling2012/argumentJointModel$1 coling2012/Joint_argument$1 1>&-

cd /users/yzcchen/chen3/eventBilingual/BilingualEvent/src
java -cp $CP event/postProcess/OutputFinalResult joint svm $1
