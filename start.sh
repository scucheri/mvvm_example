#!/bin/bash

export illegalUserException=99
export syncClientExecException=100
export killNotAllowException=101
export pullFailureException=102
export revertCodeException=103
export rsyncException=104
export buildFailureException=105

source .remoteX/code/local/utils.sh
source .remoteX/code/local/utils_inc_install.sh
source .remoteX/code/local/data_collect.sh
source .remoteX/code/local/data_collect_hermes.sh
source .remoteX/code/local/checkUpgrade.sh
source .remoteX/code/local/trycatch.sh
source .remoteX/code/local/error_handle.sh

if [ "$1" == "--version" ]; then
  VERSION=$(cat .remoteX/code/local/data_collect.sh | grep "VERSION=" | cut -d '=' -f2 | sed 's/\"//g')
  FLAVOR_TYPE=$(cat .remoteX/code/local/data_collect.sh | grep "FLAVOR_TYPE=")
  if [ -n "$FLAVOR_TYPE" ]; then
    echo "RemoteX version $VERSION cloud_workspace architecture"
  else
    echo "RemoteX version $VERSION old architecture"
  fi
  exit 0
fi

# check release status with git and user
if [ "$1" == "selfUpdated" ]; then
  eval set -- ${@:2}
else
  tryToRunSelfUpdate $@
fi

echo "===================================================================================="
echo "===================== Welcome To RemoteX By Client Infrastructure-DevOps ==========="
echo "===================================================================================="
echo ""

START_TIME="$(date +%s)"

echoHelp
writeLocalProjectDir

PROJECT_DIR="$(pwd)"
PROJECT_PARENT_DIR="$(dirname "$PROJECT_DIR")"
LOCAL_WORK_DIR="$PROJECT_DIR/.remoteX/code"
LOCAL_WORK_DIR_CUSTOM="$PROJECT_DIR/.remoteX/custom"
SAVED_USER_COMMAND=$@

function readConfigProperty() {
  grep "^${1}=" "$LOCAL_WORK_DIR/remote_machine_info" | cut -d'=' -f2
}

function mainRepoRelativePath() {
  local androidRepoRelativePath=$(readOptionValue 'gradle.properties' 'remoteX.android.repo.relative.path')
  if [ -z "$androidRepoRelativePath" ]; then
    androidRepoRelativePath=$(readOptionValue '.remoteX/custom/remotex.properties' 'android.repo.relative.path')
  fi

  if [ -n "$androidRepoRelativePath" ]; then
    echo $androidRepoRelativePath
  fi
}

function stopUserBuild() {
  COMMAND=" chmod +x .remoteX/code/local/killGradleDaemon.sh && .remoteX/code/local/killGradleDaemon.sh $USER_EMAIL  "
  ignoreErrors
  execCmdToRemote "$REMOTE_DIR" "$COMMAND"
  echo ""
  echo "Stop Success"
  exit 0
}

function handle_trap() {
  echo "用户取消编译，结束用户远程的编译中...."
  # Hermes 用户在增量包生效时，有可能会终止 APK 构建
  # 因此在用户终止 APK 编译的时候，判断如果增量包编译成功（编译时间大于0），也上报数据
  initHermesCompileTime $PROJECT_DIR $HERMES_START_TIME
  if [ $HERMES_COMPILE_TIME -gt 0 ]; then
    # 用户终止 APK 编译
    APK_COMPILE_STATUS=2
    collectHermesDataToVision &
  fi
  stopUserBuild
}

function pullCodeGen() {
  PULL_DIR=$1

  if [ -z "$PULL_DIR" ]; then
    mkdir -p build
    set +e

    #pull switch.local.yml
    pullFileOrDir -f switch.local.yml $RSYNC_SERVER $ANDROID_REPO_NAME $PROJECT_DIR / >/dev/null 2>&1
    #pull pack res
    pullFileOrDir -f build/packRes.zip $RSYNC_SERVER $ANDROID_REPO_NAME $PROJECT_DIR build/ >/dev/null 2>&1
  else
    for PULL_DIR in "$@"
    do
      echo "pull project path is: $PULL_DIR"
      pullFileOrDir -f $PULL_DIR/ $RSYNC_SERVER $ANDROID_REPO_NAME $PROJECT_DIR "$PULL_DIR/" >/dev/null 2>&1
      echo ""
    done
  fi

  if [ $? != 0 ]; then
    echo ""
    echo "[31mpull code gen source failure!!! [0m"
    exit 1
  else
    if [ -z "$PULL_DIR" ]; then
      unzip -o build/packRes.zip -d ./
    fi
    echo ""
    echo "[32mPull code gen success [0m"
    exit 0
  fi
}

function pullAndReadApkDirFromFile() {
  if [ ! -d "build" ]; then
    mkdir build
  fi
  # pull & read gradle generated apk_path file
  local path_file="apk_dir_path.txt"
  pullFileOrDir -f $path_file $RSYNC_SERVER $ANDROID_REPO_NAME $PROJECT_DIR build/ >/dev/null 2>&1
  if [ $? == 0 ]; then
    if [ -f "build/$path_file" ]; then
      local apk_dir_path=$(cat "build/$path_file")
      echo "$apk_dir_path"
    fi
  fi
}

function tryInstallLocalApk() {
  local apk_path=$1
  local path_file="apk_dir_path.txt"
  if [ -z "$apk_path" ]; then
    if [ -f "$LOCAL_WORK_DIR_CUSTOM/apk_path.txt" ]; then
      apk_path=$(readCustomApkPath "$LOCAL_WORK_DIR_CUSTOM/apk_path.txt")
      #echo "apk_path.txt: $apk_path"
    fi

    if [ -z "$apk_path" ]; then
      if [ -f "build/$path_file" ]; then
        apk_path=$(cat "build/$path_file")
        #echo "build/: $apk_path"
      fi
    fi
  fi
  installAndLaunchApk $apk_path
}

function generateSyncRepoParam() {
  local command
  # 主仓 .remoteX/custom/rsync_local_include_file.txt 文件
  if [ -f "$LOCAL_WORK_DIR_CUSTOM/rsync_local_include_file.txt" ]; then
    command+=" --include-from='$LOCAL_WORK_DIR_CUSTOM/rsync_local_include_file.txt' "
  fi
  # 主仓 .remoteX/custom/rsync_local_ignore_file.txt 文件
  if [ -f "$LOCAL_WORK_DIR_CUSTOM/rsync_local_ignore_file.txt" ]; then
    command+=" --exclude-from='$LOCAL_WORK_DIR_CUSTOM/rsync_local_ignore_file.txt' "
  fi
  # 主仓主 ignore 文件 .remoteX/code/rsync_local_ignore_file.txt
  if [ -f "$LOCAL_WORK_DIR/rsync_local_ignore_file.txt" ]; then
    command+=" --exclude-from='$LOCAL_WORK_DIR/rsync_local_ignore_file.txt' "
  fi
  echo $command
}

function syncRepoInParallel() {
  local repo_list=("$@")
  local home_dir="$HOME/.remoteX"
  if [ ! -d "$home_dir" ]; then
    mkdir -p $home_dir
  fi

  _fifofile="$home_dir/$$.fifo"
  mkfifo $_fifofile  # 创建一个FIFO类型的文件
  exec 6<>$_fifofile # 将文件描述符6写入 FIFO 管道， 这里6也可以是其它数字
  rm -rf $_fifofile  # 删也可以，

  degree=5 # 定义并行度

  # 根据并行度设置信号个数
  # 事实上是在fd6中放置了$degree个回车符
  local i
  for ((i = 0; i < ${degree}; i++)); do
    echo
  done >&6

  TEMP_STATE_TIME="$(date +%s)"
  local repo
  for repo in "${repo_list[@]}"; do
    # 从管道中读取（消费掉）一个字符信号
    # 当FD6中没有回车符时，停止，实现并行度控制
    read -u6
    {
      if [ "$repo" == "main" ]; then
        syncMainRepo
      else
        syncSubRepo $repo
      fi
      echo >&6 # 当进程结束以后，再向管道追加一个信号，保持管道中的信号总数量
    } &
  done

  wait      # 等待所有任务结束
  exec 6>&- # 关闭管道
  TIME_RSYNC_DIFF_SUB_REPO="$(($(date +%s) - TEMP_STATE_TIME))"
  TIME_RSYNC_DIFF_SUB_REPO=$((TIME_RSYNC_DIFF_SUB_REPO - TIME_RSYNC_DIFF_MAIN))
}

function syncFileMeta() {
  local fileMetaPath=$1
  local command=$(generatorSyncCommand "$RSYNC_SERVER" "$PROJECT_DIR/$fileMetaPath" "logs")
  echo ""
  echo "increment sync file attr → remote machine (增量同步文件属性): $PROJECT_DIR/$fileMetaPath"
  doRsync "$command"
}

function syncMainRepo() {
  ## rsync 同步主仓及子仓代码
  TEMP_STATE_TIME="$(date +%s)"
  local main_repo_dir="$PROJECT_DIR"
  MAIN_REPO_RELATIVE_PATH=$(mainRepoRelativePath)
  if [ -n "$MAIN_REPO_RELATIVE_PATH" ]; then
    main_repo_dir=${PROJECT_DIR%/$MAIN_REPO_RELATIVE_PATH}
  fi
  local command=$(generatorSyncCommand "$RSYNC_SERVER" "$main_repo_dir/" "$MAIN_REPO_NAME/")
  command+=" $(generateSyncRepoParam)"
  echo ""
  echo "increment sync main repo  →  remote machine （增量同步主仓文件）：$PROJECT_DIR "
  doRsync "$command"
  TIME_RSYNC_DIFF_MAIN="$(($(date +%s) - TEMP_STATE_TIME))"
}

function syncSubRepo() {
  # 同步子仓
  local repo_name=$1
  if [[ $repo_name == */* ]]; then
    local real_repo_name=${repo_name##*/}
    if [[ "$repo_name" != */ ]]; then
      repo_name="$repo_name/"
    fi
    local command=$(generatorSyncCommand "$RSYNC_SERVER" "$repo_name" "$real_repo_name/")
  else
    local command=$(generatorSyncCommand "$RSYNC_SERVER" "$PROJECT_PARENT_DIR/$repo_name/" "$repo_name/")
  fi
  command+=" $(generateSyncRepoParam)"
  echo ""
  echo "increment rsync sub repo  →  remote machine （增量同步子仓文件）： $PROJECT_PARENT_DIR/$repo_name "
  doRsync "$command"
}

function operateWorkspace() {
  local operation=$1
  .remoteX/code/local/remoteXClient.sh "none" >/dev/null
  port=$(cat $HOME/.remoteX/port_$BUILD_TYPE.txt)
  result=$(curl --connect-timeout 2 -sS -X POST -H "Content-Type: application/json" http://127.0.0.1:$port/workspace \
    -d '{"repoUrl":"'$(readConfigProperty "repoUrl")'", "workingDir":"'$PROJECT_DIR'", "email":"'$(readConfigProperty "user")'", "wsid":"'$(readConfigProperty "wsid")'", "operation":"'$operation'"}')
  echo $result
}

function collectRsyncError() {
  local rsync_fail_code=$1
  local rsync_fail_cause=$2
  local operation="rsync"
  .remoteX/code/local/remoteXClient.sh "none" >/dev/null
  port=$(cat $HOME/.remoteX/port_$BUILD_TYPE.txt)
  body="
    {
      \"email\":\"$(readConfigProperty 'user')\",
      \"repoUrl\":\"$(readConfigProperty 'repoUrl')\",
      \"wsid\":\"$(readConfigProperty 'wsid')\",
      \"machine_server\":\"$(readConfigProperty 'machineServer')\",
      \"rsync_proxy\":\"$(readConfigProperty 'rsyncProxy')\",
      \"rsync_server\":\"$(readConfigProperty 'rsyncServer')\",
      \"host_ip\":\"$(readConfigProperty 'hostIp')\",
      \"workingDir\":\"$PROJECT_DIR\",
      \"operation\":\"$operation\",
      \"rsync_fail_code\":$rsync_fail_code,
      \"rsync_fail_cause\":\"$rsync_fail_cause\"
    }
  "
  curl --connect-timeout 2 -s -o /dev/null -X POST -H "Content-Type: application/json" http://127.0.0.1:$port/workspace -d "$body"
}

function checkWSStatus() {
  local wsid=$(readConfigProperty "wsid")
  local waitStep=1
  while true; do
    isSlept=$(operateWorkspace "isSlept")
    if [ "$isSlept" == "no" ] && [ $waitStep -le 10 ]; then
      code=1
      sleep 1
      let waitStep++
    else
      code=0
      break
    fi
  done
  return $code
}

function execCmdToRemote() {
  execCommandWithRPC "$MACHINE_SERVER" "$BUILD_TYPE" "$USER_EMAIL" "$REPO_GIT_URL" "$WORKSPACE_ID" "$1" "\"$2\""
  return $?
}

function copyWorkspaceFile() {
  if [ $# -eq 5 ]; then
    wsid="$3"
    src_path="$4"
    dest_path="$5"
  else
    wsid=$(readConfigProperty "wsid")
    src_path="$3"
    dest_path="$4"
  fi
  bash "$LOCAL_WORK_DIR/local/ws_client.sh" "copy" "$BUILD_TYPE" "$wsid" "$src_path" "$dest_path"
}

function getMetaFileName() {
  local port=$(cat $HOME/.remoteX/port_$BUILD_TYPE.txt)
  local metaFileName=$(curl --connect-timeout 10 -s -X POST -d $PROJECT_DIR http://127.0.0.1:$port/fileMeta/name 2>/dev/null)
  if [ $? == 0 ]; then
    echo "$metaFileName"
  fi
}

function collectMetaFileInfo() {
  echo ""
  logColorInfo ">>>>>> start to collect local file meta info... <<<<<<"
  startTime="$(date +%s)"
  port=$(cat $HOME/.remoteX/port_$BUILD_TYPE.txt)
  metaFileName=$(curl --connect-timeout 10 -s -X POST -d $PROJECT_DIR http://127.0.0.1:$port/fileMeta/collect)
  costTime="$(($(date +%s) - startTime))"

  metaFilePath="build/${metaFileName}"
  logColorInfo "local file meta info: $metaFilePath"
  logColorInfo ">>>>>> collect local file meta info end! cost: ${costTime}s <<<<<<"
  mkdir build &>/dev/null
  echo "$costTime" >build/collect_meta_file_time
}

if [ -f "$PROJECT_DIR/script/git/gstat.sh" ]; then
  bash "$PROJECT_DIR"/script/git/gstat.sh >/dev/null 2>&1 &
fi

if [[ $1 == "update" ]]; then
  checkUpgradeWithoutGray
  if [ $? == 1 ]; then
    echo "No need to update, it is already the latest version!"
  fi
  exit 0
fi

if [[ $1 == "rollback" ]]; then
  echo "start switching back to the old architecture..."
  forceRollback
  echo "switch done!"
  exit 0
fi

if [[ $1 == "gray" ]]; then
  echo "start switching to the new architecture..."
  forceGray
  echo "switch done！welcome to use! see detail: https://bytedance.feishu.cn/docx/doxcnqPleRWRTeRQ52VdcKPCv7t"
  exit 0
fi

if [[ $1 == "install" ]]; then
  tryInstallLocalApk $2
  exit 0
fi

if [[ $1 == "openNotice" ]]; then
  writeOptionValue 'lark.notice.enabled' 'true'
  echo "open lark notice successfully!"
  exit 0
fi

if [[ $1 == "closeNotice" ]]; then
  writeOptionValue 'lark.notice.enabled' 'false'
  echo "close lark notice successfully!"
  exit 0
fi

if [[ $1 == "workspace" ]]; then
  if [[ $2 == "console" ]]; then
    wsid="${3:-$(readConfigProperty "wsid")}"
    bash "$LOCAL_WORK_DIR/local/ws_client.sh" "console" "$BUILD_TYPE" "$wsid"
  elif [[ $2 == "copy" ]]; then
    copyWorkspaceFile "$@"
  elif [[ $2 == "cmd" ]]; then
    cmd="${@:3}"
    wsid=$(readConfigProperty "wsid")
    bash "$LOCAL_WORK_DIR/local/ws_client.sh" "cmd" "$BUILD_TYPE" "$wsid" "$cmd"
  else
    echo "workspace operation: $1 start, please wait..."
    operateWorkspace $2
  fi
  exit 0
fi

## #1 add hermes build
if [ -f "$PROJECT_DIR/hermes.sh" ]; then
  ignoreErrors
  echo "init build： hermes （because root dir has hermes.sh）"
  HERMES_START_TIME="$(date +%s)"
  chmod +x hermes.sh && ./hermes.sh -c -r $@
  HERMES_FINISH_TIME="$(date +%s)"
  echo "init hermes cost time： $((HERMES_FINISH_TIME - HERMES_START_TIME))"
  throwErrors
fi

# 生成本地配置
echo "generating local config..."
TEMP_STATE_TIME="$(date +%s)"

/bin/bash .remoteX/code/local/remoteXClient.sh
if [ $? != 0 ]; then
  echo "你可以使用后面的链接来管理自己的 RemoteX 云工作区 (You can use this link to manage RemoteX Cloud Workspace): https://remotex-workspace.bytedance.net/workspace"
  echo "客户端同步失败，请先解决错误！如果解决不了，请去 lark 咨询 RemoteX Oncall 协助解决！ (client sync failed, please solve problem first! If you can't solve it yourself,please lark RemoteX oncall)"
  error_words="
  客户端同步失败，请先对照检查错误：

  1. 错误：workspace num is over limit; 解决：可使用工作区可视化前端链接 https://remotex-workspace.bytedance.net/workspace 删除长时间不使用 workspace，再重试即可！当前默认每个人最多拥有 5个 workspace.
  2. 错误：JAVA_HOME is not set and no 'java' command could be found in your PATH; 解决：请自查 java home 是否设置并设置的路径是否有效，设置后再重试！
  3. 错误：local server execute failed; 解决：请先重试，如果不行执行 pkill java && rm -rf ~/.remoteX 完再重试！
  "
  saveError $PROJECT_DIR "$SAVED_USER_COMMAND" "$error_words" "" "客户端同步失败，请先按照提示解决！如果不能解决，可以 Oncall 协助解决！"
  # kill daemon process
  ps -ef | grep java | grep "sync-client-k8s-$BUILD_TYPE.jar" | awk '{print $2}' | xargs -I {} kill -9 {}
  find ~/.remoteX -type f ! -path logs -delete
  throw ${syncClientExecException}
fi

throwErrors
TIME_GENERATOR_CONFIG="$(($(date +%s) - TEMP_STATE_TIME))"

MACHINE_SERVER=$(readConfigProperty "machineServer")
K_RSYNC_PROXY=$(readConfigProperty "rsyncProxy")
RSYNC_SERVER=$(readConfigProperty "rsyncServer")
REMOTE_MACHINE_IP=$(readConfigProperty "hostIp")
WORKSPACE_ID=$(readConfigProperty "wsid")
USER_EMAIL=$(readConfigProperty "user")
MAIN_REPO_NAME=$(readConfigProperty "mainRepoName")
REMOTE_DIR="/data00"
JAVA_TMP_DIR="/data00/tmpdir"

REPO_GIT_URL=$(readConfigProperty "repoUrl")
REPO_USER_EMAIL=$USER_EMAIL
USER_INPUT_COMMAND="$@"

if checkIfDouyinRepo $REPO_GIT_URL; then
  # 如果是抖音仓库，异步收集修改文件信息
  collectModifiedFileInfo $PROJECT_DIR $USER_EMAIL $@ &
fi

if [ -z "$REMOTE_MACHINE_IP" ]; then
  REMOTE_MACHINE_IP=$(execCmdToRemote "$REMOTE_DIR" 'echo \$REMOTEX_HOST_IP')
fi

MAIN_REPO_RELATIVE_PATH=$(mainRepoRelativePath)
ANDROID_REPO_NAME="$MAIN_REPO_NAME"
if [ -n "$MAIN_REPO_RELATIVE_PATH" ]; then
  ANDROID_REPO_NAME="$MAIN_REPO_NAME/$MAIN_REPO_RELATIVE_PATH"
fi

# 抛出 rsync proxy 环境
export RSYNC_PROXY=$K_RSYNC_PROXY

try
(
  if [[ -z "$USER_EMAIL" || "$USER_EMAIL" != *@bytedance.com ]]; then
    echo "用户名不存在或不正确，请检查 .remoteX/code/remote_machine_info 文件内容!"
    throw ${illegalUserException}
  fi
  ## check user input command and if hit to skip
  INPUT_ARGUS=$(echo "$@" | awk '{print tolower($0)}')
  if [[ "$INPUT_ARGUS" == kill* || "$INPUT_ARGUS" == killall* ]]; then
    echo "Custom Kill Not allowed, your command is: $@"
    echo "请使用 ./gradlew --stop"
    echo ""
    throw ${killNotAllowException}
  elif [[ $1 == "pull" ]]; then
    if [ $3 == "--batch" ]; then
      # ./start.sh pull -f --batch --dest=build/ a/a.apk b/b.apk c/c.apk ...
      # --batch: multiple files, --dest: one destination path [optional]
      # or pullBatchFiles -f tiger@10.11.12.13 /data00/xxx@bytedance.com/project/ /local_path --dest=build/ a/a.apk b/b.apk c/c.apk ...
      pullBatchFiles $2 $RSYNC_SERVER $ANDROID_REPO_NAME $PROJECT_DIR ${@:4}
    else
      # ./start.sh pull -f src dest
      pullFileOrDir $2 $3 $RSYNC_SERVER $ANDROID_REPO_NAME $PROJECT_DIR $4
    fi

    if [ $? -ne 0 ]; then
      echo "pull failure"
      echo ""
      throw ${pullFailureException}
    else
      echo "pull success"
      echo ""
      exit 0
    fi
  elif [[ $1 == "cleanBuildCache" ]]; then
    echo "start cleaning up gradle buildCache directories..."
    execCmdToRemote "$REMOTE_DIR" "rm -rf .gradle/caches/build-cache-1"
    execCmdToRemote "$REMOTE_DIR" "rm -rf .gradle/build-cache/*"
    echo "delete build cache done"

    execCmdToRemote "$REMOTE_DIR" "rm -rf .gradle/androidCache/*"
    echo "delete android cache done"
    echo ""
    exit 0
  elif [[ $1 == "clean" ]]; then
    echo "start cleaning workspace..."
    execCmdToRemote "$REMOTE_DIR" "rm -rf *"
    echo "rm -rf done"
    echo ""
    exit 0
  elif [[ $1 == "cleanGradle" ]]; then
    echo "start cleaning gradle home..."
    execCmdToRemote "$REMOTE_DIR" "rm -rf .gradle/"
    echo "delete done!"
    exit 0
  elif [[ $1 == "prune" ]]; then
    echo "start pruning..."
    PRUNE_CMD="find tmpdir -type f -amin +180 -delete; find .gradle/caches/build-cache-1 -type f -amin +180 -delete &>/dev/null; find $ANDROID_REPO_NAME/.gradle/build-cache -type f -amin +180 -delete &>/dev/null;"
    PRUNE_CMD+=" echo 'cleanup cache done!';"
    PRUNE_CMD+=" echo 'check the space occupied by workspace.';"
    PRUNE_CMD+=" df -h /data00"
    execCmdToRemote "$REMOTE_DIR" "$PRUNE_CMD"
    echo -e "\nprune done!"
    exit 0
  elif [[ $1 == "apkLink" ]]; then
    echo "uploading apk to tos..."
    execCmdToRemote "$REMOTE_DIR" "cd $ANDROID_REPO_NAME && bash $REMOTE_DIR/.remoteX/code/local/gen_apk_link.sh $ANDROID_REPO_NAME $USER_EMAIL"
    exit 0
  elif [[ $1 == pullCodeGen ]]; then
    pullCodeGen ${@:2}
  fi

  # rsync 同步配置文件至Server
  TEMP_STATE_TIME="$(date +%s)"
  COMMAND=$(generatorSyncCommand "$RSYNC_SERVER" "$PROJECT_DIR/.remoteX")
  COMMAND+=" --include='*/' "
  COMMAND+="--include='code/config.json' "
  COMMAND+="--include='code/local_dir.txt' "
  COMMAND+="--include='code/remote_revert_repo.py' "
  COMMAND+="--include='code/local/killGradleDaemon.sh' "
  COMMAND+="--include='code/local/env_set.sh' "
  COMMAND+="--include='code/local/upload_apk.sh' "
  COMMAND+="--include='code/local/gen_apk_link.sh' "
  COMMAND+="--include='custom/custom_before_exec.sh' "
  COMMAND+="--include='code/local/file_meta.sh' "
  if [ -f "$LOCAL_WORK_DIR/ws_phase.txt" ]; then
    COMMAND+="--include='code/ws_phase.txt' "
  fi
  COMMAND+="--exclude='*' "
  echo "sync local config  →  remote machine (同步本地配置文件到Server)..."
  doRsync "$COMMAND"
  rsync_code=$?
  if [ $rsync_code != 0 ]; then
    if [ $rsync_code == 100 ]; then
      echo -e "\033[31m[error] found rsync Read-only file system error!\033[0m"
      collectRsyncError $rsync_code "Read-only file system"
      operateWorkspace "sleep"
      if checkWSStatus; then
        exec /bin/bash start.sh "selfUpdated" $@
      else
        throw ${rsyncException}
      fi
    else
      throw ${rsyncException}
    fi
  fi

  TIME_RSYNC_CONFIG="$(($(date +%s) - TEMP_STATE_TIME))"

  REPO_PREPARE_STATE_TIME="$(date +%s)"
  metaFileName=$(getMetaFileName)
  if [ ! -f "build/$metaFileName" ]; then
    collectMetaFileInfo &
    collectMetaBg=$!
  fi

  # 执行仓库还原 & custom config
  TEMP_STATE_TIME="$(date +%s)"
  COMMAND="python .remoteX/code/remote_revert_repo.py "
  if [[ $1 == "-d" ]]; then
    COMMAND+="True"
    USER_INPUT_COMMAND="${@:2}"
    eval set -- $USER_INPUT_COMMAND
  else
    COMMAND+="False"
  fi

  gitLFSFetchInclude=$(readOptionValue 'gradle.properties' 'remoteX.git.lfs.fetchinclude')
  if [ -z "$gitLFSFetchInclude" ]; then
    gitLFSFetchInclude=$(readOptionValue '.remoteX/custom/remotex.properties' 'git.lfs.fetchinclude')
  fi
  if [ -n "$gitLFSFetchInclude" ]; then
    COMMAND+=" $gitLFSFetchInclude"
  fi

  gitLFSFetchExclude=$(readOptionValue 'gradle.properties' 'remoteX.git.lfs.fetchexclude')
  if [ -z "$gitLFSFetchExclude" ]; then
    gitLFSFetchExclude=$(readOptionValue '.remoteX/custom/remotex.properties' 'git.lfs.fetchexclude')
  fi
  if [ -n "$gitLFSFetchExclude" ]; then
    COMMAND+=" $gitLFSFetchExclude"
  fi

  echo ""
  echo "server clone code (server 还原代码)..."

  clone_log="build/clone_log"
  [ -d build ] || mkdir build
  COMMAND="$COMMAND && if [ -f $MAIN_REPO_NAME/build/clone_flag ]; then echo 'yes'; fi"
  execCmdToRemote "$REMOTE_DIR" "$COMMAND" | tee $clone_log
  code=${PIPESTATUS[0]}
  if [ $code -ne 0 ]; then
    throw ${revertCodeException}
  fi
  TIME_REMOTE_CLONE="$(($(date +%s) - TEMP_STATE_TIME))"

  has_clone=$(cat $clone_log 2>/dev/null | tail -n 1)
  if [ "$has_clone" == "yes" ]; then
    # wait for all child processes to finish
    if [ -n "$collectMetaBg" ]; then
      wait $collectMetaBg
    fi

    IS_INCREMENTAL="false"
    if [ -f "build/$metaFileName" ]; then
      TEMP_STATE_TIME="$(date +%s)"
      syncFileMeta "build/${metaFileName}"
      FILE_META_SYNC_TIME="$(($(date +%s) - TEMP_STATE_TIME))"

      logColorInfo ">>>>>> start to change remote file meta info. pls wait for a moment ... <<<<<<"
      TEMP_STATE_TIME="$(date +%s)"
      metaChangeCmd="bash .remoteX/code/local/file_meta.sh $MAIN_REPO_NAME $REMOTE_DIR/logs/${metaFileName}"
      execCmdToRemote "$REMOTE_DIR" "$metaChangeCmd"
      FILE_META_MODIFY_TIME="$(($(date +%s) - TEMP_STATE_TIME))"
    fi

    if [ -f build/collect_meta_file_time ]; then
      FILE_META_COLLECT_TIME=$(cat build/collect_meta_file_time)
    fi
  fi

  REPO_PREPARE_COST_TIME="$(($(date +%s) - REPO_PREPARE_STATE_TIME))"

  if [ -f "$LOCAL_WORK_DIR/subRepos" ]; then
    for line in $(cat $LOCAL_WORK_DIR/subRepos); do
      repo_list+=($line)
    done
  fi

  if [ "${#repo_list[*]}" -gt 0 ]; then
    # 并发同步主仓 + 子仓./
    repo_list+=("main")
    syncRepoInParallel "${repo_list[@]}"
  else
    # 只同步主仓
    syncMainRepo
  fi

  ## razor tree shaking before exec
  ## Owner: @xida
  ## More details: https://bytedance.feishu.cn/docx/PAFXdi7aHoep7gxY1vWcwKEPnfb
  RAZOR_BEFORE_EXEC_STATE_TIME="$(date +%s)"
  RAZOR_BEFORE_EXEC_PATH="$PROJECT_DIR/gradle/razor/before_exec.sh"
  if [ -f "$RAZOR_BEFORE_EXEC_PATH" ]; then
    ignoreErrors
    chmod +x "$RAZOR_BEFORE_EXEC_PATH" && sh "$RAZOR_BEFORE_EXEC_PATH"
    # sync output file to remote
    razorRelativeDir="build/razor"
    relativePath="$razorRelativeDir/external_change_info.json"
    changeInfoPath="$PROJECT_DIR/$relativePath"
    if [ -f "$changeInfoPath" ]; then
      execCmdToRemote "$REMOTE_DIR" "mkdir -p $MAIN_REPO_NAME/$razorRelativeDir"
      command=$(generatorSyncCommand "$RSYNC_SERVER" "$changeInfoPath" "$MAIN_REPO_NAME/$relativePath")
      echo ""
      echo "sync razor change info file → remote machine: $changeInfoPath"
      doRsync "$command"
    fi
    throwErrors
  fi
  RAZOR_BEFORE_EXEC_COST_TIME="$(($(date +%s) - RAZOR_BEFORE_EXEC_STATE_TIME))"

  # 执行命令
  ## 用户自定义命令
  TEMP_STATE_TIME="$(date +%s)"
  REMOTE_COMMAND_SUCCESSFUL="false"
  # 是否需要拉apk包
  IS_INSTALL="false"
  # 是否来着命令行
  IS_FROM_COMMAND="false"

  ## 去除 -i 参数
  if [[ $1 == "-i" ]]; then
    IS_INSTALL="true"
    USER_INPUT_COMMAND="${@:2}"
  fi

  # 去除 ci 参数
  if [[ $1 == "-ci" ]]; then
    IS_FROM_COMMAND="true"
    IS_INSTALL="true"
    USER_INPUT_COMMAND="${@:2}"
  fi

  # Hook 用户输入命令
  ## hook stop参数，自己去stop
  if [[ "$USER_INPUT_COMMAND" == *./gradlew*--stop* ]]; then
    stopUserBuild
  fi

  if [[ ! -f "$LOCAL_WORK_DIR/not_use_gradle_progress" && "$IS_INSTALL" == "false" ]]; then
    CONSOLE_RICH_ON=true
  fi
  DAEMON_MEMORY=$(readOptionValue 'local.properties' 'remoteX.daemon.memory')
  if [ -z "$JAVA_MEMORY" ]; then
    DAEMON_MEMORY=$(readOptionValue 'gradle.properties' 'remoteX.daemon.memory')
  fi
  [ -z "$DAEMON_MEMORY" ] && DAEMON_MEMORY="24g"

  # docker java cpu + IPv6 set
  JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport"
  JAVA_TOOL_OPTIONS+=" -Djava.net.preferIPv6Addresses=true"

  COMMAND="source /etc/profile && export LANG=en_US.utf8 && export LC_CTYPE=en_US.utf8 && export JAVA_TOOL_OPTIONS='$JAVA_TOOL_OPTIONS' && chmod +x .remoteX/custom/custom_before_exec.sh && .remoteX/custom/custom_before_exec.sh $ANDROID_REPO_NAME "
  COMMAND+=" && chmod +x .remoteX/code/local/env_set.sh && .remoteX/code/local/env_set.sh $ANDROID_REPO_NAME $USER_EMAIL $REMOTE_DIR $JAVA_TMP_DIR $PROJECT_DIR $DAEMON_MEMORY"
  COMMAND+=" && export GRADLE_USER_HOME=/data00/.gradle "
  if [ -n "$USER_INPUT_COMMAND" ]; then
    COMMAND+=" && cd $ANDROID_REPO_NAME && $USER_INPUT_COMMAND "
    chmod +x .remoteX/code/local/check_need_pull_apk.sh
    .remoteX/code/local/check_need_pull_apk.sh $MAIN_REPO_NAME $USER_INPUT_COMMAND
  fi

  COMMAND+=" && cd $REMOTE_DIR/$ANDROID_REPO_NAME && chmod +x $REMOTE_DIR/.remoteX/code/local/upload_apk.sh && $REMOTE_DIR/.remoteX/code/local/upload_apk.sh $MAIN_REPO_NAME"
  echo ""
  echo "start exec user command (开始执行用户命令): $@"

  pullApk

  trap handle_trap SIGINT SIGHUP SIGKILL

  # 设置用户执行是否成功
  ignoreErrors
  execCmdToRemote "$REMOTE_DIR" "mkdir -p $JAVA_TMP_DIR && $COMMAND"
  if [ $? == 0 ]; then
    REMOTE_COMMAND_SUCCESSFUL="true"
  fi

  # send to lark
  larkNoticeEnabled=$(readOptionValue '.remoteX/custom/remotex.properties' 'lark.notice.enabled')
  if [[ "$larkNoticeEnabled" == "true" ]]; then
    PYTHON_VERSION=$(pip3 -V 2>&1)
    if [ $? != 0 ]; then
      PYTHON_VERSION=$(pip -V 2>&1)
    fi

    PYTHON='python3'
    if [[ "$PYTHON_VERSION" == *python*2.* ]]; then
      PYTHON='python'
    fi
    $PYTHON .remoteX/code/local/python/send_lark.py $MAIN_REPO_NAME $USER_EMAIL $REMOTE_MACHINE_IP $REMOTE_COMMAND_SUCCESSFUL &
  fi

  throwErrors
  USER_INPUT_COMMAND=$@
  TIME_EXEC_USER_COMMAND="$(($(date +%s) - TEMP_STATE_TIME))"

  # rsync
  ## 回传数据 拉apk
  LOCAL_APK_PATH=""
  if [[ "$REMOTE_COMMAND_SUCCESSFUL" == "true" && $IS_INSTALL == "true" ]]; then
    TEMP_STATE_TIME="$(date +%s)"
    if [ -f "$LOCAL_WORK_DIR_CUSTOM/apk_path.txt" ]; then
      LOCAL_APK_PATH=$(readCustomApkPath "$LOCAL_WORK_DIR_CUSTOM/apk_path.txt")
    fi

    if [ -z "$LOCAL_APK_PATH" ]; then
      ignoreErrors
      LOCAL_APK_PATH=$(pullAndReadApkDirFromFile)
      echo "Read gradle generated apk dir file, apk_dir_path: $LOCAL_APK_PATH"
      throwErrors
    fi

    if [ -z "$LOCAL_APK_PATH" ]; then
      echo ""
      echo "search apk path...."
      APK_START_TIME="$(date +%s)"
      COMMAND="find $REMOTE_DIR/$ANDROID_REPO_NAME -name '*.apk' ! -name '*unsigned*.apk' -print0 | xargs -0 stat -c '%Y %n' | sort -rn | cut -d ' ' -f2 | grep build/outputs | head -1"
      APK_PATH=$(execCmdToRemote "$REMOTE_DIR/$ANDROID_REPO_NAME" "$COMMAND")

      FINISH_TIME="$(date +%s)"
      TIME_FIND_APK_PATH="$(($(date +%s) - TEMP_STATE_TIME))"
      echo "Search Apk Duration(耗时): $(formatTime $TIME_FIND_APK_PATH)"
      echo ""

      if [ -n "$APK_PATH" ]; then
        LOCAL_APK_PATH=$(generatorLocalPath $APK_PATH "$REMOTE_DIR/$ANDROID_REPO_NAME/")
      fi
    fi

    TEMP_STATE_TIME="$(date +%s)"
    if [ -n "$LOCAL_APK_PATH" ]; then
      AFTER=".apk"
      ## 判断是否是配置的目录
      if [[ $LOCAL_APK_PATH != *$AFTER* ]]; then
        LOCAL_APK_DIR=$LOCAL_APK_PATH
      else
        LOCAL_APK_DIR=$(dirname $LOCAL_APK_PATH)
      fi

      if [ ! -d "$PROJECT_DIR/$LOCAL_APK_DIR" ]; then
        mkdir -p $PROJECT_DIR/$LOCAL_APK_DIR
      fi

      ## #2 add hermes build
      if [ -f "$PROJECT_DIR/hermes.sh" ]; then
        ignoreErrors
        chmod +x hermes.sh && $PROJECT_DIR/hermes.sh -c -r -a $REMOTE_COMMAND_SUCCESSFUL $@ &
        throwErrors
      fi

      # 本次rsync是否为增量rsync
      if [ $(find $PROJECT_DIR/$LOCAL_APK_DIR -name '*.apk' | tail -1) ]; then
        IS_INCREMENTAL_RSYNC=true
      else
        IS_INCREMENTAL_RSYNC=false
      fi
      # 是否有prefetch apk为本次rsync加速
      if [ -f "$(pwd)/build/apk/debug.apk" ]; then
        IS_USE_PREFETCH=true
      else
        IS_USE_PREFETCH=false
      fi
      copyApkToReal $PROJECT_DIR $REMOTE_DIR/$ANDROID_REPO_NAME $LOCAL_APK_DIR $MACHINE_SERVER $BUILD_TYPE

      PULL_APK_DIR="$(dirname "$ANDROID_REPO_NAME/$LOCAL_APK_PATH")"
      COMMAND=$(generatorSyncToLocalCommand "$RSYNC_SERVER" "$PULL_APK_DIR/" "$PROJECT_DIR/$LOCAL_APK_DIR/")
      echo "sync remote machine apk  →  to local dir （同步远程APK至本地）: $PROJECT_DIR/$LOCAL_APK_DIR/"
      RSYNC_RESULT_FILE="build/rsync_result_tmp"
      mkdir -p $(dirname $RSYNC_RESULT_FILE)
      eval "$COMMAND | tee $RSYNC_RESULT_FILE"
      RSYNC_RESULT=$(cat $RSYNC_RESULT_FILE)
      rm $RSYNC_RESULT_FILE
      # 过滤出receive数据量
      RECEIVED_BYTE_PATTERN='received ([0-9|,]+) bytes'
      [[ $RSYNC_RESULT =~ $RECEIVED_BYTE_PATTERN ]]
      RSYNC_OUTPUT_RECEIVED_BYTES=${BASH_REMATCH[1]//,/}

      # 过滤出原始Apk大小
      TOTAL_BYTE_PATTERN='total size is ([0-9|,]+)'
      [[ $RSYNC_RESULT =~ $TOTAL_BYTE_PATTERN ]]
      RSYNC_OUTPUT_TOTAL_BYTES=${BASH_REMATCH[1]//,/}

      TIME_PULL_APK="$(($(date +%s) - TEMP_STATE_TIME))"
      echo "Sync Apk Duration(耗时): $(formatTime $TIME_PULL_APK)"
      echo ""
    else
      echo "apk path not found，rsync finish"
    fi
  fi

  # 执行用户自定义脚本
  ignoreErrors
  chmod +x .remoteX/custom/custom_after_exec.sh
  .remoteX/custom/custom_after_exec.sh $RSYNC_SERVER $ANDROID_REPO_NAME $REMOTE_COMMAND_SUCCESSFUL $@

  # added only for plugin install
  if [ -f .remoteX/custom/custom_after_exec_apk.sh ]; then
    chmod +x .remoteX/custom/custom_after_exec_apk.sh
    .remoteX/custom/custom_after_exec_apk.sh $LOCAL_APK_PATH $RSYNC_SERVER $ANDROID_REPO_NAME $REMOTE_COMMAND_SUCCESSFUL $@
  fi

  if [[ "$REMOTE_COMMAND_SUCCESSFUL" == "true" && $IS_FROM_COMMAND == "true" ]]; then
    ignoreErrors
    INSTALL_APK_START_TIME="$(date +%s)"
    # 如果是抖音仓库，且线上配置的是增量安装，则走增量安装逻辑，其他情况，走 RemoteX 的默认安装
    if checkIfDouyinRepo $REPO_GIT_URL && checkUseIncInstall $USER_EMAIL; then
      incInstallAndLaunch $PROJECT_DIR $USER_EMAIL $LOCAL_APK_PATH
    else
      installAndLaunchApk $LOCAL_APK_PATH
    fi
    INSTALL_APK_TIME="$(($(date +%s) - INSTALL_APK_START_TIME))"

    if [ $? -ne 0 ]; then
      echo "======安装APK失败====="
      echo "1. 请确认APK路径是否正确"
      echo "2. 请确认ADB是否配置环境到变量"
      echo ""
    fi
  fi

  # 编译失败拉错误文件至本地
  if [[ "$REMOTE_COMMAND_SUCCESSFUL" == "false" ]]; then
    mkdir -p build
    pullFileOrDir -f error_log.txt $RSYNC_SERVER $MAIN_REPO_NAME $PROJECT_DIR build/ >/dev/null 2>&1
    pullFileOrDir -f build/byte_build_scan.json $RSYNC_SERVER $MAIN_REPO_NAME $PROJECT_DIR build/ >/dev/null 2>&1
    if [ -f "$PROJECT_DIR/build/error_log.txt" ]; then
      ERROR_LOG=$(cat $PROJECT_DIR/build/error_log.txt)
      #    echo -e "\033[31m $ERROR_LOG \033[0m"
      echo "$ERROR_LOG"
    fi
  fi

  if [[ $IS_INSTALL == "true" || $IS_FROM_COMMAND == "true" ]]; then
    ignoreErrors
    COMMAND="cd $ANDROID_REPO_NAME && cat build/byte_build_scan.id && rm -rf build/byte_build_scan.id"
    BUILD_ID=$(execCmdToRemote "$REMOTE_DIR" "$COMMAND")
    if [[ -n $BUILD_ID ]] && [[ $BUILD_ID =~ ^[0-9]+$ ]]; then
      echo ""
      echo "===================================================================================="
      if [ "$REMOTE_COMMAND_SUCCESSFUL" == "true" ]; then
        echo -e "\033[32mHummer Build Link:\033[0m https://hummer.bytedance.net?id=${BUILD_ID}&source=RemoteX"
      else
        echo -e "\033[31mHummer Build Link:\033[0m https://hummer.bytedance.net?id=${BUILD_ID}&source=RemoteX"
      fi
      echo "===================================================================================="
    fi
  fi

  throwErrors
  FINISH_TIME="$(date +%s)"
  echo ""

  TIME_TOTAL="$((FINISH_TIME - START_TIME))"

  ## #3 add hermes build
  if [ -f "$PROJECT_DIR/hermes.sh" ]; then
    ignoreErrors
    chmod +x hermes.sh && $PROJECT_DIR/hermes.sh -c -r -b $REMOTE_COMMAND_SUCCESSFUL $@
    throwErrors
  fi

  ## razor tree shaking after exec
  ## Owner: @xida
  ## More details: https://bytedance.feishu.cn/docx/PAFXdi7aHoep7gxY1vWcwKEPnfb
  RAZOR_AFTER_EXEC_PATH="$PROJECT_DIR/gradle/razor/after_exec.sh"
  if [ -f "$RAZOR_AFTER_EXEC_PATH" ]; then
    ignoreErrors
    chmod +x "$RAZOR_AFTER_EXEC_PATH" && sh "$RAZOR_AFTER_EXEC_PATH" $REMOTE_COMMAND_SUCCESSFUL $@
    throwErrors
  fi

  if [ "$REMOTE_COMMAND_SUCCESSFUL" == "true" ]; then
    echo "[32mSuccess with duration (编译总耗时): $(formatTime $TIME_TOTAL) [0m"
    echo ""
    collectData
    exit 0
  else
    echo "[31mFailure with duration (编译总耗时): $(formatTime $TIME_TOTAL) [0m"
    echo ""
    throw ${buildFailureException}
  fi
)
catch || {
  show_error=true
  case ${ex_code} in
  ${illegalUserException})
    STACK_TRACE="用户名不存在或不是以 @bytedance.com 结尾的！ 请git config user.email 设置你字节邮箱后重试"
    ;;
  ${syncClientExecException})
    STACK_TRACE="sync client 执行失败"
    ;;
  ${killNotAllowException})
    STACK_TRACE="执行 kill 命令阻止，请使用命令 ./gradlew --stop"
    ;;
  ${pullFailureException})
    STACK_TRACE="拉取任意远端编译产物失败，可执行 ./start.sh cat xxx 来检查拉取文件/目录是否在远端存在！或者使用可视化前端 webshell 进去查看 https://remotex-workspace.bytedance.net 拉取产物的命令使用请查看：https://bytedance.feishu.cn/wiki/wikcn4sgLjSUYAEN89R4RDaEGUd"
    ;;
  ${revertCodeException})
    STACK_TRACE="远端服务器还原代码失败，请尝试 push 代码到远端再重试！"
    ;;
  ${rsyncException})
    STACK_TRACE="rsync 同步失败！"
    ;;
  ${buildFailureException})
    STACK_TRACE="编译代码失败, 请查看 hummer 错误链接解决！"
    show_error=false
    ;;
  esac
  # APK 编译异常
  APK_COMPILE_STATUS=0
  collectData
  if [ "$show_error" == true ]; then
    saveError $PROJECT_DIR "$SAVED_USER_COMMAND" "$STACK_TRACE" "" "请先按照提示解决问题， 如果不能解决，可以 Oncall 协助解决！"
  fi
  exit 1
}
