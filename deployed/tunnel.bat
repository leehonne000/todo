@echo off
chcp 65001 >nul
title Big Tree 隧道（此窗口保持打开，关掉即断开）
echo ============================================
echo   Big Tree 本地查看隧道（双击即用）
echo ============================================
echo.
echo   建立中... 看到无报错即为成功，此窗口别关！
echo.
echo   建好后在本地这样连：
echo     数据库 Navicat 等  -^>  127.0.0.1:13306
echo     MinIO 网页控制台   -^>  http://localhost:9001
echo     S3 客户端工具      -^>  http://localhost:9000
echo.
ssh -i C:\Users\ink\.ssh\id_ed25519_bigtree -N -o StrictHostKeyChecking=accept-new -L 13306:127.0.0.1:3306 -L 9001:127.0.0.1:9001 -L 9000:127.0.0.1:9000 ubuntu@124.220.182.134
echo.
echo 隧道已断开。
pause
