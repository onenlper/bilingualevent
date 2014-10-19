


base = "/users/yzcchen/workspace/Coling2012/src/joint_svm_3extend/result.trigger"

# allFn = open('ACE_Chinese_list').read().splitlines()
# print 'all', len(allFn)
# 
# setFn = set(allFn)
# print 'allSet', len(setFn)

lastFn = ""
all = 0

allFn = []

for i in range(1, 11):
    
    oldFns = open('/users/yzcchen/workspace/Coling2012/src/ACE_Chinese_test' + str(i)).read().splitlines()
    
    f = open(base + str(i))
    fns = []
    for l in f.readlines():
        l = l.strip('\n')
        fn = l.split()[0]
        if fn!=lastFn:
            
#             if fn in allFn:
#                 print fn

            if fn not in oldFns:
                print fn
            
            allFn.append(fn)
            fns.append(fn + '\n')
            
            lastFn = fn
             
    all += len(fns)
    print len(fns)
     
    outF = open('ACE_Chinese_test'+str(i), 'w')
    outF.writelines(fns)
    outF.close()
 
print 'all: ', all
    
    

