# AMenu

AMenu 是一个面向 Spigot、Paper 与 Folia 的配置化 Minecraft 菜单插件。  
它的核心定位是菜单引擎：代码只提供通用菜单能力，真正的菜单内容、按钮状态、动作流程和入口绑定都写在 `plugins/AMenu/menus/*.yml` 里。

AMenu 不会在新安装时自动启用默认菜单。插件首次启动只会释放 `config.yml` 和 `templates/` 示例文件，管理员需要把模板复制到 `menus/` 后才会成为可打开的活菜单。

## 可以做什么

- 用 `Shape` + `BUTTONS` 写箱子菜单，支持中文按钮名、反引号长 token、边框按钮、材质、头颅贴图、名称、lore、数量和发光。
- 修改 `config.yml` 或 `menus/**/*.yml` 后自动热重载，尽量不踢人、不重开服。
- 菜单内容变更只重绘当前 GUI，标题或行数变化才重开当前 GUI，配置错误时保留上一版已加载菜单。
- 按权限、条件、PlaceholderAPI 或其他插件变量切换按钮图标和动作。
- 用 `update: <ticks>` 定时刷新按钮，适合倒计时、冷却、开关状态和动态变量。
- 配置物品绑定和命令绑定，`/amenu give <bindingId>` 可生成带 PDC 的绑定物品。
- 执行动作链：打开菜单、返回、关闭、刷新、延迟、玩家命令、控制台命令、消息、标题、声音、聊天输入、分页和点券扣除。
- 支持分页列表、上一页、下一页、刷新、loading/empty/error 占位图标。
- 支持 provider 扩展动态列表数据，静态列表也可以直接写在菜单 YAML 里。

## 安装与快速开始

1. 将 `target/amenu-1.0.0-SNAPSHOT-shaded.jar` 放入服务器 `plugins/`。
2. 启动服务器，插件会生成：
   - `plugins/AMenu/config.yml`
   - `plugins/AMenu/templates/*.yml`
   - `plugins/AMenu/menus/`
3. 从 `templates/` 复制一个示例到 `menus/`，例如：

```powershell
Copy-Item "plugins/AMenu/templates/功能展示-基础布局.yml" "plugins/AMenu/menus/main.yml"
```

4. 游戏内打开：

```text
/amenu open main
```

菜单 ID 来自 `menus/` 下的相对路径。  
例如 `plugins/AMenu/menus/shop/vip.yml` 的菜单 ID 是 `shop/vip`，也可以在无歧义时用短名 `vip`。

## 命令

| 命令 | 说明 |
| --- | --- |
| `/amenu open <menuId>` | 打开指定菜单 |
| `/menu open <menuId>` | `/amenu` 的别名 |
| `/skinmenu open <menuId>` | `/amenu` 的别名 |
| `/amenu reload` | 手动重载配置与菜单 |
| `/amenu give <bindingId>` | 获取菜单里配置的物品绑定入口 |

`/amenu` 无参数只显示用法，不会自动打开默认菜单。

## 权限

| 权限 | 默认 | 说明 |
| --- | --- | --- |
| `amenu.open` | 配置项默认要求 | 使用 `/amenu open <menuId>` |
| `amenu.admin` | OP | 重载配置、获取绑定物品 |

可以在 `config.yml` 里修改 `command-open-permission`。设置为空可取消打开菜单的统一权限门槛。

## 菜单文件结构

一个最小菜单：

```yaml
title: "<green><bold>服务器菜单</bold></green>"

Shape:
  - "#########"
  - "#`回主城`#`帮助`#`关闭`##"
  - "#########"

BUTTONS:
  "#":
    material: GRAY_STAINED_GLASS_PANE
    name: " "

  "回主城":
    material: COMPASS
    name: "<aqua><bold>回主城</bold></aqua>"
    lore:
      - "<gray>点击执行 /spawn</gray>"
    click:
      - "close"
      - "delay: 1"
      - "player: spawn"

  "帮助":
    material: BOOK
    name: "<yellow><bold>帮助</bold></yellow>"
    click:
      - "message: <green>欢迎使用服务器菜单。</green>"

  "关闭":
    material: BARRIER
    name: "<red><bold>关闭</bold></red>"
    click:
      - "close"
```

`Shape` 每一行代表箱子 GUI 的一行。  
单字符可以直接作为按钮符号，中文或长名称请用反引号包起来，并在 `BUTTONS` 下写同名按钮。

## 动作系统

常用动作：

```yaml
click:
  - "open: shop"
  - "menu: shop"
  - "back"
  - "close"
  - "refresh"
  - "delay: 2"
  - "player: home"
  - "command: spawn"
  - "console: say {player} opened menu"
  - "message: <green>操作成功</green>"
  - "title: <green>成功</green>||<gray>动作已执行</gray>"
  - "sound: ENTITY_EXPERIENCE_ORB_PICKUP:1:1"
  - "prompt: rename"
  - "page: next rewards"
  - "page: previous rewards"
  - "page: refresh rewards"
  - "take-point: 100"
```

`player` 和 `command` 都表示让玩家执行命令。  
`console` 表示控制台执行命令。  
`take-point` 是点券扣除快捷动作，会尝试 `points take` 和 `playerpoints take`，适合 PlayerPoints 类点券环境。其他经济后端可以直接用 `console` 动作写自己的扣费命令。

点击类型分支：

```yaml
actions:
  all:
    - "message: <gray>任意点击都会执行</gray>"
  left:
    - "message: <green>左键</green>"
  right:
    - "message: <yellow>右键</yellow>"
  shift-left:
    - "message: <aqua>Shift 左键</aqua>"
  shift-right:
    - "message: <light_purple>Shift 右键</light_purple>"
```

## 条件与多状态按钮

按钮可通过 `conditions` 控制是否可解析，也可通过 `states` 或 `icons` 切换显示和动作。

```yaml
"礼包":
  update: 20
  material: CHEST
  name: "<yellow><bold>每日礼包</bold></yellow>"
  lore:
    - "<gray>状态来自 PlaceholderAPI 或其他插件变量。</gray>"
  click:
    - "message: <red>当前不可领取。</red>"
  states:
    available:
      conditions:
        - "check papi *%daily_reward_ready% = *true"
      material: EMERALD
      name: "<green><bold>每日礼包可领取</bold></green>"
      click:
        - "console: reward give %player_name% daily"
        - "message: <green>领取成功。</green>"
    cooldown:
      conditions:
        - "check papi *%daily_reward_ready% = *false"
      material: CLOCK
      name: "<red><bold>冷却中</bold></red>"
      lore:
        - "<gray>剩余：%daily_reward_time%</gray>"
```

支持的条件简写：

```yaml
conditions:
  - "perm: example.use"
  - "!perm: example.blocked"
  - "placeholder: binding-type=item"
  - "check papi *%playerpoints_points% >= *100"
```

## 条件购买示例

```yaml
"飞行权限":
  material: FEATHER
  name: "<aqua><bold>飞行权限</bold></aqua>"
  lore:
    - "<gray>价格：100 点券</gray>"
    - "<gray>余额：%playerpoints_points%</gray>"
  click:
    - condition: "check papi *%playerpoints_points% >= *100"
      actions:
        - "take-point: 100"
        - "console: lp user %player_name% permission set cmi.command.fly true"
        - "sound: ENTITY_PLAYER_LEVELUP:1:1"
        - "title: <green>购买成功</green>||<gray>飞行权限已解锁</gray>"
      deny:
        - "sound: BLOCK_ANVIL_DESTROY:1:1"
        - "message: <red>点券不足。</red>"
  icons:
    - condition: "perm: cmi.command.fly"
      material: EMERALD
      name: "<green><bold>飞行权限已拥有</bold></green>"
      click:
        - "player: fly"
```

## 输入流程

推荐使用稳定的 `CHAT` 输入。

```yaml
prompts:
  rename:
    type: CHAT
    timeout-seconds: 30
    start:
      - "<yellow>请输入 2-16 位名称，输入 cancel 可取消。</yellow>"
    validation:
      matches: "[A-Za-z0-9_]{2,16}"
    invalid:
      - "message: <red>格式错误。</red>"
    cancel:
      - "message: <yellow>已取消输入。</yellow>"
    submit:
      - "console: nick %player_name% {input}"
      - "message: <green>已提交：<white>{input}</white></green>"

BUTTONS:
  "改名":
    material: WRITABLE_BOOK
    name: "<aqua><bold>改名</bold></aqua>"
    click:
      - "prompt: rename"
```

也可以把输入直接写在按钮里：

```yaml
"输入说明":
  material: NAME_TAG
  name: "<aqua><bold>输入说明</bold></aqua>"
  input:
    type: CHAT
    submit:
      - "message: <green>你输入了 {input}</green>"
```

## 分页与动态列表

静态分页：

```yaml
Shape:
  - "#########"
  - "#`列表``列表``列表``列表``列表``列表``列表`#"
  - "#`上一页`###`刷新`###`下一页`#"

BUTTONS:
  "上一页":
    material: ARROW
    name: "<yellow>上一页</yellow>"
    click:
      - "page: previous rewards"
  "下一页":
    material: ARROW
    name: "<yellow>下一页</yellow>"
    click:
      - "page: next rewards"
  "刷新":
    material: SUNFLOWER
    name: "<green>刷新</green>"
    click:
      - "page: refresh rewards"

pages:
  rewards:
    symbol: "列表"
    loading:
      material: CLOCK
      name: "<yellow>加载中</yellow>"
    empty:
      material: BARRIER
      name: "<red>没有条目</red>"
    error:
      material: RED_STAINED_GLASS_PANE
      name: "<red>加载失败</red>"
    entries:
      daily:
        material: CHEST
        name: "<green>每日礼包</green>"
        click:
          - "message: <green>点击了 {entry-id}，第 {page}/{page-total} 页。</green>"
      weekly:
        material: ENDER_CHEST
        name: "<aqua>每周礼包</aqua>"
```

动态 provider：

```yaml
pages:
  state:
    symbol: "列表"
    provider:
      type: placeholder-state
      params:
        viewer: "%player_name%"
        points: "%playerpoints_points%"
      cache:
        ttl: 20
    update:
      interval: 20
    entries:
      driver:
        material: PAPER
        name: "<yellow>{viewer}</yellow>"
        lore:
          - "<gray>点券：{points}</gray>"
```

内置 provider：

- `entries`：使用 YAML 内的 `entries`。
- `placeholder-state`：把 `provider.params` 渲染后注入为页面占位符。

更多动态数据源可在代码里注册新的 `MenuDataProvider`，菜单 YAML 只需要声明 `provider.type` 和参数。

## 绑定入口

物品绑定：

```yaml
bindings:
  server-menu:
    type: ITEM
    material: COMPASS
    name: "<aqua><bold>服务器菜单</bold></aqua>"
    actions:
      - RIGHT_CLICK_AIR
      - RIGHT_CLICK_BLOCK
    placeholders:
      source: compass
```

获取绑定物品：

```text
/amenu give server-menu
```

命令绑定：

```yaml
bindings:
  quick-menu:
    type: COMMAND
    command: "quickmenu"
```

玩家执行 `/quickmenu` 时会打开该菜单。

## 热重载行为

- 监听 `plugins/AMenu/config.yml` 和 `plugins/AMenu/menus/**/*.yml`。
- 新增、修改、删除菜单文件都会自动处理。
- 按钮材质、名称、lore、动作、状态等变化会直接重绘已打开菜单。
- 菜单标题或行数变化会重新打开当前菜单。
- 当前正在看的菜单被删除时会关闭 GUI。
- YAML 解析失败时不会覆盖旧菜单，管理员会收到提示，控制台会输出错误。

## 默认资源

插件包内只包含 `templates/` 示例资源。  
这些文件不会自动成为可打开菜单，复制到 `plugins/AMenu/menus/` 后才会加载。

当前模板包括：

- `功能展示-基础布局.yml`
- `功能展示-动作执行.yml`
- `功能展示-权限条件.yml`
- `功能展示-分页数据.yml`
- `功能展示-聊天输入.yml`
- `功能展示-刷新绑定.yml`

## 兼容性

- Spigot：支持
- Paper：支持
- Folia：支持，内部使用 Folia-aware 调度抽象
- PlaceholderAPI：可选，用于变量渲染和条件判断
- PlayerPoints：可选，用于 `take-point`

## 构建

需要 Java 21。

PowerShell 示例：

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -q test
mvn -q -DskipTests package
```

构建产物：

```text
target/amenu-1.0.0-SNAPSHOT-shaded.jar
```

