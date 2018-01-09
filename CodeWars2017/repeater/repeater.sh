pushd `dirname $0` > /dev/null
SCRIPTDIR=`pwd`
popd > /dev/null

java -Xms128M -Xmx2G -cp ".:*:$SCRIPTDIR/*" -jar repeater.jar $1 3f17a285bd73234a501e8582ad2a66491d49648e_0