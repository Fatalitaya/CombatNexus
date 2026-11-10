# 为 NeoForge 1.21.1 准备的 common 重构计划

目标是在保持 26.1.2 行为不变的前提下，减少 1.21.1 target 必须重复实现的业务状态与计算逻辑。优先迁移独立类、抽取业务时间和能力状态，再处理伤害决策与展示依赖；不以 target 文件数最少为目标，也不要求先把全部运行时抽象完成。

本计划描述待实施工作，不改变当前支持范围或发布状态。开发约束仍以 [AGENTS.md](../AGENTS.md)、[开发流程与平台边界](DEVELOPMENT.md)、[领域合约](DOMAIN_CONTRACTS.md) 为准。源码依据包括当前工作树中的能力、驱动、定义绑定及持久化实现；实施时应保留这些已有行为。

## 已有共享基础

下列逻辑已经在 common，1.21.1 应复用，不另建一套实现：

| 领域 | 已有实现或边界 |
| --- | --- |
| 计算 | `formula`；`HealingPipeline`；伤害组件、通道、规则及计算状态 |
| 伤害状态 | `DamagePacketState`、`DamageCombatState`、`DamagePhaseState`、`CriticalDecisionState` |
| 执行机制 | `CombatAdmissionBudget`、`CombatCompletionQueue`、执行/通知作用域和权限合约 |
| Effect | `DefaultEffectService`、定义、来源计划与生命周期值 |
| 护盾与恢复 | `ShieldPoolState`、`RegenerationScheduleState`、`DefaultRegenerationScheduler` |
| 状态与持久化 | `RuntimeStateStore`、`CombatPersistentState`、`CombatStateCodec`、各领域 Saved 值 |
| 定义与展示 | 定义编译及代际值、属性快照、描述文档和展示值 |

是否可以迁移必须检查完整依赖链，包括全限定类名、同包类型、泛型、回调参数及静态初始化。没有 Minecraft import 不等于平台无关。例如 `CombatPersistence` 通过世界服务实现依赖 target；`CombatWorld` 通过原生伤害请求和驱动接口依赖 target。

## 第一批 独立类与资源归属

以下三个类的现有类型依赖均可在 common 中满足，适合先原包名迁移并删除 target 中的原文件：

| 类 | 保持的语义 | 平台仍承担的职责 |
| --- | --- | --- |
| [AbilityExecutors](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/ability/internal/AbilityExecutors.java) | 执行器与目录共同注册、重复拒绝、容量上限、冻结 | 在加载器启动阶段收集并冻结注册 |
| [AbilityContinuationIntent](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/ability/internal/AbilityContinuationIntent.java) | execution/step 编解码及步骤范围 | 实体调度存储、到期执行与权限检查 |
| [DriverRegistrations](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/driver/internal/DriverRegistrations.java) | 注册容量、冻结、凭据对象身份检查 | 驱动会话与世界生命周期 |

迁移不新增公共扩展入口，不改变静态注册生命周期，也不把凭据校验改为仅比较 ID。保持 source-set sharing，不生成独立发布 common Jar。

资源修正与类迁移分开审查：

- [木偶挖掘标签](../common/src/main/resources/data/minecraft/tags/block/mineable/axe.json) 只包含 `combatnexus:damage_dummy`，应移入活动 target，遵守木偶专用资源归 target 的约定。
- [矛标签](../common/src/main/resources/data/combatnexus/tags/item/spears.json) 包含可选的 `minecraft:spear` 和 `minecraft:trident`。启用 1.21.1 前核对真实物品与规则引用；`required: false` 只允许条目缺失，不代表玩法覆盖成立。不因标签名称直接认定整份文件必须迁移。
- target 现有 Mixin 配置、模组元数据、木偶模型、纹理和掉落表继续留在 target。

## 第二批 业务时间状态

入口为 [WorldBusinessTime](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/time/internal/WorldBusinessTime.java)。先于能力状态实施，便于后续统一使用现有 `BusinessTime` 语义。

拟在 `combat.time.internal` 抽取内部状态类，承担：

- `Lane` 的值、锚点、驱动归属和绑定状态，以及实体/来源时钟的计算。
- `EntityTime` 的参与者身份值、相位偏移、来源时钟和推进记录。
- 推进 sequence/request 的一致性判断、已消费数量、`slicePending` 与取消状态。
- 状态到 `SavedTime` 的投影及恢复所需的值转换。

target 的 `WorldBusinessTime` 继续检查世界有效性和当前实体实例，采集原生 `tickCount`，解析来源时钟身份，执行事务准入并调用 `world.advanceBusinessTime()`。通过参数传入当前父时钟、身份和业务结果，不向 common 传入 `CombatRuntime` 或提供完整世界代理。

一次时间片的顺序保持为：取得准入、必要时推进时钟并标记未完成、执行业务推进、确认完成后增加 consumed。预算耗尽后的续跑必须复用未完成时间片，不能重复增加时钟或重新执行已消费业务。原生预算时钟与可推进业务时钟继续分离。

退出条件：正常推进、相同请求续跑、不同参数复用身份拒绝、预算中断、来源撤销、实体替换、驱动释放和进程重启恢复均保持原语义。查询仍不得创建状态或推进时间。

## 第三批 能力执行状态

入口为 [DefaultAbilityService](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/ability/internal/DefaultAbilityService.java)。拟在 `combat.ability.internal` 抽取内部状态类，复用现有 Ability API 和 Saved 值，不整体迁移服务。

| common 状态职责 | target 服务职责 |
| --- | --- |
| 授权记录索引、贡献身份、冷却键及截止时间 | 确认来源有效、定义绑定正确、实体实例和世界仍有效 |
| 执行步骤、WAITING/RUNNING/终态、下一次到期时间 | 计算资格、执行回调、限制权限并提交领域命令 |
| 参数状态、命令证据和不可变快照 | 捕获权威输入、记录实际写入结果 |
| 按显式时间生成保存值及恢复状态 | 安装/移除 `NativeSchedules` 任务、协调随机样本和生命周期 |

状态类接收经服务检查的事实和应用结果，不自行恢复权限。服务仍是唯一调度安装者和能力执行入口。`AbilityContinuationIntent` 仅标识待续步骤，不能作为调用授权。

保持以下行为：

- 同一步骤只消费一次，未到期、外部驱动拥有执行权或授权失效时拒绝。
- 取消、来源撤销、死亡和换维度同步清理续步任务；完成后释放样本所有权。
- 子命令已写入后失败，保留 journal 和 PARTIAL/UNKNOWN 等原有结果，不重放、不伪装回滚。
- 保存 WAITING 执行时保留剩余等待、参数、步骤、命令证据和随机样本；冷却按所属业务时钟保存剩余量。
- 恢复时仍检查来源、贡献和定义匹配，并由 target 重新绑定当前实体身份，不直接恢复旧活实体或旧权限。

退出条件：能力正常执行、边界步骤、重复续步、取消、部分写入、外部驱动、保存后新进程恢复均通过对应行为检查。

## 第四批 伤害纯决策

按独立小步推进，优先从 [DamageAttributeScalingEngine](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/damage/core/pipeline/DamageAttributeScalingEngine.java) 开始，再评估 [CriticalDecisionEngine](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/damage/core/pipeline/CriticalDecisionEngine.java) 与 [DodgeDecisionEngine](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/damage/core/pipeline/DodgeDecisionEngine.java)。

common 接收冻结属性、攻击类别、原生暴击事实及明确的采样事实，产生有序贡献或决策。target 继续负责采集、阶段权限、一次性执行标记、随机数来源、上下文写入与取消后的子请求抑制。优先复用已有伤害事实和工作区，不新增一套平行伤害管线。

必须保持组件遍历、浮点归约、原生伤害桶及来源记录的顺序。随机消费按现有分支保留：当前暴击机会大于零时进入采样，包括限幅为 1 的情况；闪避机会大于等于 1 时不采样。不得为了共用一个概率工具改变消费次数。无资格或强制/抑制分支也不得提前抽样。

[Lifesteal](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/combat/healing/api/Lifesteal.java) 可随后抽取基于证据的资格与金额计算；当前结算作用域、攻击者存活及同世界检查、请求创建和子治疗提交继续留 target。金额仍来自已确认生命损失，并保留 appliedDamage 与受伤前生命上限，不计护盾和原版吸收。

这一批不要求迁移 `DamageModuleContext`、`DamageRequest` 或 `DamageSettlementSnapshot`。如局部抽取必须改变公开签名，先明确新签名和受影响调用方，将其作为显式 API 变更处理，不添加旧接口兼容层。

## 第五批 展示依赖解耦

候选为 `EffectTooltip`、`EquipmentDescriptions` 和 `EffectBehaviorDescriptors`。当前阻塞链是：

1. `EquipmentDescriptions` 调用 `EffectTooltip`。
2. `EffectTooltip` 使用行为描述器；[EffectBehaviorDescriptors](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/effect/internal/EffectBehaviorDescriptors.java) 静态注册内置描述器。
3. 内置描述器引用 `BuiltinBehaviors`、周期执行类型和 `EffectCapabilities`；后者保存的 [EffectCapability](../targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus/effect/api/EffectCapability.java) 执行上下文包含原生实体与伤害来源。

实施方向是将描述器容器与平台内置描述器安装分离：common 负责已有纯值的说明组合和描述器合约；target 在明确的启动阶段安装依赖原生行为类型的描述器，并提供能力说明。保留类型匹配、重复注册、冻结、未知行为回退和必需描述缺失时的拒绝语义。

不要仅为移动说明类拆散全部行为 API。若某段描述仍需原生行为类型，就保留为 target 注册项。渲染、ItemStack 观察、网络编码和客户端接线继续属于 target。

common 中 [VanillaCombatEffectDescriptions](../common/src/main/java/io/github/fatalitaya/combatnexus/presentation/platform/VanillaCombatEffectDescriptions.java) 硬编码了原版效果数值；启用 1.21.1 前核对这些数值和标识。确认有版本差异的事实由对应 target 提供，共享层只组合展示，不使用运行时版本分支。

## 暂不列为启动前置条件的抽取

`SourceInstallations` 的安装记录、`WorldDrivers` 的所有权冲突选择具有潜在共享价值，但当前同时承担定义绑定身份、真实贡献安装、补偿、驱动会话和时间释放协调。待 1.21.1 适配出现明确重复点后，再按状态边界抽取；不先新增通用世界接口。

以下实现继续留 target：

- `CombatRuntime`、`WorldServices`、`CombatServerRuntime` 的世界和服务器组合接线。
- `DefaultHealingService`、`DefaultShieldService` 的准入、身份复核、应用和通知协调；复用已有 common 计算及池状态。
- `CombatPersistence` 的各服务保存/恢复顺序；复用已有 common 编解码和值模型。
- 原生注册、事件、Mixin、生命与资源写入、持久化宿主、网络和客户端渲染。
- 木偶全部专用代码，即使其中部分记录类型只有 UUID、数字和字符串。

`ProjectileLaunch` 持有 `NativeEntityCapture.Participant` 与 `CapturedItem`，进而影响 `CombatMoment`、`DamageFormulaRequest/Result`。这些类型不能按 record 外观直接迁移；本计划不要求重做其公开模型。

## 1.21.1 实施前置检查

这些工作是独立 target 的编译和平台适配准备，不提供跨版本存档、网络或旧 API 兼容。

| 项目 | 当前依据 | 实施要求 |
| --- | --- | --- |
| 完整共享源码 | [1.21.1 构建脚本](../targets/neoforge-1.21.1/build.gradle) 排除了 combat、attribute、formula、effect、presentation | 正式接入时删除这些排除，完整编译 common；缺失适配留作真实编译错误，不增加同名替身 |
| Java 与库依赖 | [common 构建脚本](../common/build.gradle) 使用 Java 25、DFU 9.0.19 等；骨架使用 Java 21 | 实际编译核对共享源码语言/API 与目标依赖类路径；根据具体失败选择共同可编译实现或 target 边界，不能仅据工具链版本推断必须全面降级 |
| 平台事实与资源 | 共享原版说明、标签及 target 原生捕获 | 核对实际注册项、原生事件和资源格式；版本专有事实留对应 target |
| 验证工具 | [运行器](../scripts/verify-capabilities-runtime.py) 的 TARGET 固定为 26.1.2；边界和制品检查也含活动路径/版本假设 | 为 1.21.1 明确选择待验证 target 和相应原生探针，复用业务断言；不得把对 26.1.2 的通过结果视为 1.21.1 证据 |
| 发布与支持 | 骨架有发布阻断 | 适配和运行验证完成前保持阻断；启用时同步支持声明及适用的构建/CI 描述，不提前宣称支持 |

第一至三批完成后，可开始 1.21.1 平台适配。第四、五批按对应模块的适配需要完成，不把来源账本和完整伤害上下文共用作为启动门槛。各版本的平台入口分别实现，不在 common 中按版本选择实现。

## 实施验证安排

每批独立落地，先确认 26.1.2 现有行为，再进入下一批。纯类移动保持包名、字段和格式；状态拆分只调整执行位置，不同时改变业务规则。

按 [开发流程](DEVELOPMENT.md) 执行活动 target 的 `gradlew clean build`、common 独立构建和依赖检查，再执行 `verify-baseline.py`、`verify-common-boundary.py` 及相关脚本语法检查。迁移后同步更新验证脚本中的旧文件定位，保留原有语义断言。检查 common 源码与编译类均无原生依赖、无反向引用和重复类。

| 批次 | 现有 AutoJS 入口 | 需要核对或补充的断言 |
| --- | --- | --- |
| 独立类及资源 | `abilities`、`identifier`；资源实际加载检查 | 重复/冻结/容量拒绝，伪造注册凭据拒绝，续步 codec 边界，木偶标签仍生效 |
| 业务时间 | `business-time --start --restart` | 时间片续跑不重复推进，预算用尽后恢复、相位、取消、驱动释放、重启后的 consumed 与剩余量 |
| 能力状态 | `abilities --start --restart` | 正常/边界/失败执行，重复续步，部分写入，来源与定义绑定，冷却、参数和样本恢复 |
| 伤害决策 | `vanilla-integration`、`defense-inputs`、`native` | 非有限/零/满概率、随机消费次数、强制/抑制暴击、有序贡献、吸血确认金额和子治疗失败 |
| 展示 | `description-values`、`effect-foundation`、`verify-carried-effect-tooltips.py` | 注册冻结、未知行为、定义引用缺失、来源明细、客户端与服务端说明一致 |

表中的套件通过 `python scripts/verify-capabilities-runtime.py --suite <套件> --start` 调用；带 `--restart` 的项目必须实际保存后启动新进程。现有套件名称只表示可复用入口，实施时逐项确认断言覆盖，不将脚本存在视为行为通过。

验证证据按开发流程保存在本地 `build/verification/` 等输出目录；本计划不追加执行流水账。退出条件是对应批次的边界和行为检查通过，且仍由同一个领域所有者执行每项收益。
