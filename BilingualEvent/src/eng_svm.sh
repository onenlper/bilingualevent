./compile.sh
cp=../lib/edu.mit.jwi_2.2.3.jar:.

java -Xmx32g -cp $cp event/triggerEng/EngTrigger train svm $1
java -Xmx32g -cp $cp event/triggerEng/EngTrigger test svm $1 


cd /users/yzcchen/tool/svm_multiclass
echo train trigger identify
#-c 2000000
./svm_multiclass_learn -c 200000000  /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/engTrFea_train$1 engTrModel$1

echo test trigger identify
./svm_multiclass_classify /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/engTrFea_test$1 engTrModel$1 engTrOutput_test$1 1>&-

cd /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/
java -cp $cp event/triggerEng/EngTriggerEval test svm $1

#java event/triggerEng/EngArg train svm $1
#java event/triggerEng/EngArg test svm $1

#cd /users/yzcchen/tool/svm_multiclass
#echo train argument role labeling

#./svm_multiclass_learn -c 100000 /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/engArgFea_train$1 engArgModel$1 1>&-
#echo test argumetn identify
#./svm_multiclass_classify /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/data/engArgFea_test$1 engArgModel$1 engArOutput_test$1 1>&-

#cd /users/yzcchen/chen3/eventBilingual/BilingualEvent/src/
#java event/triggerEng/EngArgEval test svm $1
