<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — 用户指南

> 静态站点生成 Gradle 插件 —— JBake + Thymeleaf + Firebase Auth + Analytics + 知识图谱 + LLM。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **版本**：`0.0.2` · **Group**：`education.cccp` · **插件 ID**：`education.cccp.bakery`
- **构建**：`./gradlew build` · **测试**：`./gradlew check`（JUnit5 + functionalTest + Cucumber + E2E Playwright）
- **覆盖率**：≥ 85 %（Kover `koverThresholdCheck`，接入 `check`）· **Cucumber**：129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 功能简介

`bakery-gradle` 通过 **JBake 2.7.0**（Thymeleaf 模板、AsciiDoc/Markdown 内容）生成并部署静态站点。它内置 Firebase Auth + 评论、Google Forms、Analytics/Newsletter、主题系统、无障碍审计（WCAG/RGAA）、10 种语言的 i18n 迁移、AI 辅助的文章/脚手架/主题生成（LangChain4j + Ollama），以及为 runner-gradle N3 复合上下文提供数据的 **LENtille** 增强上下文模式。`publishSite` 聚合任务可一键烘焙并部署到 `gh-pages`。

属于 CCCP Education 多插件生态系统的一部分：

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## 快速开始

### 1. 应用插件

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. 通过 DSL 或 `site.yml` 配置

```gradle
bakery {
    configPath = file("site.yml").absolutePath
    siteType    = "blog"        // "blog" | "basic"
    language    = "fr"          // ISO 639-1
    supportedLanguages = listOf("fr", "en", "ar")
    ia {
        baseUrl  = "http://localhost:11464"
        modelName = "gpt-oss:120b-cloud"
    }
}
```

`site.yml`（未纳入版本控制——未经备份切勿覆盖）驱动 JBake、部署、Firebase、Analytics、主题、布局及增强上下文配置。

### 3. 烘焙并部署

```bash
./gradlew publishSite        # 聚合：bake + 部署到 gh-pages
./gradlew bake               # 仅 JBake 渲染
./gradlew deploySite          # 将烘焙产物推送到 gh-pages
```

## 可用任务

| 任务 | 分组 | 说明 |
|------|-------|-------------|
| `bake`                      | —        | JBake 渲染（由 `configureBakeTask` 配置） |
| `generateSite`              | generate | 初始化站点 + maquette 目录（类型：`blog`/`basic`） |
| `generateSiteFromIntention` | generate | AI 辅助站点脚手架（交互式，Ollama） |
| `generateTheme`             | generate | 从目录生成主题（变体 + 覆盖） |
| `generateArticle`           | generate | AI 辅助博客文章 → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | 将现有 bakery 站点迁移到 i18n（扫描模板、提取硬编码文本、生成 `messages_{lang}.properties`） |
| `pagefind`                  | transform | 使用 Pagefind 为烘焙站点建立全文检索索引 |
| `deploySite`                | deploy    | 将烘焙产物推送到 GitHub `gh-pages` 分支（JGit） |
| `deployMaquette`            | deploy    | 将 maquette 文件推送到仓库 |
| `deployProfile`             | deploy    | 将 profile 文件（如 README.md）推送到 GitHub 仓库 |
| `publishSite`               | publish   | **聚合** —— bake + 部署 `gh-pages`（便捷任务） |
| `collectSiteConfig`         | collect   | 初始化 Bakery 配置 |
| `collectSiteContext`        | collect   | 收集烘焙站点上下文 → `build/bakery/metadata.json`（供 runner-gradle N3 使用） |
| `collectAugmentedContext`   | collect   | 收集 LENS 增强上下文（隔离 + 富化 + 预算）→ `build/bakery/augmented-context.json` |
| `serve`                     | info      | 本地服务烘焙站点（JavaExec） |
| `validateFirebaseConfig`   | validate  | 验证 Firebase 配置一致性（机械检查 + 可选 AI） |
| `verifyConfigurationMapping`| verification | 验证 YAML `site.yml` → `SiteConfiguration` 映射并屏蔽敏感信息 |
| `accessibilityAudit`       | audit     | 对烘焙站点执行 WCAG/RGAA 无障碍审计 —— JSON/ASCII 报告 |
| `installPlaywright`         | verification | 为 E2E 测试安装 Playwright Chromium 浏览器 |

> **降级模式**：当 `configPath` 缺失/无效时，插件回退到 **仅脚手架** 模式（`generateSite`、`generateSiteFromIntention`、`deployProfile`）。可通过 DSL、`gradle.properties`（`bakery.config.path=site.yml`）或 `-Pbakery.config.path=...` 设置 `configPath`。

## 扩展 DSL

```gradle
bakery {
    configPath        = "site.yml"
    sitesBaseDir      = "..."
    siteName          = "..."
    siteType          = "blog"          // "blog" | "basic"
    language          = "fr"
    supportedLanguages = listOf("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")

    ia {
        baseUrl   = "http://localhost:11464"
        modelName = "gpt-oss:120b-cloud"
    }
    googleForms   { /* BKY-JB-3 */ }
    firebaseAuth  { /* BKY-JB-4 */ }
    commentsConfig { /* BKY-JB-4 Firestore */ }
    analytics     { /* BKY-JB-5 Plausible/Matomo */ }
    newsletter    { /* BKY-JB-5 mail footer */ }
    theme         { /* BKY-JB-6 CSS vars, dark/light, logo */ }
    layout        { /* BKY-JB-7 FULL_WIDTH, SIDEBAR_LEFT, SIDEBAR_RIGHT, CENTERED */ }
    articleIntention  { /* BKY-JB-8 topic, ton, audience, keywords, lang */ }
    augmentedContext  { /* BKY-LENS budget + injection */ }
    scaffoldIntention { /* BKY-IA-1 description, siteType, lang, projectName */ }
    themeIntention    { /* BKY-IA-2 description, variante, surcharges */ }
    i18nMigration     { /* BKY-I18N-MIG siteDir, languages, defaultLanguage, dryRun */ }
    a11y              { /* BKY-A11Y-1 auditDir, reportPath, conformanceLevel (AA/AAA), failOnNonCompliant */ }
}
```

## 前置要求

- **Java** 24+（Kotlin 2.3.20 工具链）
- **Gradle** 9.5.1+
- **Node.js**（用于 Pagefind + Playwright，通过 Gradle Node 插件 `7.1.0`）
- **Ollama**，端口 `11437–11465`（端口 `11434–11436` 禁用）—— 模型 `gpt-oss:120b-cloud`、`gemma4:31b-cloud`
- **GPG**（仅在发布到 Maven Central 时需要）

## 构建与测试

```bash
./gradlew build                     # 完整构建（UT + functionalTest + Cucumber）
./gradlew test                      # JUnit5 单元测试（排除 Cucumber 引擎）
./gradlew functionalTest            # GradleTestKit 功能测试
./gradlew cucumberTest              # Cucumber BDD（129/129 PASS）
./gradlew e2eTest                   # Playwright E2E（自动安装 Chromium）
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # 覆盖率 ≥ 85 %
./gradlew publishToMavenLocal       # 本地发布
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## 故障排查

| 症状 | 修复 |
|---------|-----|
| `configPath is not set`          | 设置 `bakery { configPath = "site.yml" }`、`gradle.properties` 或 `-Pbakery.config.path=...` |
| `Failed to read site.yml`        | 检查文件是否存在且为有效 YAML；插件会回退到仅脚手架模式 |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"`（cucumberTest maxHeap 1g） |
| Playwright Chromium 缺失      | `./gradlew installPlaywright`（由 `e2eTest` 自动运行） |
| Ollama 连接被拒绝        | 验证端口在 `11437–11465` 之间；`11434–11436` 被禁用 |
| gh-pages 推送被拒            | 检查 JGit SSH 密钥及 `site.yml` 中的 pushPage 配置 |

治理信息见 [AGENT.adoc](../bakery-plugin/AGENT.adoc)，项目背景见 [README.adoc](../README.adoc)。

## 许可证

Apache License 2.0 —— 见 [LICENSE](../bakery-plugin/LICENCE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_