<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — 插件内部指南

> `bakery-plugin` Gradle 插件的开发者与贡献者指南。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **版本**：`0.0.2` · **Group**：`education.cccp` · **插件 ID**：`education.cccp.bakery`
- **工具链**：Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **构建**：`./gradlew build` · **测试**：`./gradlew check` · **覆盖率闸门**：`./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 模块布局

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # 插件入口点 — 注册所有任务（afterEvaluate 分支）
│   │   ├── BakeryExtension.kt           # DSL 扩展（configPath、siteType、language、ia、themes、a11y 等）
│   │   ├── BakeryConstants.kt           # 任务分组（generate/deploy/publish/transform/info/collect/validate/audit）
│   │   ├── SiteConfiguration.kt         # site.yml 模型（bake、pushMaquette、pushPage、pushProfile 等）
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite（聚合）
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext（LENtille）
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # 无障碍 WCAG/RGAA（AuditTask、Auditor、ColorContrast、Dsl）
│   │   ├── llm/                         # LlmService、OllamaLlmService（LangChain4j）、IaConfig
│   │   ├── i18n/                        # I18nMigrationService、LlmTranslationApplier、audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl（LENtille：隔离 + 富化 + 预算）
│   │   ├── article/ theme/ scaffold/   # 意图 DSL（BKY-JB-8、BKY-IA-2、BKY-IA-1）
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # 本地镜像的 N0 契约（i18n、pipeline）
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # GradleTestKit 功能测试
├── src/e2eTest/kotlin/bakery/e2e/       # Playwright E2E 测试
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # 版本目录
```

## N0 契约（来自 workspace-bom MEMPHIS）

| 契约 | 构件 | 提供 |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _（镜像于 `contracts/i18n/`）_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _（镜像于 `contracts/pipeline/`）_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

BOM 通过 `bakery-plugin/build.gradle.kts:29` 中的 `implementation(platform("education.cccp:workspace-bom:0.0.1"))` 接入。

## 关键依赖

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — 静态站点引擎（api）
- **Thymeleaf** `3.0.14.RELEASE` — 模板渲染（testImplementation + e2e）
- **JGit** `6.10.0`（core + ssh + archive）+ **xz** `1.10` — gh-pages 部署（api）
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — AsciiDoc 图表（api，在 jbake bundle 中）
- **Firebase Auth + Comments** — BKY-JB-4（maquette + site）
- **Analytics + Newsletter** — BKY-JB-5（Plausible/Matomo）
- **LangChain4j + Ollama** (`langchain4j-ollama`) — BKY-IA-0 LLM 服务
- **kotlinx-coroutines** `1.10.2`（core + jdk8）— OllamaLlmService 异步
- **Arrow-kt**（core + fx-coroutines）— 函数式编程（Either、Validation、Raise DSL）
- **Rome** `2.1.0` — RSS 解析（CS-8，替换脆弱的 DocumentBuilderFactory DOM）
- **commons-io** `2.13.0` — 文件工具（api）
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright（NpxTask）
- **Playwright** — E2E 浏览器测试
- **Jackson**（kotlin + yaml）— YAML 配置、JSON 序列化
- **Graphify plugin** `0.0.2` — 知识图谱集成（LENtille）
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — RAG / 增强上下文消费者

## Ollama 实例（全局约束）

端口 `11434–11436` 被禁用。在 `11437–11465` 上轮换（29 个端口）。
授权模型：`gpt-oss:120b-cloud`、`gemma4:31b-cloud`。
Bakery 默认：`http://localhost:11464` + `gpt-oss:120b-cloud`（见 `IaConfig.kt`）。

## 测试矩阵

| 任务 | SourceSet | 范围 | 超时 | 并行度 |
|------|-----------|-------|---------|-------------|
| `test`           | test          | JUnit5 单元（排除 Cucumber 引擎 + `bakery.scenarios.*`） | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | GradleTestKit 功能 | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD（129/129 PASS） | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Playwright 浏览器（依赖 `installPlaywright`） | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | 覆盖率 ≥ 85 %（解析 Kover XML） | — | — |

验证接线（`build.gradle.kts`）：
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` **未**接入 `check` —— 通过 `./gradlew e2eTest` 显式运行。
- Cucumber 步骤位于 `src/test/scenarios`，features 位于 `src/test/features`。
- `testImplementation` 继承自 `functionalTest` 的 implementation（而非反向）。

## JVM 调优

- 所有 `Test` 任务：`jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`：`maxHeapSize = "1g"`，在 `doLast` 中清理超过 1 小时的过期 `gradle-test-*` 临时目录
- `e2eTest`：`maxParallelForks = 1`，`forkEvery = 1`（避免浏览器资源冲突）
- Kover：`includedSourceSets = ["main", "functionalTest"]`，在 `check` 时生成 HTML + XML 报告

## 构建命令

```bash
./gradlew build                                   # 完整构建（UT + functionalTest + Cucumber）
./gradlew build -x test                            # 仅编译
./gradlew test                                     # JUnit5 单元测试
./gradlew functionalTest                           # GradleTestKit 功能测试
./gradlew cucumberTest                             # Cucumber BDD（129/129 PASS）
./gradlew e2eTest                                  # Playwright E2E（自动安装 Chromium）
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # 覆盖率 ≥ 85 %
./gradlew publishToMavenLocal                      # 本地发布
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central（NMCP）
./gradlew installPlaywright                        # 为 E2E 安装 Chromium
```

> **规则 0 (AGENT.adoc)**：任何源码变更后，`./gradlew -q publishToMavenLocal` 是**强制**的，
> 先于消费者项目的任何功能测试。

## CI 流水线

`.github/workflows/test.yml`（单任务，`ubuntu-latest`，超时 20 min，JDK 24 temurin）：
1. `./gradlew build`（UT + Cucumber）—— 在 `bakery-plugin/`
2. `./gradlew installPlaywright` —— 安装 Chromium
3. `./gradlew e2eTest` —— Playwright E2E

辅助工作流：`website.yml`（站点部署）、`readme_plantuml.yml`（通过 readme-gradle 生成 README）。

## 发布（NMCP）

通过 `bakery-plugin/settings.gradle.kts` 中的 `com.gradleup.nmcp.settings`（`1.5.0`）配置。
凭据从 `~/.gradle/gradle.properties`（`ossrhUsername`、`ossrhPassword`）读取，
`publishingType = "AUTOMATIC"`。
签名使用 `useGpgCmd()`；当 `CI=true` 或版本以 `-SNAPSHOT` 结尾时跳过签名。
POM（在 `withType<MavenPublication>` 上）声明：
- Apache 2.0 许可证，开发者 `cccp-education`（cccp.edu@gmail.com）
- 指向 `github.com/cccp-education/bakery-gradle` 的 SCM
- 用于 groupId 迁移的可选 `relocationGroup` 注入

`gradlePlugin` 块：`id = education.cccp.bakery`、`implementationClass = bakery.BakeryPlugin`、
displayName "Bakery Plugin"、标签 `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`、
`website = https://cccp-education.github.io/`、`vcsUrl = https://github.com/cccp-education/bakery-gradle.git`。

发布命令：
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## EPIC 状态

所有 EPIC 已关闭（见 `bakery-plugin/.agents/INDEX.adoc`）：
- EPICs 1–13（治理、Supabase→Firebase、构建、联系表单、Cucumber 超时、publishProfile、
  YAML 容错、0.1.4 发布、脚手架 TDD、测试时长、KG、覆盖率）—— ✅
- **BKY-JB-0→9**（JBake 专家：脚手架、Thymeleaf、Google Forms、Firebase Auth、Analytics、
  主题、布局、AI 文章、E2E）—— ✅
- **BKY-LENS-1→6**（LENtille：隔离、富化、预算、BDD 重写、N3 连接、增强模板）—— ✅
- **BKY-IA-2**（参数化 IA 主题）—— ✅
- **BKY-KOV-1**（Kover 78 % → 85.88 %）—— ✅
- **BKY-PUB-1**（Maven Central 0.0.1，session 109）—— ✅
- **BKY-PUB-2**（Maven Central + Portal 0.0.2，session 143）—— ✅
- **BKY-COV-2**（微裂纹 DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt）—— ✅
- **BKY-I18N**（10 种语言，23/23 pts）—— ✅
- **BKY-I18N-MIG**（`migrateToI18n`，16/16 pts）—— ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN**（真实迁移 magic-stick、cccp.education、cheroliv.com）—— ✅
- **BKY-I18N-DELTA**（墨水经济法则，3/3 pts）—— ✅
- **BKY-CR-V5**（60 处代码异味）—— ✅
- **BKY-CR-V6**（3 处结构重复，8/8 pts）—— ✅
- **BKY-A11Y-1**（WCAG/RGAA 无障碍，13/13 pts）—— ✅
- **BKY-FIX-1**（无 site.yml 下的 deployProfile）—— ✅

## 架构文档

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — 绝对规则与治理
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — 会话、EPIC、路线图
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — 剩余工作
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — 当前会话任务
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — JBake 专家路线图
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — 代码审查 V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — 覆盖率分析
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Lazy/Eager 模式
- [README.adoc](../README.adoc) — 项目级 README（AsciiDoc）
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## 贡献

1. 构建可编译：`./gradlew build -x test`
2. 本地重新发布：`./gradlew -q publishToMavenLocal`（规则 0，强制）
3. 单元测试通过：`./gradlew test`
4. Cucumber 通过：`./gradlew cucumberTest`（129/129）
5. 覆盖率达标：`./gradlew koverThresholdCheck`（≥ 85 %）
6. 遵循 DDD 约定（值对象、端口/适配器、Arrow `Either`/`Raise`、无泄漏）
7. 未经备份切勿覆盖 `site.yml`（AGENT.adoc §1b —— 安全规则）
8. 会话结束时运行 6 步收尾流程（AGENT.adoc §3）

## 许可证

Apache License 2.0 —— 见 [LICENCE](../bakery-plugin/LICENCE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_