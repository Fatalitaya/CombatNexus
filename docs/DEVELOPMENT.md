# 开发流程与平台边界

遵循 [AGENTS.md](../AGENTS.md)；业务规则见 [领域合约](DOMAIN_CONTRACTS.md)。本文只保存持续适用的开发规则与可复用命令，具体变更的结果记录在评审说明及本地验证报告中。

## 支持与代码归属

活动基线为 Minecraft 26.1.2、NeoForge 26.1.2.126、Java 25。版本以 Gradle 配置为准。只支持当前新世界，不承诺旧 Java API、旧数据或跨版本协议兼容。启用其他 target 前必须完成版本适配、相同语义验证与实际运行验收，并同步 CI descriptor、发布门禁和支持声明；README 变更须由用户主动要求。

按真实依赖链决定代码位置：独立值、公式、编解码预算、纯状态和窄合约可归 common；原生实体、世界、物品、注册、Mixin、网络、客户端接线及依赖它们的实现归 target。领域所有权不由目录决定，禁止为搬文件新增一套业务系统或改变行为。通用公式无实体访问、事件发布或随机数所有权。common 与 target 可共享包名，但不能出现重复类，common 也不能引用 target 的同包类。

平台绑定显式安装，原生 ID 转换只做 namespace/path 转换，世界注册查询使用对应世界。业务 ID 使用 `NexusId`。完整贡献身份必须包含来源路径与实例语义，不得仅按 namespace 生成属性标识或 UUID。保存、网络与资源格式变化单独审查，不能由包名未改推断兼容。

可选兼容实现隔离为 `combatnexus_compat`，通过核心公开 API 接入；可选类型不进入核心入口。compat 使用 compileOnly，主 Jar 内嵌独立 compat Jar，第三方本体不得打包。装载前检查可选模组存在，Mixin require 保持严格。依赖可解析和能力声明只证明装配条件，联合兼容必须验证实际调用与执行抑制。

## 构建与静态验证

在活动 target 目录运行：

```powershell
.\gradlew.bat clean build
```

回仓库根目录，独立编译 common 后检查制品与边界：

```powershell
.\gradlew.bat -p common clean build
.\gradlew.bat -p common dependencies --configuration compileClasspath
python scripts/verify-baseline.py
python scripts/verify-common-boundary.py
python scripts/verify-capabilities-runtime.py --syntax-only
git diff --check
```

边界检查须覆盖 common 源码及编译类的原生/加载器引用、独立依赖类路径、source-set 隔离、重复类、common→target 引用及两棵源码树的公开 API。不得通过豁免或删除失败断言掩盖真实越界；实现变化后更新失效的文本定位，保留对应语义检查。`test NO-SOURCE` 只代表没有 JUnit/GameTest。

## 运行验证与证据

按改动选择实际套件，运行命令从仓库根执行：

| 范围 | 命令 |
| --- | --- |
| ID、值与编解码 | `python scripts/verify-capabilities-runtime.py --suite identifier --start` |
| 世界、会话与权限 | `python scripts/verify-capabilities-runtime.py --suite platform-lifecycle --start` |
| Effect、定义及持久化 | `python scripts/verify-capabilities-runtime.py --suite effect-foundation --start --restart --reload` |
| 原版事件及 Mixin | `python scripts/verify-capabilities-runtime.py --suite vanilla-integration --start` |
| 护盾与恢复状态 | `python scripts/verify-capabilities-runtime.py --suite target-thinning --start` |
| 客户端展示与同步 | `python -X utf8 scripts/verify-carried-effect-tooltips.py` |
| 可选兼容组合 | `python scripts/verify-capabilities-runtime.py --suite compat-scenes --start --compatibility` |
| 运行器故障与报告 | `python scripts/verify-verification-runners.py` |

服务端可显式指定空闲的 `--port 25595 --game-port 25570`。隔离运行器负责临时世界、配置、AutoJS 操作与实例清理；启动前检查端口，执行及停止均核对本次实例标识，重启生成新标识。不得把已有客户端或服务端当作临时实例停止。

新增行为覆盖正常、边界、拒绝、异常及生命周期；持久化验证必须保存后启动新进程，精确检查剩余寿命、周期相位、已消费次数和恢复分数，不能让清零或重新发放也通过。拒绝断言检查业务异常类型和原因，Rhino 解析或重载调用失败不能算业务拒绝。临时扩展只使用公开 API，不进入发布包；冻结时钟等验证设施不得改变产品行为。

报告应绑定运行身份、套件和 running/failed/passed 状态，未捕获异常及后续子检查失败必须使整体失败。实际断言计数与子检查范围分开，不按 tick 人为累计。运行器模拟回归不替代游戏行为，自动化客户端断言不替代人工视觉，开发环境通过不替代最终打包制品通过。

评审说明记录工作树或提交标识、脚本与命令、Java/加载器/模组版本、数据及兼容组合、预期与实际结果、失败修复后重跑范围和未覆盖项。日志及报告置于根或 target 的 `build/verification/` 等本地输出目录，不入库；clean 前按需保留证据。历史断言数不沿用为当前通过结果，性能声明须有预热、负载、样本量及测量范围。

## 依赖与发布

固定版本与官方来源如下；调整依赖时同步更新本表及构建配置。

| 依赖 | 固定版本 / 文件 | 官方来源 |
|---|---|---|
| NeoForge | 26.1.2.126 | https://maven.neoforged.net/releases/net/neoforged/neoforge/ |
| common DFU | 9.0.19 | https://libraries.minecraft.net/com/mojang/datafixerupper/ |
| common Gson | 2.13.2 | https://repo.maven.apache.org/maven2/com/google/code/gson/gson/ |
| common fastutil | 8.5.18 | https://repo.maven.apache.org/maven2/it/unimi/dsi/fastutil/ |
| common JSpecify | 1.0.0 | https://repo.maven.apache.org/maven2/org/jspecify/jspecify/ |
| common JetBrains annotations | 26.0.2-1 | https://repo.maven.apache.org/maven2/org/jetbrains/annotations/ |
| ModDevGradle | 2.0.141 | https://github.com/neoforged/ModDevGradle |
| mod-publish-plugin | 2.1.1 | https://github.com/modmuss50/mod-publish-plugin |
| AutoJS | 1.0.1 | https://maven.sighs.cc/repository/maven-public/com/sighs/autojs/ |
| Rhino | 2101.2.8-build.91 / 8463898 | https://www.curseforge.com/minecraft/mc-mods/rhino/files/8463898 |
| spark | 1.10.187 / 8918574 | https://www.curseforge.com/minecraft/mc-mods/spark/files/8918574 |
| JEI | 29.53.0.115 / 9384706 | https://www.curseforge.com/minecraft/mc-mods/jei/files/9384706 |
| 通用拼音搜索 | 4.6.9 / 9163773 | https://www.curseforge.com/minecraft/mc-mods/just-enough-characters/files/9163773 |
| Jade | 9156414 | https://www.curseforge.com/minecraft/mc-mods/jade/files/9156414 |
| 可选 Apotheosis | 9.1.0 / 9054740 | https://www.curseforge.com/minecraft/mc-mods/apotheosis/files/9054740 |
| 可选 Placebo | 10.0.2 / 8502256 | https://www.curseforge.com/minecraft/mc-mods/placebo/files/8502256 |
| 可选 Apothic Attributes | 3.1.0 / 9076588 | https://www.curseforge.com/minecraft/mc-mods/apothic-attributes/files/9076588 |

AutoJS、Rhino 与观察工具用于开发运行；可选组合由 `verificationCompatibility` / `verificationAttributesOnly` 显式开启，不自动外推其他组合支持。

每个发布制品只对应一个 loader 与精确 Minecraft 版本，不使用 universal Jar，common 不单独发布。发布前核对正式版本、发布类型、项目归属、依赖范围、许可证和最终 Jar 内容，并完成相应打包运行验证。未移植 target 不得解除发布门禁。

Maven 使用 `COMBATNEXUS_MAVEN_URL`、`COMBATNEXUS_MAVEN_USER`、`COMBATNEXUS_MAVEN_PASSWORD`；未配置 URL 时不创建远程发布仓库。CurseForge/Modrinth 项目通过 Gradle 属性或 `CURSEFORGE_PROJECT_ID` / `MODRINTH_PROJECT_ID` 指定，令牌仅用环境变量 `CURSEFORGE_TOKEN` / `MODRINTH_TOKEN`，说明可用 `PUBLISH_CHANGELOG`。发布任务从对应 target 执行，凭据不得入库。
