# 日记应用 (Dairy App) 项目说明

这是一个基于 Jetpack Compose 和 Room 开发的现代化 Android 日记应用。它允许用户以“事件”为单位，记录包含图片和文字的日记条目。

## 核心概念

本项目的核心数据模型由三个主要实体构成，它们之间是一种层级关系：

1.  **事件 (Event / DiaryEvent)**
    *   **定义**: 这是最高层级的容器，代表一个独立的、可聚合的日记集合。例如，一次“欧洲之旅”、一个“工作项目”或就是“日常生活”。

2.  **日记页 (Diary Page)**
    *   **定义**: 在一个“事件”内部，所有日记条目会根据其标题（caption）被自动归类到不同的“日记页”中。这是一种虚拟的、动态的分类方式。

3.  **日记条目 (Diary Entry)**
    *   **定义**: 这是最基础的数据单元，代表一次具体的记录。

**关系**: `事件 (Event)` 包含多个 `日记页 (Diary Page)`，而每个 `日记页 (Diary Page)` 实际上是拥有相同标题 (`caption`) 的 `日记条目 (Diary Entry)` 的集合。

## 核心文件详解

下面是项目中各个关键文件的详细职责说明和重要函数介绍。

### 数据层 (Data Layer)

数据层负责应用所有的数据操作和持久化存储，是整个应用的基石。

#### `data/DiaryDb.kt`
*   **职责**: 定义 Room 数据库。它声明了数据库包含哪些表 (Entities) 以及提供哪个 DAO (Data Access Object) 来访问这些表。

#### `data/DiaryDao.kt`
*   **职责**: 定义所有与数据库交互的接口。Room 库会根据这个接口中的注解自动生成所有 SQL 操作的实现。
*   **重要函数**:
    *   `getEvents(): Flow<List<DiaryEvent>>`: 获取所有“事件”的列表，并以 `Flow` 形式返回，实现数据的响应式更新。
    *   `getEvent(id: Long): Flow<DiaryEvent?>`: 根据 ID 获取单个“事件”。
    *   `getEntriesForEvent(eventId: Long): Flow<List<DiaryEntry>>`: 获取特定“事件”下的所有“日记条目”。
    *   `upsertEvent(event: DiaryEvent)`: 插入一个新事件或更新一个已存在的事件。
    *   `deleteEvent(event: DiaryEvent)`: 删除一个指定的事件。
    *   `deleteEntriesByEventId(eventId: Long)`: 删除特定事件下的所有条目。
    *   `(其他针对 DiaryEntry 的增删改查函数)...`

#### `data/DiaryRepository.kt`
*   **职责**: 作为**单一数据源 (Single Source of Truth)**，它封装了对 `DiaryDao` 的所有调用。所有 ViewModel 都只和 Repository 交互，隐藏了数据持久化的具体细节。
*   **重要函数**: Repository 中的函数基本与 `DiaryDao` 一一对应，为 ViewModel 提供了清晰、统一的数据访问 API。
    *   `getEvents()`
    *   `addEvent(eventName, coverImageUri)`
    *   `deleteEventAndEntries(event: DiaryEvent)`: 这是一个典型的业务逻辑封装，它在一个事务中同时删除了事件和其下的所有条目，保证了数据的一致性。

### 业务逻辑层 (ViewModels)

ViewModels 负责处理 UI 相关的业务逻辑和状态管理，是连接数据层和 UI 层的桥梁。

#### `diary/DiaryEventListViewModel.kt`
*   **职责**: 管理主屏幕（事件列表）的状态和逻辑。
*   **重要函数**:
    *   `addEvent(eventName, coverImageUri)`: 调用 Repository 创建并保存一个新的事件。
    *   `deleteEvent(event)`: 调用 Repository 删除一个指定的事件及其所有相关数据。

#### `diary/DiaryPageViewModel.kt`
*   **职责**: 管理特定“事件”内部的数据状态，包括获取其下的所有日记条目，并动态生成“日记页”列表。
*   **关键逻辑**: 它会从 Repository 获取原始的 `DiaryEntry` 列表，然后通过 `groupingBy` 和 `eachCount` 等操作，动态计算出所有不重复的“日记页”名称 (`diaryPageNames`)，并暴露给 UI 层。
*   **重要函数**:
    *   `deletePage(pageName)`: 根据页面名称（即条目标题），调用 Repository 删除所有相关的日记条目。
    *   `deleteEntry(entry)`: 删除一条单独的日记条目。

#### `diary/PhotoCaptionViewModel.kt`
*   **职责**: 处理“创建新日记条目”页面的逻辑。
*   **重要函数**:
    *   `saveDiaryEntry(imageUris, caption)`: 接收用户选择的图片和输入的标题，构建一个 `DiaryEntry` 对象，并调用 Repository 将其存入数据库。

### UI 层 (Screens)

UI 层使用 Jetpack Compose 构建，负责界面的展示和用户交互的响应。

#### `features/DiaryEventListScreen.kt`
*   **职责**: 应用的主屏幕，使用 `LazyVerticalGrid` 以网格形式展示所有“事件”。
*   **核心组件**:
    *   `Scaffold`: 构建了页面的基本结构，包含 `TopAppBar` 和一个 `FloatingActionButton` (FAB)。
    *   `FloatingActionButton`: “+”号按钮，点击后触发创建新事件的流程。
    *   `DiaryEventCard`: 自定义的卡片组件，用于显示单个事件的封面和名称，并处理点击和删除事件。

#### `features/DiaryPageListScreen.kt`
*   **职责**: 展示特定“事件”内所有“日记页”的列表。
*   **核心组件**:
    *   `LazyColumn`: 以列表形式展示所有“日记页”的名称。
    *   `ListItem`: 列表中的每一项，点击后会导航到该页的详情 (`DiaryEntryListScreen`)。
    *   `FloatingActionButton`: “+”号按钮，点击后导航到 `PhotoCaptionScreen` 以创建新的日记条目。

#### `features/DiaryEntryListScreen.kt` (在旧版README中被称为`DiaryPageScreen`)
*   **职责**: 展示属于同一个“日记页”的所有“日记条目”。
*   **核心组件**:
    *   `LazyColumn`: 以列表形式展示所有“日记条目”。
    *   `DiaryEntryCard`: 自定义的卡片组件，用于显示单条日记的图片和标题，并处理删除操作。

#### `features/PhotoCaptionScreen.kt`
*   **职责**: 创建新日记条目的界面。
*   **核心组件**:
    *   `rememberLauncherForActivityResult`: 用于启动系统的图片选择器，并获取用户选择的图片 URI。
    *   `TextField`: 供用户输入日记标题 (`caption`)。
    *   `Button`: “保存”按钮，点击后调用 `PhotoCaptionViewModel` 中的 `saveDiaryEntry` 函数。

#### `features/NavGraph.kt`
*   **职责**: 使用 `NavHost` 定义了整个应用的导航图。它将每个 `Screen` 的路由（Route）与对应的 Composable 函数（UI界面）关联起来，并定义了页面间的参数传递。

#### `MainActivity.kt`
*   **职责**: 应用的主入口 Activity。它的核心工作是设置 `NavHost`，启动 Jetpack Compose 的 UI 渲染流程。