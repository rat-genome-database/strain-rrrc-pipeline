#!/usr/bin/env bash
#
# Strain RRRC pipeline
#
. /etc/profile
APPNAME="strain-rrrc-pipeline"
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "$@" > run.log 2>&1

mailx -s "[$SERVER] Strain RRRC Pipeline Run" mtutaj@mcw.edu < $APPDIR/logs/summary.log
