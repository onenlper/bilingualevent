import os
import sys
import re

def getFscore(str):
    a = str.index('F1:')
    b = str.index('%', a+1)
    return float(str[a+4:b])
    pass

scorer = '/users/yzcchen/tool/Scorer8.01/reference-coreference-scorers/v8.01/scorer.pl'
goldFile = sys.argv[1]
sysFile = sys.argv[2]

#print 'run', goldFile, sysFile

cmd1 = 'perl ' + scorer + ' muc ' + goldFile + ' ' + sysFile
cmd2 = 'perl ' + scorer + ' bcub ' + goldFile + ' ' + sysFile
cmd3 = 'perl ' + scorer + ' ceafe ' + goldFile + ' ' + sysFile

mucLines = os.popen(cmd1).readlines()
bcubLines = os.popen(cmd2).readlines()
ceafeLines = os.popen(cmd3).readlines()
print ''
print "MUC:\t" + re.sub('\([^\)]*\)', '', mucLines[-2].strip('\n'))
muc = getFscore(mucLines[-2])
print "BCUB:\t" + re.sub('\([^\)]*\)', '', bcubLines[-2].strip('\n'))
bcubed = getFscore(bcubLines[-2])
print "CEAFE:\t" + re.sub('\([^\)]*\)', '', ceafeLines[-2].strip('\n'))
ceafe = getFscore(ceafeLines[-2])
avg = (muc + bcubed + ceafe)/3.0
print "AVG-F: ", str(avg) + '%'

