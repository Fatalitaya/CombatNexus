# 开发流程与平台边界

遵循 [AGENTS.md](../AGENTS.md)；业务规则见 [领域合约](DOMAIN_CONTRACTS.md)。本文只保存持续适用的开发规则与可复用命令，具体变更的结果记录在评审说明及本地验证报告中。

## 支持与代码归属

开发重点为 NeoForge 1.21.1 的正式支持移植，26.1.2 保留为实现和回归基线。版本以各 target 的 Gradle 配置为准。只面向各版本的新世界，不承诺旧 Java API、旧数据或跨版本网络兼容。1.21.1 完成平台适配、领域语义验证及打包运行验证后，才能同步启用 CI descriptor、解除发布门禁并更新支持声明；README 变更须由用户主动要求。

| 模块 | Minecraft / NeoForge | Java | 用途 |
| --- | --- | --- | --- |
| `targets/neoforge-1.21.1` | 1.21.1 / 21.1.265 | 21 | 正式支持开发目标，尚未完成移植 |
| `targets/neoforge-26.1.2` | 26.1.2 / 26.1.2.126 | 25 | 现有实现及回归基线 |
| `common` | 不依赖 Minecraft 或 NeoForge | 17（toolchain 与 release） | 独立编译的共享源码，不发布 Jar |

按真实依赖链决定代码位置：独立值、公式、编解码预算、纯状态和窄合约可归 common；原生实体、世界、物品、注册、Mixin、网络、客户端接线及依赖它们的实现归 target。领域所有权不由目录决定，禁止为搬文件新增一套业务系统或改变行为。通用公式无实体访问、事件发布或随机数所有权。common 与 target 可共享包名，但不能出现重复类，common 也不能引用 target 的同包类。

平台绑定显式安装，原生 ID 转换只做 namespace/path 转换，世界注册查询使用对应世界。业务 ID 使用 `NexusId`。完整贡献身份必须包含来源路径与实例语义，不得仅按 namespace 生成属性标识或 UUID。保存、网络与资源格式变化单独审查，不能由包名未改推断兼容。

可选兼容实现隔离为 `combatnexus_compat`，通过核心公开 API 接入；可选类型不进入核心入口。compat 使用 compileOnly，主 Jar 内嵌独立 compat Jar，第三方本体不得打包。装载前检查可选模组存在，Mixin require 保持严格。依赖可解析和能力声明只证明装配条件，联合兼容必须验证实际调用与执行抑制。

## 1.21.1 开发指引

### 源码依据与接入起点

以 `targets/neoforge-1.21.1/gradle.properties` 固定的版本及其 NeoForge 补丁源码为平台事实依据。ModDevGradle 生成的源码可从 IDE 依赖查看；本地生成位置为 `targets/neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.265-sources.jar`，不提交该文件。环境要求见 [NeoForge 1.21.1 官方开发文档](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)。

`CombatNexusNeoForge121` 当前只有 `@Mod` 和模组 ID，没有业务初始化。`prepareCommonSources` 排除了 combat、attribute、formula、effect、presentation，资源还排除了 `assets/combatnexus/**`、`data/**` 和图标。正式接入必须取消这些骨架排除，完整编译 common 并纳入应发布资源；不能增加排除项或同名替身绕过错误。

共享基础已包括 `BusinessTimeState`、`AbilityExecutionState`、`AbilityExecutors`、`AbilityContinuationIntent`、`DriverRegistrations`、`DamageAttributeScaling`、`DamageDecisionCalculations`、`EffectTooltip`、`EquipmentDescriptions` 和 `EffectBehaviorDescriptors`。先复用这些实现；原生服务、世界接线及实际写入从 26.1.2 对应领域逐项适配，不重新执行已完成的 common 抽取，也不要求先抽象全部世界服务。

`docs/DamageNexus-main` 只读保留，不能替代当前领域合约或作为编译输入。`docs/version-differences` 是第三方版本差异参考，保留原作者及许可证；回移时按差异定位，再以 1.21.1 补丁源码确认，不把迁移文章当作本项目的支持声明。

### 平台适配落点

| 范围 | 1.21.1 源码依据与实施要求 |
| --- | --- |
| 标识与注册 | 原生标识为 `ResourceLocation`，使用 `fromNamespaceAndPath` / `parse`；业务仍使用 `NexusId`。按本版本注册表和事件接入属性、组件、效果、木偶与公开注册入口。 |
| 伤害与治疗 | 核对 `LivingEntity.hurt(DamageSource, float)`、`actuallyHurt(DamageSource, float)`、`heal(float)` 和 `Player.attack(Entity)`；重新确定 Mixin 描述符、调用点与玩家覆写路径，不能直接沿用 26.1.2 注入位置。 |
| 原生减伤与结算 | 核对 `LivingIncomingDamageEvent`、`DamageContainer`、`LivingDamageEvent.Pre/Post` 与 `CommonHooks`。`LivingEntity.actuallyHurt` 中 Pre 在护甲/魔法减伤后、吸收及生命写入前，Post 在实际写入后；事件金额不能直接当作已确认生命损失。取消、无敌帧、吸收、死亡和子请求均保留单一执行所有者。 |
| 保存与恢复 | 实体保存使用 `CompoundTag`，`SavedData.save` 接收 `CompoundTag` 和 `HolderLookup.Provider`。适配原生读写及注册上下文，复用共享 Saved 值与状态语义，保存后新进程恢复；不照搬新版本存储接口。 |
| 定义重载 | `SimpleJsonResourceReloadListener` 使用 Gson、目录及 `Map<ResourceLocation, JsonElement>`。接入共享候选编译和整批发布，保持失败时旧代际有效，避免创建第二套定义系统。 |
| 物品默认组件 | `Item.Properties.component`、`Item.components()` 和 `ModifyDefaultComponentsEvent` 可用；本版本事件使用 `modify(ItemLike, Consumer<DataComponentPatch.Builder>)` / `modifyMatching(Predicate<Item>, Consumer<...>)`，没有 26.1.2 `ItemBindingDefaults` 使用的候选组件 validator/context 接口。按本版本初始化顺序采集最终原型，重载时重新编译绑定；不能假设同名事件生命周期与参数相同。 |
| 网络与展示 | 按本版本 payload、StreamCodec 和客户端事件接线，复用共享展示值。提示隐藏规则和原生组件逐项核对，不照搬新版本 `TOOLTIP_DISPLAY` 路径。服务端保持权威，不新增 schema/protocol version 或跨版本握手校验。 |
| 渲染与木偶 | `LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>>` 直接接收实体渲染；适配模型、渲染器和客户端注册，不照搬新版本 render-state 接口。木偶代码及资源全部归 1.21.1 target。 |
| 平台事实与资源 | 核对属性 ID、效果数值、武器类别、标签、模型和数据目录。只引用本版本真实注册项；共享公式与说明通过明确输入接收版本事实，不在 common 中引入版本判断。 |

原生组件规则参见 [NeoForge 1.21.1 Data Components](https://docs.neoforged.net/docs/1.21.1/items/datacomponents/)。具体业务绑定遵循 [领域合约](DOMAIN_CONTRACTS.md)，不恢复旧实施计划中的绑定格式。

### 实施顺序与完成条件

1. **完整构建接线**：取消骨架源码/资源排除，接入完整 common；核对 Java 21 语言/API、编译依赖和运行依赖。common 使用 Java 17 不代表其 DFU、Gson、fastutil API 自动适用于 1.21.1；以目标解析的依赖及实际调用为准，不能强行替换 Minecraft 自带库来掩盖差异。
2. **平台基础**：接入注册、ID、配置、资源重载、服务器/世界/实体生命周期与持久化，确保专用服务器不加载客户端类；建立可运行的 AutoJS 环境。
3. **领域闭环**：接入伤害、属性、治疗、护盾、恢复、Effect、来源安装、物品绑定、能力和业务时间。保持领域合约及随机消费顺序，覆盖来源撤销、重载与读档。
4. **展示与兼容**：完成权威快照同步、面板、提示和木偶；可选模组组合独立验证。开发依赖已安装不代表兼容实现已移植。
5. **发布条件**：完整构建与制品检查、专用服务端/客户端及最终 Jar 运行验证通过后，才能启用 `ci.properties`、解除 `publish*` 阻断并更新支持范围。当前仓库没有 `.github/workflows`，descriptor 不能代替实际 CI 工作流，启用时核对实际消费入口。元数据只声明经过验证的 Minecraft 与 NeoForge 范围。

### 1.21.1 验证入口

在 `targets/neoforge-1.21.1` 目录执行：

```powershell
.\gradlew.bat clean build
.\gradlew.bat dependencies --configuration compileClasspath
.\gradlew.bat dependencies --configuration runtimeClasspath
```

在该目录使用 `runServer` / `runClient` 启动开发实例。构建命令目前只能检查骨架，取消排除并完成接线后才具有完整实现的验证意义。改动 common 时，同时构建独立 common 与 26.1.2，防止共享改动破坏现有实现。

`verify-baseline.py`、`verify-common-boundary.py`、`verify-capabilities-runtime.py` 和客户端运行器仍包含 26.1.2 路径或平台假设。接入 1.21.1 时先让运行器明确选择 target、Java、端口和制品，调整原生探针及 init 脚本，再复用业务断言；当前没有可直接套用的 1.21.1 `--target` 命令。检查各版本制品中的完整共享类、资源、重复类和元数据，不混用输出或运行证据。

行为覆盖使用现有 `identifier`、`platform-lifecycle`、`native`、`vanilla-integration`、`effect-foundation`、`item-bindings`、`abilities`、`business-time` 及客户端展示套件，逐项适配本版本 API；持久化必须重启，定义必须重载。覆盖正常、边界与失败路径，保持 Mixin require，不恢复 JUnit/GameTest。

## 26.1.2 基线构建与静态验证

在 `targets/neoforge-26.1.2` 目录运行：

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

以下命令当前运行 26.1.2，按改动选择实际套件，从仓库根执行；1.21.1 使用前先完成上文所述运行器适配：

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

以下为 26.1.2 及 common 的固定依赖；1.21.1 使用后表，不能混用两版本的第三方模组制品。调整依赖时同步更新对应表及构建配置。

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

1.21.1 构建固定依赖如下；文件 ID 表示当前配置，不代表已经通过联合运行验证：

| 依赖 | 固定版本 / 文件 ID | 官方来源 |
| --- | --- | --- |
| NeoForge | 21.1.265 | https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.265/ |
| ModDevGradle / mod-publish-plugin | 2.0.141 / 2.1.1 | 同上表插件来源 |
| AutoJS | AutoJS-neoforge-1.21.1:1.0.1 | https://maven.sighs.cc/repository/maven-public/com/sighs/autojs/ |
| Rhino | 8463898 | https://www.curseforge.com/minecraft/mc-mods/rhino/files/8463898 |
| spark | 1.10.124 / 6225208 | https://www.curseforge.com/minecraft/mc-mods/spark/files/6225208 |
| JEI | 19.51.0.418 / 8792638 | https://www.curseforge.com/minecraft/mc-mods/jei/files/8792638 |
| 通用拼音搜索 | 4.5.24 / 8117474 | https://www.curseforge.com/minecraft/mc-mods/just-enough-characters/files/8117474 |
| Apotheosis | 8.9.0 / 8993983 | https://www.curseforge.com/minecraft/mc-mods/apotheosis/files/8993983 |
| Placebo | 9.9.4 / 9153039 | https://www.curseforge.com/minecraft/mc-mods/placebo/files/9153039 |
| Apothic Attributes | 2.11.0 / 9006294 | https://www.curseforge.com/minecraft/mc-mods/apothic-attributes/files/9006294 |
| Cataclysm | 3.33 / 8931182 | https://www.curseforge.com/minecraft/mc-mods/lendercataclysm/files/8931182 |
| Curios | 9.5.1+1.21.1 / 6529130 | https://www.curseforge.com/minecraft/mc-mods/curios/files/6529130 |
| Lionfish API | 3.1 / 8345326 | https://www.curseforge.com/minecraft/mc-mods/lionfish-api/files/8345326 |
| KubeJS | 9396059 | https://www.curseforge.com/minecraft/mc-mods/kubejs/files/9396059 |
| Jade | 8591319 | https://www.curseforge.com/minecraft/mc-mods/jade/files/8591319 |

AutoJS、Rhino 与观察工具用于开发运行。26.1.2 的可选组合由 `verificationCompatibility` / `verificationAttributesOnly` 显式开启。1.21.1 骨架当前将上述工具和兼容模组直接加入运行依赖（Jade 为 implementation）；正式适配时先建立最小运行组合，再显式启用可选组合。不能把默认混装启动成功作为所有兼容组合通过的依据。

每个发布制品只对应一个 loader 与精确 Minecraft 版本，不使用 universal Jar，common 不单独发布。发布前核对正式版本、发布类型、项目归属、依赖范围、许可证和最终 Jar 内容，并完成相应打包运行验证。未移植 target 不得解除发布门禁。

Maven 使用 `COMBATNEXUS_MAVEN_URL`、`COMBATNEXUS_MAVEN_USER`、`COMBATNEXUS_MAVEN_PASSWORD`；未配置 URL 时不创建远程发布仓库。CurseForge/Modrinth 项目通过 Gradle 属性或 `CURSEFORGE_PROJECT_ID` / `MODRINTH_PROJECT_ID` 指定，令牌仅用环境变量 `CURSEFORGE_TOKEN` / `MODRINTH_TOKEN`，说明可用 `PUBLISH_CHANGELOG`。发布任务从对应 target 执行，凭据不得入库。
