#!/usr/bin/env python
'''
Created on Mar 11, 2014

@author: gaprice@lbl.gov
'''
from __future__ import print_function
import sys
from configobj import ConfigObj
import os
import stat

PORT = 'port'
THREADS = 'server-threads'
MINMEM = 'min-memory'
MAXMEM = 'max-memory'


def printerr(*objs):
    print(*objs, file=sys.stderr)
    sys.exit(1)


def make_executable(path):
    st = os.stat(path)
    os.chmod(path, st.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


def getConfig(param, cfg, cfile):
    if param not in cfg:
        printerr('Missing expected parameter {} in config file {}'
                 .format(param, cfile))
    return cfg[param]

if len(sys.argv) < 8:
    printerr("Missing arguments to build_server_control_scripts")
if len(sys.argv) == 8:
    _, serviceDir, war, target, javaHome, deployCfg, asadmin, serviceDomain =\
        sys.argv
    port = None
else:
    _, serviceDir, war, target, javaHome, deployCfg, asadmin, serviceDomain,\
        port = sys.argv

if not os.path.isfile(deployCfg):
    printerr('Configuration parameter is not a file: ' + deployCfg)
cfg = ConfigObj(deployCfg)
if serviceDomain not in cfg:
    printerr('No {} section in config file {} - '.format(
        serviceDomain, deployCfg))
wscfg = cfg[serviceDomain]

if port is None:
    if PORT not in wscfg:
        printerr("Port not provided as argument or in config")
    port = wscfg[PORT]

threads = getConfig(THREADS, wscfg, deployCfg)
minmem = getConfig(MINMEM, wscfg, deployCfg)
maxmem = getConfig(MAXMEM, wscfg, deployCfg)

with open(os.path.join(serviceDir, 'start_service'), 'w') as ss:
    ss.write('export JAVA_HOME={}\n'.format(javaHome))
    ss.write('export PATH=$JAVA_HOME/bin:$PATH\n')
    ss.write(('JARS={}/lib/jars\n').format(target))
    ss.write('if [ -z "$KB_DEPLOYMENT_CONFIG" ]\n')
    ss.write('then\n')
    ss.write('    export KB_DEPLOYMENT_CONFIG={}/deployment.cfg\n'
             .format(target))
    ss.write('fi\n')
    ss.write(('cd {}\n').format(serviceDir))
    ss.write(('java -cp $JARS/jetty/jetty-start-7.0.0.jar:$JARS/jetty/jetty-all-7.0.0.jar:$JARS/servlet/servlet-api-2.5.jar ' +
        '-Xmx{}m ' +
        '-Djetty.port={} ' +
        '-DKB_DEPLOYMENT_CONFIG=$KB_DEPLOYMENT_CONFIG ' +
        'org.eclipse.jetty.start.Main jetty.xml\n')
        .format(maxmem,port,serviceDir,serviceDir))

with open(os.path.join(serviceDir, 'stop_service'), 'w') as ss:
    ss.write('export JAVA_HOME={}\n'.format(javaHome))
    ss.write('export PATH=$JAVA_HOME/bin:$PATH\n')
    ss.write('export CLASSPATH=\n')
    ss.write('killall java\n')

make_executable(os.path.join(serviceDir, 'start_service'))
make_executable(os.path.join(serviceDir, 'stop_service'))
