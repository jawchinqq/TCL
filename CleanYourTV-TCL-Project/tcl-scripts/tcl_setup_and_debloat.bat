@echo off
chcp 65001 >nul
echo ========================================================
echo   TCL Google TV / Android TV - CleanYourTV 自動安裝與淨化腳本
echo ========================================================
echo.
set /p TV_IP="請輸入您的 TCL 電視 IP 位址 (例如 192.168.1.100): "

echo.
echo [1/4] 正在透過 ADB 連線至 TCL 電視 (%TV_IP%:5555)...
adb connect %TV_IP%:5555
echo 請看電視畫面，若跳出「允許 USB 偵錯嗎？」請勾選一律允許並按確定！
pause

echo.
echo [2/4] 正在備份目前電視的套件清單至 backup_packages.txt ...
adb -s %TV_IP%:5555 shell pm list packages > backup_packages.txt
echo 備份完成！

echo.
echo [3/4] 正在安裝 CleanYourTV Apple TV 風格極簡桌面 (app-debug.apk)...
if exist app-debug.apk (
    adb -s %TV_IP%:5555 push app-debug.apk /data/local/tmp/cleanyourtv.apk
    adb -s %TV_IP%:5555 shell pm install -r /data/local/tmp/cleanyourtv.apk
    adb -s %TV_IP%:5555 shell cmd package set-home-activity com.cleanyourtv.launcher/.MainActivity
    echo ✅ CleanYourTV 桌面安裝並設為預設首頁完成！
) else (
    echo ⚠️ 找不到 app-debug.apk，請將從 GitHub 下載解壓縮後的 app-debug.apk 放在與此腳本同一個資料夾！
)

echo.
echo [4/4] 是否要順便停用 TCL 內建廣告、遙測與推薦列 (Debloat)？
echo (注意：完全保留遙控器按鍵、HDMI訊號源、藍牙、語音與 Google Play)
set /p DO_DEBLOAT="輸入 Y 開始淨化，輸入 N 跳過: "
if /i "%DO_DEBLOAT%"=="Y" (
    echo 正在停用 TCL 遙測與背景廣告服務...
    adb -s %TV_IP%:5555 shell pm disable-user --user 0 com.tcl.cyberui
    adb -s %TV_IP%:5555 shell pm disable-user --user 0 com.tcl.partnercustomizer
    adb -s %TV_IP%:5555 shell pm disable-user --user 0 com.tcl.usercenter
    adb -s %TV_IP%:5555 shell pm disable-user --user 0 com.tcl.messagebox
    adb -s %TV_IP%:5555 shell pm disable-user --user 0 com.tcl.bootadservice
    adb -s %TV_IP%:5555 shell pm disable-user --user 0 com.tcl.bi
    adb -s %TV_IP%:5555 shell pm disable-user --user 0 com.google.android.tvrecommendations
    echo ✅ TCL 廣告與背景推播服務已停用！
)

echo.
echo [選填] 是否要開啟 AdGuard DNS 阻擋電視廣告？
set /p DO_DNS="輸入 Y 開啟，輸入 N 跳過: "
if /i "%DO_DNS%"=="Y" (
    adb -s %TV_IP%:5555 shell settings put global private_dns_specifier dns.adguard-dns.com
    adb -s %TV_IP%:5555 shell settings put global private_dns_mode hostname
    echo ✅ 已設定 AdGuard 私人 DNS！
)

echo.
echo 🎉 全部完成！現在可以按下電視遙控器的「首頁 (Home)」鍵體驗新桌面了！
pause
