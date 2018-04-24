'''
Created on Jul 24, 2014

@author: joshua
'''

import argparse, os
import packager

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--startdir', type=str, required=True)
    parser.add_argument('--pkgprefixes', type=str, nargs='+', required=True)
    args = vars(parser.parse_args())
    
    startdir = args['startdir']
    pkgprefixes = args['pkgprefixes']

    for root, dirs, files in os.walk(startdir):
        print "root: ", root
        for currentdir in dirs:
            print "\tdir: ", currentdir
        for currentfile in files:
            if currentfile.endswith("rsf"):
                print "\trsf file: ", currentfile
                filenameTokens = currentfile.split(".")
                filePrefix = filenameTokens[:-1]
                fileSuffix = filenameTokens[-1]
                packagerArgs = "--pkgprefixes " + ' '.join(pkgprefixes) + " --infile " + root + os.sep + currentfile + " --outfile " + root + os.sep + '.'.join(filePrefix) + "_pkgs." + fileSuffix
                print packagerArgs
                packager.main(packagerArgs.split(' '))