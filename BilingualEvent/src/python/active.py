import os

def run(cmd):
	os.system(cmd)

print "Run...."
run("rm ACE_Chinese_train6")
run("touch ACE_Chinese_train6")


for i in range(0, 15):

	print "--------ITERATION", i,  "---------"
	print 

	run("./run_semantic_seed.sh 6")

        print "--------ITERATION", i,  "---------"
        print

	run("./run_event_extract_seed.sh 6")

        print "--------ITERATION", i,  "---------"
        print

	run("java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/porter-stemmer-1.4.jar:. active/ActiveSelect")

        print "--------ITERATION", i,  "---------"
        print


#	print "<<<<<<<<<<<<<Apply to ACE TEST DATA"
#	run("./run_event_extract_seed.sh 0")
#	print "Apply to ACE TEST DATA>>>>>>>"

	run("cp ACE_Chinese_train6 ACE_Chinese_train6." + str(i))
