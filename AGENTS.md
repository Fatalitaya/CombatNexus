# CombatNexus 开发约定

仓库开发规范仅维护三个文件：本文件、[开发流程与平台边界](docs/DEVELOPMENT.md)、[领域合约](docs/DOMAIN_CONTRACTS.md)。直接更新对应约束，不追加阶段交接、过时方案或逐轮验收流水账。

- 仅维护 NeoForge；活动 target 为 `targets/neoforge-26.1.2`，Java 25。NeoForge 1.21.1 是未启用骨架，Fabric 和 Forge 不在开发范围内，未移植 target 禁止发布。
- 模组 ID 为 `combatnexus`，根包为 `io.github.fatalitaya.combatnexus`。伤害属于 `combat.damage` 子域；属性、Effect、治疗、护盾、恢复与展示各守自身职责。
- `common` 只保留平台无关、可独立编译的代码与共享资源。Minecraft 依赖实现及依赖调用链全部归活动 target；target 可保留业务包名，不要求全部位于 platform 包。木偶专用代码和资源全部归 target。
- 共用源码以 source-set sharing 编译，target 不依赖 `project(':common')`。common 使用普通 Java library，不依赖 Minecraft、加载器或 target 输出，不纳入 target 源码或资源，不生成发布 Jar。允许共享包名，禁止重复类和 common 反向依赖 target。
- 不用 Object 包装原生对象、反射、同名替身、源码排除或运行时版本分支伪装平台无关与兼容。
- `docs/DamageNexus-main` 是只读迁移参考，不能作为编译输入或修复目标。迁移映射见 `docs/legacy/migration/damage-source-manifest.json`。第三方版本参考与机器可读清单不属于开发规范正文。
- 属性、Effect 和面板共享服务端计算快照及来源明细；客户端只展示。复用原版已有属性，同一收益只有一个执行所有者。
- 修改命名空间须检查注册 ID、数据目录、Mixin、网络、配置、语言、Java API 和验证脚本。公开 API 破坏性变更明确说明，不添加隐式旧接口兼容，不宣称旧存档或跨版本网络兼容。
- 行为验证使用 AutoJS 配合脚本，不恢复 JUnit、GameTest 及其专用夹具和构建接线。新行为覆盖正常、边界和失败语义；不降低 Mixin require 规避失败。
- 提交前在活动 target 目录执行 `gradlew clean build`；Mixin、事件、网络和资源变化须做相应运行验证。编译成功、依赖安装或脚本存在均不等于行为通过。
- 依赖固定版本并记录官方来源；不提交个人绝对路径、凭据、缓存或 `build/` 输出。支持声明、CI 启用状态和发布范围保持一致，空入口构建不算功能覆盖。
- 除非用户主动要求，否则不改动 README。
- 不要将改动、验收、交接信息写入文档，默认仅作源码修改。
- 除非明确要求，否则不要考虑任何跨版本兼容，也不要在内部制造任何schema version或protocol version与校验行为。