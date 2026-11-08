# Quick Search Tevriq — 正式版自动发布

当前定制包名：`org.tevriq.quicksearch`（与原作者 `com.tk.quicksearch` 独立安装）。
源码基于上游 Quick Search v5.0，**正式安装包只发布固定签名的 F-Droid flavor APK**。

## 自动链路

- 每次 `main` 有新提交，或手动运行 `Tevriq Quick Search Release`，均启动 GitHub Actions。
- Actions 完成源代码检查与单元测试（可能内部编译 Debug 类，但**不发布 Debug APK**）。
- 读取四项固定签名 Secrets，构建正式版，校验 APK 的包名、版本号、签名证书。
- 自动向 GitHub Releases 发布**单独的 .apk 文件**和 SHA256 校验值（不会只上传到临时 Actions Artifacts）。
- P20 的 `quicksearch-tevriq-release-apk` Watcher 每 15 分钟检查 Releases，新 .apk 经 P20/QQ Flash 推送，48 小时有效；在手机上点下载并手动安装升级。

## 自动版本

基于上游 v5.0；每次工作流的 `github.run_number` 生成一个唯一递增的定制版本：
`versionName=5.0-tevriq.<run_number>`，`versionCode=100000+<run_number>`，
Release 标签 `tevriq-v5.0-r<run_number>`。
保留默认本地开发版本 `5.0/104`，但正式 CI APK 始终大于上一份 104 APK，
确保同一包名、同一签名可以覆盖升级。

**关键约束**：未来同步上游到 5.1 或更高主版本时，需要同时更改工作流中的
`5.0` 基准；不得降低 versionCode 或更换签名证书。不要删除并重建此
GitHub workflow 导致 run_number 重置。

## 固定签名 Secrets

在仓库 Settings > Secrets and variables > Actions 配置：

- `TEVRIQ_RELEASE_KEYSTORE_BASE64`
- `TEVRIQ_RELEASE_STORE_PASSWORD`
- `TEVRIQ_RELEASE_KEY_ALIAS`
- `TEVRIQ_RELEASE_KEY_PASSWORD`

必须妥善保存签名包。**严禁把 keystore、Base64、密码提交到公开仓库**。
运行中缺少 Secrets 或证书指纹不符，将拒绝发布。

## 发布与部署范围

- GitHub Actions 使用公开 GitHub-hosted runner，不使用 Mac Mini/VPS 编译。
- 仅 `main` 代码更改触发正式发布；外部上游更新不会自动同步，需按需合并。
- Release APK 发到公开 GitHub Releases，P20 通过独立 Watcher 拉取；不向手机静默安装 APK。
- 失败的测试/构建/校验不发布 APK，P20 只会收到已成功发布的版本。
- P20 仍以 48 小时清理政策处理文件；Watcher 不负责同步上游源码。
- 在 vivo X200 Ultra 上安装以后，还要人工验证 OriginOS 的 Widget、挖孔手势、搜索权限等实际体验。
