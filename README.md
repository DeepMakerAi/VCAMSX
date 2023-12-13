# 安卓虚拟摄像头
- 基于Xposed的虚拟摄像头
# 请勿用于任何非法用途，所有后果自负！！
## 使用演示
- https://fastly.jsdelivr.net/gh/iiheng/TuChuang@main/1700961311425EasyGIF-1700961287297.gif
## 开发计划
- [ ] 支持rtmp传输直播
- [ ] 支持视频提前选择，自定义播放顺序
## 开发环境
- Android SDK 34
- Xposed 82
- xiaomi 9 MIUI 11.0.3
- xiaomi 8 MIUI 11.0.3
- 酷比魔方50pro MIUI 14.0.5
- Lsposed lastest
## 使用方法
1. 在Lsposed中勾选自己想要的播放平台
2. 在软件中选择自己想要播放的视频
3. 打开视频开关
4. 然后选择平台播放
## 注意事项
1. 视频播放需要与平台播放的格式相同，基本支持9:16的视频,例如：3840x2160,1920x1080,1280x720,854x480,640x360,426x240,256x144
2. 画面黑屏，相机启动失败，因为视频解码有问题，请多次点击翻转摄像头
3. 画面翻转，和原视频不匹配，当前视频播放还未做调整，请手动调整视频
4. 不同软件对于硬解码和软解码的要求不同，如果多次只出声音不出画面，请切换视频解码方式
5. 硬解码流畅于软解码,请根据你的手机型号来判断是否支持硬解码，软解码的适配性较高，视频基本都支持播放

## 反馈问题
- QQ群：[点击加入](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=BrFy3-Jidig08nnnSL1TkJ1-mq1pVTyl&authKey=l0r7NULDSHhc8hM4z9SrL7RDMTU%2BUerKNN8VsgUy4W7Fh0kq2sF5RcX7We610qRd&noverify=0&group_code=711762040)
- 在issues中反馈，如果为BUG反馈，请附带Xposed模块日志信息

## 致谢
- 提供hook代码：https://github.com/Xposed-Modules-Repo/com.example.vcam
