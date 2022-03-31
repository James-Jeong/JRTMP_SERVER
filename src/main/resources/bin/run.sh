#!/bin/sh

SERVICE_HOME=/home/uangel/urtmp_server
SERVICE_NAME=urtmp_server
PATH_TO_JAR=$SERVICE_HOME/lib/JRTMP_SERVER.jar
JAVA_CONF=$SERVICE_HOME/config/user_conf.ini
JAVA_OPT="-Dlogback.configurationFile=$SERVICE_HOME/config/logback.xml"
JAVA_OPT="$JAVA_OPT -XX:+UseG1GC -XX:G1RSetUpdatingPauseTimePercent=5 -XX:MaxGCPauseMillis=500 -XX:+UseLargePages -verbosegc -Xms4G -Xmx4G -verbose:gc -Xlog:gc=debug:file=$SERVICE_HOME/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m"

function exec_start() {
        PID=`ps -ef | grep java | grep RtmpServerMain | awk '{print $2}'`
        if ! [ -z "$PID" ]
        then
                echo "RTMP_SERVER is already running"
        else
                ulimit -n 65535
                ulimit -s 65535
                ulimit -u 10240
                ulimit -Hn 65535
                ulimit -Hs 65535
                ulimit -Hu 10240

                /usr/lib/jvm/java-11/bin/java -jar $JAVA_OPT $PATH_TO_JAR RtmpServerMain $JAVA_CONF > /dev/null 2>&1 &
                #/usr/lib/jvm/java-11/bin/java $JAVA_OPT -classpath $PATH_TO_JAR RtmpServerMain $JAVA_CONF > /dev/null 2>&1 &
                echo "$SERVICE_NAME started ..."
        fi
}

function exec_stop() {
	PID=`ps -ef | grep java | grep RtmpServerMain | awk '{print $2}'`
	if [ -z "$PID" ]
	then
		echo "RTMP_SERVER is not running"
	else
		echo "stopping RTMP_SERVER"
		kill "$PID"
		sleep 1
		PID=`ps -ef | grep java | grep RtmpServerMain | awk '{print $2}'`
		if [ ! -z "$PID" ]
		then
			echo "kill -9 ${PID}"
			kill -9 "$PID"
		fi
		echo "RTMP_SERVER stopped"
	fi
}

function exec_status() {
  PID=`ps -ef | grep java | grep RtmpServerMain | awk '{print $2}'`
	if [ -z "$PID" ]
	then
		echo "RTMP_SERVER is not running"
	else
	  ps -aux | grep RtmpServerMain | grep "$PID"
	fi
}

case $1 in
    restart)
		exec_stop
		exec_start
		;;
    start)
		exec_start
    ;;
    stop)
		exec_stop
    ;;
    status)
    exec_status
    ;;
esac
