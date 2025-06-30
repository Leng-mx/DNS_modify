@echo off
echo ========================================
echo DNS中继服务器测试脚本
echo ========================================
echo.

echo 1. 检查Java环境...
java -version
if %errorlevel% neq 0 (
    echo 错误：未找到Java环境，请安装Java 8或更高版本
    pause
    exit /b 1
)
echo.

echo 2. 检查项目文件...
if not exist "target\dns-relay.jar" (
    echo 错误：未找到dns-relay.jar文件，请先运行 mvn package
    pause
    exit /b 1
)

if not exist "config\dnsrelay.txt" (
    echo 错误：未找到配置文件，请检查config\dnsrelay.txt
    pause
    exit /b 1
)
echo 项目文件检查完成
echo.

echo 3. 显示配置文件内容...
echo --- config\dnsrelay.txt ---
type config\dnsrelay.txt
echo --- 配置文件结束 ---
echo.

echo 4. 测试程序帮助信息...
java -jar target\dns-relay.jar -h
echo.

echo 5. 准备启动DNS中继服务器...
echo 注意：程序需要管理员权限才能绑定53端口
echo 如果出现权限错误，请以管理员身份运行此脚本
echo.
echo 启动参数：调试模式，上游DNS=8.8.8.8，配置文件=config\dnsrelay.txt
echo 按任意键开始启动服务器...
pause > nul
echo.

echo 6. 启动DNS中继服务器...
echo 提示：按Ctrl+C可以停止服务器
echo.
java -jar target\dns-relay.jar -d 8.8.8.8 config\dnsrelay.txt

echo.
echo 服务器已停止
pause
