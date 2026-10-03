# 音量键双击 (Volume Keys)

通过无障碍服务拦截音量键，实现双击触发自定义动作。单击正常调节音量，双击触发你设定的操作。

## 功能

- **双击音量键触发动作** — 音量上 / 音量下分别独立设置
- **三种动作类型**：
  - 打开应用（从已安装应用列表中选择）
  - 执行 Shell 指令（如 `input keyevent 26` 锁屏 / 息屏）
  - 切换铃声模式（静音 / 震动 / 正常）
- **可调双击判定时间** — 100~2000ms，默认 400ms
- **拦截反馈日志** — 实时显示按键拦截事件，确认模块生效
- **后台保活引导** — 自启动权限、电池优化忽略、后台锁定一站式设置
- **Shizuku 支持** — Shell 指令可通过 Root 或 Shizuku 执行，在设置中切换

## 下载

从 [Releases](../../releases) 页面获取最新 APK。

## 使用方法

### 1. 开启无障碍服务

安装后打开应用，点击「去开启无障碍服务」，在系统设置中找到「音量键双击」并开启。

### 2. 设置双击动作

选择音量上 / 音量下的动作类型：

- **打开应用** — 点击「选择应用」从列表中选取
- **Shell 指令** — 输入要执行的命令，如 `input keyevent 26`（锁屏）
- **铃声模式** — 选择要切换的模式

### 3. 配置 Shell 执行权限（可选）

如果使用 Shell 指令动作，进入 设置 → Shell 执行权限：

- **Root** — 需设备已 root，通过 `su` 执行
- **Shizuku** — 需安装并激活 [Shizuku](https://github.com/RikkaApps/Shizuku)，通过 Shizuku 服务执行

### 4. 后台保活（推荐）

进入「后台保活」页面，依次完成：
- 自启动权限
- 忽略电池优化
- 锁定后台（在最近任务中锁定应用）

## 技术细节

| 项目 | 规格 |
|---|---|
| 包名 | `com.volumekeys.tap` |
| 最低系统 | Android 8.0 (API 26) |
| 目标系统 | Android 14 (API 34) |
| 语言 | Kotlin |
| 依赖 | Shizuku API v13.1.5 |

## 构建

```bash
git clone https://github.com/xuanwei130-netizen/Volume-keys.git
cd Volume-keys
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 工作原理

应用通过 Android 无障碍服务（AccessibilityService）拦截音量键事件。当检测到短时间内连续两次按下同一音量键时，判定为双击并触发预设动作；单次点击则放行，保留系统原有的音量调节功能。

Shell 指令通过 `ShellExecutor` 执行，根据设置选择 Root（`su -c`）或 Shizuku（`IShizukuService.newProcess`）。

## 许可

本项目仅供学习交流使用。
