@echo off
chcp 65001 >nul
echo ========================================================
echo   TCL 電視一鍵還原原廠狀態腳本 (Restore)
echo ========================================================
set /p TV_IP="請輸入您的 TCL 電視 IP 位址: "

adb connect %TV_IP%:5555

echo 正在重新啟用所有 TCL 與 Google TV 原廠服務...
adb -s %TV_IP%:5555 shell pm enable com.tcl.cyberui
adb -s %TV_IP%:5555 shell pm enable com.tcl.partnercustomizer
adb -s %TV_IP%:5555 shell pm enable com.tcl.usercenter
adb -s %TV_IP%:5555 shell pm enable com.tcl.messagebox
adb -s %TV_IP%:5555 shell pm enable com.tcl.bootadservice
adb -s %TV_IP%:5555 shell pm enable com.tcl.bi
adb -s %TV_IP%:5555 shell pm enable com.google.android.tvrecommendations
adb -s %TV_IP%:5555 shell pm enable com.google.android.apps.tv.launcherx
adb -s %TV_IP%:5555 shell pm enable com.google.android.tvlauncher
adb -s %TV_IP%:5555 shell settings put global private_dns_mode opportunistic

echo 正在移除自訂 Launcher 並恢復原廠首頁...
adb -s %TV_IP%:5555 uninstall com.cleanyourtv.launcher

echo ✅ 已完全恢復原廠狀態！
pause
