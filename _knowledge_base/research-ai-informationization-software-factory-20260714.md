# AI 时代信息化自主建设与软件工厂调研笔记

调研日期：2026-07-14

调研目标：为《从项目实践到软件工厂：AI 时代信息化自主建设能力探索》45 分钟内部汇报建立项目事实与权威知识证据链。

## 关键问题

1. 如何专业、准确地比较单体、微服务与云原生架构，并解释本项目为何选择 RuoYi-Cloud-Plus？
2. RuoYi、RuoYi-Cloud、JeecgBoot、Yudao 等开发底座的定位与取舍是什么？
3. DDD 限界上下文、事件驱动、容器化、K3s、DevSecOps 与敏捷交付应如何准确表述？
4. AI 辅助开发的收益、风险和人机责任边界有哪些权威证据？
5. 软件工厂如何与单位自主研发结合，形成可执行的未来路线？

## 项目事实发现

- 项目 README 将系统定义为单位内部信息服务门户，以 RuoYi-Cloud-Plus 为底座，围绕服务门户、管理后台、应用入口、资料共享和服务论坛组织能力。
- 当前门户已按 Portal Kernel、AppCenter、Resources、Forum、RequiredKnowledge 五个限界上下文拆成独立 Maven 模块和服务。
- 跨 BC 协作主要经 RabbitMQ 事件完成；同步调用只允许面向通用服务，并保留有限的内核统计聚合例外。
- 开发与验证保留 Docker Compose 路径；目标生产形态设计为麒麟 V10、ARM64、K3s 单节点可扩展部署，运行时由 K3s 内置 containerd 管理。
- 工程治理包括 AGENTS.md、架构法源、Spec/Plan、端口表、运行手册、数据库迁移三件套、Nacos 配置发布规则、编译/单测/前端构建/设计审计/冒烟门禁和 GitHub PR 流程。

## 外部研究发现

### 开发底座定位（第 1 轮）

- RuoYi-Cloud 官方将自身定位为前后端分离的微服务版本，当前官方主线基于 Spring Boot、Spring Cloud Alibaba、Nacos 等体系；相较基础 RuoYi，其核心价值是已有的网关、注册配置、认证与服务治理骨架。
- JeecgBoot 官方定位更偏 AI 低代码/零代码平台，强调在线表单、代码生成、流程、报表与快速搭建，微服务栈使用 Spring Cloud Alibaba、Nacos、Gateway、Sentinel、SkyWalking。它适合配置化和 CRUD 密集型场景，但平台抽象更重。
- Yudao（ruoyi-vue-pro）官方同时维护单体和微服务形态，业务组件覆盖面很广，包含 BPM、商城、支付、AI、IoT 等，并公开较多单元测试信息；优势是业务模块丰富，代价是基线规模和理解成本更高。
- 本项目实际底座是 RuoYi-Cloud-Plus，而不是原始 RuoYi-Cloud。选择理由应基于项目现有事实表达为：Java 17/Spring Boot 3 技术基线、微服务骨架、多租户和工程增强、前后端分离、较完整的通用能力，以及便于裁剪和扩展；不能把社区项目间的主观优劣写成绝对结论。
- RuoYi-Cloud-Plus 官方文档将其定位为“微服务权限管理系统”，强调组件化、模块化、轻耦合和高扩展；本项目当前基线实际使用 Spring Boot 3.5.15、Spring Cloud 2025.0.3、Dubbo 3.3.6、Sa-Token 和 MyBatis-Plus，并已按业务需要大量裁剪上游模块。

### 阶段摘要（第 1 轮）

产品比较应采用“定位—适配场景—本项目取舍”三列，而不是简单排名。需要明确承认：本项目当前业务规模采用模块化单体也能满足，选择云原生微服务底座主要服务于全生命周期实践、架构边界训练、部署治理和未来扩展验证。

### 架构、DDD 与部署（第 2 轮）

- Martin Fowler 的“Monolith First”指出，微服务存在额外的分布式系统成本，只有在复杂度足以覆盖这笔成本时才有价值；服务边界不稳定时，先做模块化单体通常更稳妥。这为汇报中“本项目用单体也够”提供专业依据。
- Microsoft 的 DDD 微服务指南强调：服务应围绕业务能力而不是技术层拆分；限界上下文的价值是让领域模型在明确边界内成立，并通过统一语言减少业务与技术之间的误解。限界上下文是微服务候选边界，不必机械地一一对应物理服务。
- CNCF 对云原生的定义不等同于“上了 Kubernetes”：容器、微服务、不可变基础设施和声明式 API 是典型手段，目标是形成松耦合、韧性、可管理、可观察的系统，并借助自动化实现频繁、可预测的变更。
- Docker 主要解决应用及依赖的一致打包、交付和运行环境问题；Kubernetes/K3s 进一步解决声明式编排、健康检查、故障重启、服务发现和期望状态维持。两者解决的问题层级不同。
- K3s 是完全兼容 Kubernetes 的轻量发行版，单二进制、依赖少、内置 containerd，官方明确列出 ARM、边缘和离线环境等适用场景，与本项目麒麟 V10、飞腾 ARM64、离线部署约束吻合。

### 软件生命周期、敏捷与安全（第 3 轮）

- ISO/IEC/IEEE 12207:2026 为软件全生命周期建立通用过程框架，覆盖构想、开发、运行、支持和退役，也适用于内部研发、外部采购、迭代和敏捷方法。汇报可借此说明“自主建设能力”不是编码能力，而是完整生命周期过程能力。
- Agile Manifesto 的原则把“尽早且持续交付有价值的软件”“业务人员与开发人员持续协作”“可工作的软件是进度主要度量”“持续关注技术卓越和良好设计”放在核心位置。因此敏捷不能被解释为省略设计、测试和文档。
- NIST SSDF 建议把安全实践嵌入既有 SDLC，而不是在发布前临时补安全；其目标包括准备组织、保护软件、生产安全软件、响应漏洞。软件工厂中的模板、流水线和门禁应承载这些治理要求。

### AI、平台工程与软件工厂（第 4 轮）

- DORA 2024 的调查结果显示，AI 与个体生产力、心流和满意度提升相关，但同时观察到交付稳定性和吞吐量的负面影响；DORA 因此强调小批量、健壮测试和以用户为中心等基础能力。这支持“AI 是放大器而不是质量保证”的核心判断。
- DORA 强调以最终用户为中心、稳定的组织优先级和实验式持续改进。软件工厂不应只优化开发者生成代码的速度，而要缩短从业务问题到用户反馈的完整回路。
- 本汇报可把“软件工厂”定义为组织内部的软件交付生产系统：通过标准模板、可复用组件、参考架构、AI 上下文、自动化流水线、环境与治理规则，为常见项目提供受支持的黄金路径，同时允许有依据的例外。该定义更接近现代平台工程，而不是传统的代码生成器或某一个产品。
- DORA 的平台工程能力页面将内部开发平台的核心价值概括为简单、自助式的“黄金路径”，使开发者把注意力集中到用户价值；Google Cloud 进一步强调平台应作为产品运营，开发者是平台的内部客户。
- Google Cloud 对黄金路径的示例资产包括入门指南、骨架代码、依赖管理、CI/CD 模板、基础设施即代码、Kubernetes 清单、策略护栏、日志监控和参考文档。它与本项目的 Spec/Plan、底座、部署清单、规约、门禁、RUNBOOK 高度对应，可作为“项目资产如何进入软件工厂”的映射依据。
- 平台工程还应区分黄金路径、硬性护栏、故障安全网和人工检查点。不是所有规则都应做成阻断，也不能把所有风险交给人工审批。

### 阶段摘要（第 2—4 轮）

权威资料共同指向一个结论：敏捷来自快速、可靠的反馈闭环，而不是省略工程过程；云原生来自自动化、声明式、可观察和可恢复的运行方式，而不是技术名词堆叠；AI 只有嵌入稳定的工程系统，才可能转化为组织交付能力。

## 调研结论

### 关键事实

1. 当前项目以 RuoYi-Cloud-Plus 为底座，门户五个 BC 已完成物理服务拆分；Docker Compose 路径已进入主线，K3s 仍是目标设计而非已验证部署。
2. 当前业务规模采用模块化单体也具有合理性；微服务是否值得取决于故障隔离、独立发布、团队边界和目标运行环境等约束，而不是功能数量本身。
3. DDD 的核心价值是围绕业务能力建立统一语言和边界；BC 是微服务候选边界，不应机械地把每个模块都拆成服务。
4. Docker 解决容器化制品与运行隔离，Compose 解决多容器编排；K3s 增加声明式资源、探针和期望状态恢复，但单节点不等于高可用。
5. ISO 12207、NIST SSDF、Agile Manifesto、DORA 和平台工程资料共同支持“全生命周期、内建安全、快速反馈、用户价值和黄金路径”的软件工厂方法。
6. DORA 2024 表明 AI 收益与交付稳定性风险可以同时出现，因此 AI 必须进入受控任务、自动验证和人工发布决策闭环。

### 待确认问题

- K3s 是否已在仓库之外的目标服务器完成实际部署和故障演练；在获得证据前不得在汇报中声称已落地。
- 单位现有“软件工厂”的产品、组织和权限边界；当前稿件采用现代平台工程的操作性定义，后续应与单位真实平台对齐。

### 写作建议

- 第一部分以真实项目生命周期贯穿，不把框架比较讲成产品评测。
- 所有关键能力标记“已落地、已验证、目标设计/待验证”。
- 软件工厂表述为标准资产、平台能力、组织角色和反馈度量组成的持续生产系统。
- 完整技术稿保留全部细节；45 分钟逐字稿预先裁剪，不依赖现场临时跳段。

## 来源列表

| 来源 | URL | 发布/访问日期 | 可信度 |
|---|---|---|---|
| RuoYi-Cloud 官方 README | https://gitee.com/y_project/RuoYi-Cloud/blob/master/README.md | 访问 2026-07-14 | 高 |
| JeecgBoot 官方 GitHub | https://github.com/jeecgboot/JeecgBoot | 访问 2026-07-14 | 高 |
| Yudao/ruoyi-vue-pro 官方发行页 | https://gitee.com/zhijiantianya/ruoyi-vue-pro/releases | 访问 2026-07-14 | 高 |
| RuoYi-Cloud-Plus 官方文档 | https://gitee.com/dromara/plus-doc/blob/master/_coverpage.md | 访问 2026-07-14 | 高 |
| Martin Fowler, Monolith First | https://martinfowler.com/bliki/MonolithFirst.html | 2015-06-03 | 高 |
| Microsoft, Use domain analysis to model microservices | https://learn.microsoft.com/en-us/azure/architecture/microservices/model/domain-analysis | 访问 2026-07-14 | 高 |
| CNCF Cloud Native Definition | https://www.cncf.io/about/who-we-are/ | 访问 2026-07-14 | 高 |
| Docker 官方概览 | https://docs.docker.com/get-started/docker-overview/ | 访问 2026-07-14 | 高 |
| Kubernetes Self-Healing | https://kubernetes.io/docs/concepts/architecture/self-healing/ | 访问 2026-07-14 | 高 |
| K3s 官方文档 | https://docs.k3s.io/ | 访问 2026-07-14 | 高 |
| ISO/IEC/IEEE 12207:2026 | https://www.iso.org/standard/90219.html | 2026-04 | 高 |
| Agile Manifesto Principles | https://agilemanifesto.org/principles | 访问 2026-07-14 | 高 |
| NIST SP 800-218 SSDF 1.1 | https://csrc.nist.gov/pubs/sp/800/218/final | 2022-02 | 高 |
| DORA 2024 Report | https://dora.dev/research/2024/dora-report/ | 2024 | 高 |
| DORA Platform Engineering Capability | https://dora.dev/capabilities/platform-engineering/ | 访问 2026-07-14 | 高 |
| Google Cloud, Golden Paths | https://cloud.google.com/blog/products/application-development/golden-paths-for-engineering-execution-consistency | 2023-09-11 | 中高 |
| Google Cloud, Platform Control Mechanisms | https://cloud.google.com/blog/products/application-modernization/platform-engineering-control-mechanisms | 2025-08-15 | 中高 |
