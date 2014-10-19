import os

base = "/users/yzcchen/ACL12/data/ACE2005/English/"
lst = ['bc', 'bn', 'cts', 'nw', 'un', 'wl']

fw = open('ACE_English_all', 'w')

trainFw = open('ACE_English_train', 'w')
testFw = open('ACE_English_test', 'w')

for fold in lst:
    folder = base + fold + os.path.sep + 'timex2norm/'
    for file in os.listdir(folder):
        if str(file).endswith('.sgm'):
            fw.write(folder + file[0:-4] + '\n')
            
            if "AFP" in file:
                testFw.write(folder + file[0:-4] + '\n')
            else:
                trainFw.write(folder + file[0:-4] + '\n')
            
fw.close()
trainFw.close()
testFw.close()