# AMenu

AMenu 是一个面向 Spigot、Paper 与 Folia 的现代化 Minecraft 菜单插件。  
它不是皮肤插件，皮肤只是早期示例；现在 bundled 默认内容已经改成“综合服主菜单 + 功能实验室”结构。

## 命令

- `/amenu`
- `/menu`
- `/amenu open <menuId>`
- `/amenu reload`
- `/amenu give <bindingId>`
- `/amenu sync-bundled`

## 当前 bundled 菜单

- `menu.yml`
  服务器综合菜单模板，适合直接改成你的正式首页。
- `showcase.yml`
  功能实验室，集中放分页、输入、绑定、后台演示等测试能力。
- `history.yml`
  分页与异步卡片展示示例。
- `runtime.yml`
  稳定的 CHAT 输入示例与分页实验室入口。
- `admin.yml`
  权限、拒绝动作、刷新与管理工具示例。

## 推荐 DSL

AMenu 现在推荐的写法是：

- `Title`
- `Shape`
- `Fill`
- `BUTTONS`
- `display`
- `actions`
- 按钮内联 `input`

同时也保留更短的直写字段：

- `material`
- `name`
- `lore`
- `click`
- `head`
- `glow`

## 动作写法

现在动作不再必须写成 `[close]` 这种带中括号的形式了。  
你可以直接写更自然的简写：

```yaml
actions:
  all:
    - "close"
    - "delay: 1"
    - "player: home"
    - "console: say hello"
    - "message: <green>已打开</green>"
    - "menu: history"
    - "sound: ENTITY_EXPERIENCE_ORB_PICKUP:1:1.1"
```

已支持的简写动作包括：

- `close`
- `back`
- `refresh`
- `delay: <ticks>`
- `player: <command>`
- `command: <command>`
- `console: <command>`
- `message: <text>`
- `menu: <menuId>`
- `open: <menuId>`
- `prompt: <promptId>`
- `page: next <region>`
- `page: previous <region>`
- `page: refresh <region>`
- `sound: <sound>:<volume>:<pitch>`

旧写法如 `[close]`、`[player] home`、`[open history]` 仍然兼容。

## 综合菜单示例

```yaml
Title: "服务器主菜单"

Shape:
  - "`档案``家`#`传送`#`便携`#`帮助``活动`"
  - "##`商店``任务``福利``地标``称号`##"
  - "##`领地奖励``在线奖励``随机传送``功能实验室``管理`##"
  - "#########"
  - "#`返回主城`#`资源说明`#`模板说明`#`现代路线`#"
  - "#########"

Fill:
  display:
    mats: WHITE_STAINED_GLASS_PANE
    name: " "

BUTTONS:
  "家":
    display:
      material: RED_BED
      name: "<light_purple><bold>温馨小家</bold></light_purple>"
    actions:
      all:
        - "close"
        - "delay: 1"
        - "player: home"

  "帮助":
    display:
      material: BOOK
      name: "<yellow><bold>帮助中心</bold></yellow>"
    actions:
      all:
        - "close"
        - "delay: 1"
        - "player: help"

  "功能实验室":
    display:
      material: NETHER_STAR
      name: "<light_purple><bold>功能实验室</bold></light_purple>"
    actions:
      all:
        - "menu: showcase"
```

## 输入示例

```yaml
"快捷输入":
  display:
    material: WRITABLE_BOOK
    name: "<aqua><bold>快捷输入</bold></aqua>"
  actions:
    all:
      - "close"
  input:
    type: CHAT
    start:
      - "<yellow>请输入任意文本。</yellow>"
    cancel:
      - "message: <yellow>已取消。</yellow>"
      - "menu: showcase"
    submit:
      - "message: <green>收到输入：<white>{input}</white></green>"
      - "menu: runtime"
```

## 分页示例

```yaml
Pages:
  showcase:
    symbol: "I"
    async-delay: 8
    entries:
      alpha:
        material: BOOK
        name: "<aqua><bold>Alpha</bold></aqua>"
        click:
          - "message: 你点击了 {entry-id}"

BUTTONS:
  "<":
    actions:
      all:
        - "page: previous showcase"
  ">":
    actions:
      all:
        - "page: next showcase"
```

## 兼容说明

- `Spigot`：支持
- `Paper`：支持
- `Folia`：支持，内部带 Folia-aware 调度抽象

## 构建

需要 Java 21。

```powershell
mvn package
```

产物输出到：

`target/amenu-1.0.0-SNAPSHOT-shaded.jar`
