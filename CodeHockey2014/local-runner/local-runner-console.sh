RUNNER=local-runner-console.properties

cat $RUNNER | grep 'base-adapter-port' | sed 's/.*=//g' > ololo.txt

read PORT < ololo.txt 

cat $RUNNER | sed "s/$PORT/$1/g" > ololo.txt

cat ololo.txt > $RUNNER

pushd `dirname $0` > /dev/null
SCRIPTDIR=`pwd`
popd > /dev/null

java -cp ".:*:$SCRIPTDIR/*" -jar "local-runner.jar" local-runner-console.properties
