# Playwright MCP Document

本目录用于沉淀“只使用 Playwright MCP”的端到端测试文档。

## 文档清单

- `React-SpringBoot-Playwright-MCP-E2E-Guide.md`
  - 说明测试边界、环境准备、执行策略、断言与证据标准。

- `通用E2E案例矩阵-Playwright-MCP.md`
  - 提供 48 个可执行通用场景，覆盖 React 前端与 Spring Boot 后端联动。

- `P0执行清单-Playwright-MCP.md`
  - 将全部 P0 用例拆成 5 个发布阻断批次，含通过门槛与失败阻断规则。

- `P1-P2回归清单-Playwright-MCP.md`
  - 将 P1/P2 用例按功能域拆分为日常回归与扩展回归，含最短路径与全量路径。

- `并行执行排程-Playwright-MCP.md`
  - 给出 2 人/3 人并行执行方案、数据隔离策略与合并报告模板。

## 使用顺序

1. 先阅读 `React-SpringBoot-Playwright-MCP-E2E-Guide.md`
2. 再按 `通用E2E案例矩阵-Playwright-MCP.md` 确认用例范围
3. 发布前执行 `P0执行清单-Playwright-MCP.md`
4. 日常/发布回归执行 `P1-P2回归清单-Playwright-MCP.md`
5. 多人并行执行时参考 `并行执行排程-Playwright-MCP.md`

## 约束重申

- 仅使用 Playwright MCP。
- 不引入其他测试工具。
