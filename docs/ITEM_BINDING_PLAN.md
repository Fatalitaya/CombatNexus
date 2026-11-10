# 战斗内容标准化注册与物品绑定实施计划

本计划面向活动 target `targets/neoforge-26.1.2`（Java 25），确定行为类型扩展、命名战斗定义和物品战斗绑定三类入口的设计及实施顺序。本文规定 API、分层语义及实施验收范围；运行证据保存在本地验证输出中。开发约束仍由 [仓库约定](../AGENTS.md)、[开发流程与平台边界](DEVELOPMENT.md)和[领域合约](DOMAIN_CONTRACTS.md)维护。

## 目标与范围

Java 与数据包共用定义编译和绑定解析机制，继续使用现有伤害管线、Effect 与来源安装服务。本次覆盖 Behavior、Effect、entry template、affix template，以及物品默认绑定、数据包覆盖、实例补丁和客户端展示。

Item、Block、Entity、原版 MobEffect 保留 NeoForge 原生注册方式。治疗、护盾和恢复继续使用现有领域入口，不重构其定义 API。外部装备槽和脚本适配后置；首版使用现有 `EquipmentPosition`，不提前引入通用槽位或脚本系统。

组合声明、原生注册和组件接线归活动 target；只有实际依赖链完全平台无关的值对象进入 common。README 不在修改范围内。

## 当前基础与迁移落点

- `CombatRegistrar.behavior(BehaviorType<T>)` 已提供行为组件类型注册入口。
- `DamageItemTemplateReferences` 分别保存 entry 与 affix 模板 ID，`DamageModuleItemApi` 通过 `RuleItemAccess` 操作实例规则及模板引用。
- `EquipmentBindings` 保存具有独立 ID、定义 ID 和装备位置的 Behavior／Effect 绑定。
- `EquipmentBehaviorController` 已维护安装账本、撤销、重试、恢复和代际刷新；新绑定继续接入这些机制。
- `DefinitionGeneration` 是请求持有的完整不可变资源代际；新定义与绑定索引纳入该边界。

标准绑定组件 `combatnexus:item_bindings` 替代原模板引用组件和装备绑定组件，实例补丁组件为 `combatnexus:item_binding_patch`。仓库内 API、装备观察、伤害捕获、保存恢复、提示、命令和 AutoJS 调用方一并迁移，不并行执行旧绑定路径。物品实例具体伤害规则保留现有领域表示，运行中的效果状态仍归安装实例管理。

这是公开 API 与组件格式的破坏性变更。不添加隐式旧接口、旧存档兼容、schema version、protocol version 或相应版本校验。

## 标准化注册与命名定义

### 三类入口

| 入口 | 职责 |
| --- | --- |
| `CombatRegistrar.behavior(BehaviorType<T>)` | 注册组件类型、实现类及 Codec，不注册具体玩法定义 |
| `CombatRegistrar.definitions()` | 按 `NexusId` 注册命名 Behavior、Effect、entry template、affix template 声明 |
| `CombatItems.defaults(...)` 与 `CombatRegistrar.itemBindings(...)` | 为新物品声明默认绑定，或为已有物品贡献绑定 |

行为类型继续由现有注册事件收集、校验和冻结。命名定义入口分别提供 `behavior`、`effect`、`entryTemplate`、`affixTemplate` 方法，接收 ID 和类型化声明。不同定义类别分别管理身份；同类别 Java 重复 ID 拒绝，启动冻结后禁止新增定义。

### Builder 与不可变 Spec

- Behavior Builder 构建有序组件、priority 和 duration。
- Effect Builder 构建寿命、叠加、属性、Behavior ID 引用、限制及展示信息。
- 伤害模板 Builder 包装既有 entry／affix 定义结构，不增加第二套规则表达式系统。

具体声明为 `BehaviorSpec`、`EffectSpec`、`EntryTemplateSpec` 和 `AffixTemplateSpec`；注册入口同时接收 Spec 或对应 Builder Consumer。Builder Consumer 在声明时执行并立即产出不可变 Spec，注册表保存 Spec，不保存延迟执行的 Builder 回调。Effect Spec 保留 Behavior ID，不能提前固化展开后的 `BehaviorPlan`，以保证重载时引用胜出的 Behavior 定义。

### Java 默认值与数据包覆盖

Java 定义作为默认值；数据包同类别、同 ID 定义整体覆盖。每次重载先选择胜出声明，再解析引用、编译执行与展示数据，纳入现有完整候选代际。移除数据包覆盖后重新采用 Java 默认定义。

Java 与数据包使用同一套语义校验及容量限制，包括身份、范围、组件顺序、重复类型、引用和结构预算。任何失败保留旧代际，不能提前发布部分定义或展示数据。

注册不授予收益。运行时继续通过 `CombatWorld.install(DefinitionInstallation)` 和来源服务安装、撤销；普通扩展不直接依赖内部 Runtime。

## 物品默认声明与统一组件

新增 `CombatItems.defaults(Item.Properties, builderConsumer)`，配置并返回原生 `Item.Properties`，继续使用原生物品注册工厂。Builder 支持 entry、affix、Effect 和 Behavior 引用；每条引用都显式携带独立 binding ID，装备绑定附带 `EquipmentPosition` 集合。

采用统一的不可变 `CombatItemBindingSpec`，保存有序、类型化的绑定条目。统一组件只承载绑定声明，不承载具体实例伤害规则或运行状态。entry 与 affix 使用不同声明方法和类型，不能因 ID 相同混淆模板类别。

三类身份分别建模：

| 身份 | 含义 |
| --- | --- |
| definition ID | 引用哪个类别中的命名定义 |
| binding ID | 当前物品声明中可独立替换、移除的绑定条目 |
| 运行来源身份 | 哪个世界／实体会话、提供者及槽位上的安装 |

绑定列表中的 ID 唯一，不以模板 ID 自动代替 binding ID。重复调用 `defaults` 追加不同 ID 的声明；同 ID 拒绝，不能无提示覆盖。Consumer 即时执行，组件仅保存不可变值和引用。

默认层从物品原型声明读取，实例层从独立补丁读取。禁止把 `stack.get(...)` 返回的有效默认值再次作为实例增量加入。通过原生默认组件初始化事件观察本次重载待发布的最终原型声明，按本次 registry lookup 交给候选编译器，不能在此阶段读取尚未绑定或上一代的 `Item.components()`。注册阶段只保存稳定定义 ID，不提前解析规则对象。

## 分层合并与数据包操作

固定合并顺序为：

`物品默认声明 → 已有物品的 Java 绑定贡献 → 数据包绑定操作 → 实例补丁`

伤害解析和装备对账消费同一份合并结果，但保留独立执行生命周期：伤害模板进入请求捕获及现有规则管线，装备 Effect／Behavior 通过来源安装和撤销，来源及展示信息交给既有快照和描述器。

### 已有物品的 Java 贡献

通过 `CombatRegistrar.itemBindings(...)`，以贡献 ID、物品 ID 和不可变声明提交绑定贡献，冻结后不可修改。贡献进入统一解析器，不修改第三方物品属性。贡献 ID 唯一，按贡献 ID 稳定排序；条目按各声明内部顺序追加，重复 binding ID 拒绝，不依赖注册回调顺序。

### 数据包绑定

新增目录 `data/<namespace>/combatnexus_item_bindings/<path>.json`。每份资源包含 `item`、`priority`、`operations` 字段，指定明确物品 ID；首版不加入标签选择器。操作包含 `action` 和 `id`；ADD／REPLACE 还包含 `binding`，其字段为 `id`、`kind`、`definition`、`slots`，内外 binding ID 必须相同。kind 为 ENTRY／AFFIX／EFFECT／BEHAVIOR，伤害模板的 slots 为空。

同路径文件继续遵循原版资源覆盖。不同胜出资源按 priority 升序、资源 ID 排序，再依次执行文件内操作：

| 操作 | 语义 |
| --- | --- |
| `ADD` | 添加不存在的 binding ID；已存在则拒绝；新条目追加到末尾 |
| `REPLACE` | 替换已有 binding ID，保留其列表位置；不存在则拒绝 |
| `REMOVE` | 移除指定 binding ID；不存在时无操作 |

最终结果保留确定执行顺序。数据包非法操作、缺失引用及超预算导致整个候选代际失败。操作不扩展为第二套同路径资源合并机制。

### 实例补丁

实例补丁 `CombatItemBindingPatch` 保存 `clearInherited` 标志及有序 operations，使用同一套 `ADD / REPLACE / REMOVE` 语义。

| 实例状态或操作 | 结果 |
| --- | --- |
| 未设置补丁，或 `clearInherited=false` 且操作为空 | 继承前层结果 |
| 删除补丁组件 | 恢复继承 |
| `clearInherited=true` | 清空前层结果后执行本层操作；操作为空时显式清空 |
| 保存 `REMOVE` | 删除指定能力并保留删除意图，不能通过删除默认组件表达 |

实例补丁最后执行，因此数据包变化不能越过实例删除或清空意图。补丁写入前完整校验，失败保留原值。重载后若补丁变得不适用，明确拒绝该物品绑定解析，不静默回退到不同能力，也不把有效的全局候选代际当作实例补丁写入成功。

组件数据不授予权限；实例修改通过 `CombatItems.patch(ServerLevel, ItemStack, CombatItemBindingPatch)`，删除补丁通过 `CombatItems.inherit(ServerLevel, ItemStack)`，继续使用服务端授权入口和结构预算。单物品最多 32 条 entry、32 条 affix、8 条装备绑定；补丁最多 144 个操作，数据包绑定资源及 Java 贡献分别最多 256 份。

## 代际发布、来源与装备对账

新定义与绑定索引共同发布到 `DefinitionGeneration`。单次请求始终使用捕获的完整定义句柄；成功重载影响后续请求，在途请求不混用新旧代际，失败保留旧代际。

装备继续按世界／实体会话、槽位、binding ID 对账，物品类型与声明参与变化判定。不引入每件物品 UUID；相同类型、相同声明的直接替换不刷新收益。实际卸下后重新装备按既有安装生命周期处理。

定义重载不能通过全量清空再安装返还已消费次数。相同来源中仍存在的组件保留消费状态及周期进度；删除组件撤销其贡献，新增组件按新安装初始化。部分安装和撤销失败沿用来源账本协调，不重复补发收益。

实例补丁变化必须触发装备对账，即使物品类型和槽位未变。读档恢复使用有效绑定结果核对来源，保留剩余寿命、周期进度和已消费状态，避免重复发放。

## 客户端展示与权限边界

同步展示所需的定义描述和绑定索引，客户端与服务端采用相同合并规则。物品提示结合当前实例补丁及实例规则，显示数据包新增能力和实例覆盖后的结果，不重复计入默认绑定。

客户端只合并声明和格式化说明，不执行行为、安装来源或计算战斗结果。重载和会话切换更新相应展示缓存，继续遵守既有隐藏组件、高级提示、结构预算和权限边界，不新增格式或协议版本字段。

## 分步实施与验收

### 第一步：命名定义注册与共同编译

1. 保留行为类型注册职责，新增 `definitions()` 及四类类型化 Builder／Spec。
2. 声明时执行 Consumer，接入收集、重复身份校验和启动冻结。
3. 将 Java 默认值与数据包胜出声明汇入共同引用解析、执行编译及展示编译。
4. 接入完整候选代际，验证覆盖、恢复默认、缺失引用和失败保留旧代际。

### 第二步：标准组件、实例补丁与物品 DSL

1. 实现有序 `CombatItemBindingSpec`、独立补丁和统一操作解析器。
2. 实现 `CombatItems.defaults(...)`，确保重复调用追加及重复 ID 拒绝。
3. 迁移模板引用和装备绑定组件及仓库内全部调用方，保留实例伤害规则的现有表示，移除旧绑定执行路径。
4. 验证默认层只计入一次、entry／affix 类型区分、继承／清空／删除和服务端授权写入。

### 第三步：绑定资源、已有物品贡献与代际对账

1. 实现冻结的 `itemBindings(...)` 贡献入口及稳定排序。
2. 加载 `combatnexus_item_bindings`，校验物品、操作、引用和预算，生成绑定索引。
3. 将定义和绑定索引整批发布，伤害捕获与装备观察消费同一有效绑定结果。
4. 接入补丁变化检测、组件级重载协调及读档来源核对，验证失败重试和消费状态保留。

### 第四步：客户端同步与完整行为验证

1. 同步只读定义描述与绑定索引，提示复用相同合并规则，并结合实例规则渲染。
2. 处理重载和会话切换的展示缓存失效。
3. 补齐 AutoJS 行为覆盖并完成构建、common 独立编译和边界检查。
4. 如实施需要调整设计，直接更新本计划对应条款，不追加逐轮改动、验收或交接记录。

## 验证矩阵与执行要求

行为验证使用 AutoJS，覆盖正常、边界和失败语义：

- 自定义 BehaviorType → Java 命名 Behavior → Effect → 物品绑定 → 实际生效与独立撤销。
- Java 定义被数据包整体覆盖、移除覆盖后恢复默认；同类重复 ID、缺失引用、非法参数、容量超限和冻结后注册均拒绝。
- entry／affix 不混淆，默认绑定只执行一次；重复 `defaults` 追加不同 ID、拒绝同 ID。
- 各层 `ADD / REPLACE / REMOVE` 的顺序和失败语义、继承、删除补丁、显式清空、补丁写入失败保留原值及无权限修改符合约定。
- 重载使实例补丁不适用时明确拒绝该物品解析，不回退到不同能力。
- 双槽与多绑定独立，撤销一个来源不影响另一个；相同装备直接替换不刷新，实际卸下再装备按生命周期处理，补丁变化及时对账。
- 成功重载采用新定义与绑定，失败保留旧代际，在途请求不混用。
- 重载与新进程读档不补发消费状态，保留周期进度；新增／删除组件以及部分安装、撤销失败可正确协调。
- 提示包含数据包新增能力，实例覆盖后的展示与服务端结果一致；重载和会话切换不展示旧缓存。

实现完成后，在活动 target 执行 `gradlew clean build`，在仓库根执行 common 独立构建及开发流程规定的边界检查，运行相关 AutoJS 套件。涉及 Effect、定义和持久化的验证使用 `effect-foundation --start --restart --reload`，客户端提示运行对应展示验证脚本；根据实际接线补充相关套件。

构建成功、依赖安装或脚本存在不等于行为通过。运行证据保存到本地 `build/verification/` 等输出目录，不将验收流水写入本文。
