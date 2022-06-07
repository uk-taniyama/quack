set -x
if [ ! "$GH_TOKEN" ]
then
    exit -1
fi

OWNER=uk-taniyama
REPOSITORY=quack

curl -s \
    -H "Accept: application/vnd.github.v3+json" \
    -H "Authorization: token ${GH_TOKEN}" \
    "https://api.github.com/repos/${OWNER}/${REPOSITORY}/actions/artifacts" \
    > artifacts.json
cat artifacts.json

download() {
    name=$1
    path=$2
    url=`cat artifacts.json | jq -r "[.artifacts[] | select(.name==\"$name\") | select(.expired==false) ] | sort_by(.created_at) [0].archive_download_url"`
    curl -s -L -H "Authorization: token ${GH_TOKEN}" $url -o work.zip
    ls -l work.zip
    mkdir -p $path
    unzip work.zip -d $path
}

download quickjs-windows quack-jni/src/main/resources/META-INF/
download quickjs-linux quack-jni/src/main/resources/META-INF/
download quickjs-macos quack-jni/src/main/resources/META-INF/
download quickjs-android build/
