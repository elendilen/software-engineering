# 图片日记 — 前端代码详解

本文件聚焦说明 Android 前端（位于 `app/` 模块）中的每个重要源码文件的职责、重要数据段与关键函数，帮助开发者快速定位与理解项目实现。

说明范围：仅覆盖前端（Jetpack Compose + Room + Retrofit）源码；后端接口以 `/v1/generate-caption` 为契约。文档旨在清晰、可操作地描述每个文件的用途与边界逻辑。

---

## 目录（按模块分组）

- app/src/main/java/com/example/dairyApp/
  - data/  —— 网络与本地 DB 相关
  - diary/ —— Room 实体、DAO、仓库、与事件/页相关 ViewModel/Screen
  - features/ —— Compose 界面与页面级 ViewModel
  - utils/ —— 辅助工具（分享等）
  - MainActivity.kt、Screen.kt（导航）

---

## 快速概览（工作流）

1. 用户在客户端选择 1~9 张图片（系统 Photo Picker），可填入可选 prompt。
2. `PhotoCaptionViewModel` 调用 `CaptionRepository.generateCaptionForImages` 上传图片与 prompt 到后端 `/v1/generate-caption`，并接收返回的 `caption`。
3. 生成的文案可在 `PhotoCaptionScreen` 编辑并保存为 `DiaryEntry`（Room）。
4. 分享时使用 `ShareUtils.shareImagesWithCaption`：把图片复制到 `cacheDir/shared_images`，通过 `FileProvider` 生成 `content://` URI 并发起系统分享 Intent；同时把文案复制到剪贴板以兼容目标应用忽略 EXTRA_TEXT 的情形。

---

## data 模块（网络 + 数据库）

文件：`app/src/main/java/com/example/dairyApp/data/`。

- `ApiService.kt`
  - 作用：Retrofit 接口契约。
  - 关键：
    - `@Multipart @POST("v1/generate-caption") suspend fun generateCaption(@Part images: List<MultipartBody.Part>, @Part("prompt") prompt: RequestBody?): CaptionResponse`
    - `CaptionResponse(val caption: String)` — 后端返回字段（简短文案）。

- `RetrofitClient.kt`
  - 作用：创建 Retrofit 实例和 `ApiService`。
  - 关键字段：`baseUrl`（默认示例为 `http://10.21.207.162:8000/`，需根据本地环境调整）。

- `CaptionRepository.kt`
  - 作用：封装对后端 `/v1/generate-caption` 的调用并提供本地兜底文案。
  - 关键流程与函数：
    - `suspend fun generateCaptionForImages(imageUris: List<Uri>, prompt: String? = null): String` — 对外主方法。
    - `private suspend fun callBackendApi(imageUris: List<Uri>, prompt: String?): String` — 具体实现：
      - 将每个 `Uri` 用 `contentResolver.openInputStream(uri)` 读为 bytes，封装为 `MultipartBody.Part.createFormData("images", "image.jpg", requestBody)`（字段名 `images`）。
      - prompt 转为 `text/plain` 的 RequestBody 并作为 `@Part("prompt")` 发送。
      - 调用 `RetrofitClient.apiService.generateCaption(...)`，解析 `response.caption`；若为空或异常返回 `FALLBACK_CAPTION = "请在此处编辑生成的文字"`。
    - 错误处理：所有异常被捕获并返回兜底文案，避免抛出到 UI 层。

- `AppDatabase.kt`
  - 作用：Room 数据库单例，注册实体与 TypeConverter。
  - 关键：`@Database(entities = [DiaryEntry::class, DiaryEvent::class], version = 1)`，`getDatabase(context)` 提供单例实例。

- `StringListConverter.kt`
  - 作用：Room 的 List<String> 与 JSON 字符串互转（使用 Gson）。
  - 关键函数：
    - `@TypeConverter fun fromString(value: String?): List<String>?`
    - `@TypeConverter fun fromList(list: List<String>?): String?`

---

## diary 模块（实体、DAO、Repository、事件/页相关逻辑）

路径：`app/src/main/java/com/example/dairyApp/diary/`

- `DiaryEntry.kt`（Room 实体）
  - 表：`diary_entries`
  - 字段（重要）：
    - `id: String`（主键）
    - `imageUris: List<String>`（图片 URI 列表，以字符串形式持久化）
    - `caption: String`
    - `timestamp: Long`
    - `diaryPageName: String?`（列名 groupName，在 @ColumnInfo 中指定）
    - `eventId: String?`（列名 tripId）

- `DiaryEvent.kt`（Room 实体）
  - 表：`diary_trips`（历史名未改动）
  - 字段：`id, name, entryIds: List<String>, coverImageUri, timestamp`

- `DiaryDao.kt`（DAO）
  - 作用：数据库 CRUD 方法。
  - 关键查询：
    - `insertEntry/updateEntry/deleteEntry`
    - `getEntryById(id): Flow<DiaryEntry?>`
    - `getEntriesForEvent(eventId): Flow<List<DiaryEntry>>`
    - `getEntriesForEventAndPage(eventId, pageName): Flow<List<DiaryEntry>>`
    - `getPagesForEvent(eventId): Flow<List<String>>`（DISTINCT groupName）
    - `insertEvent/updateEvent/deleteEvent/getEventById/getAllEvents`

- `DiaryRepository.kt`（仓库）
  - 作用：对 DAO 的包装，提供业务级方法并保证操作一致性。
  - 关键方法：
    - `insertEntry/updateEntry/deleteEntry`（直接委派给 DAO）
    - `deleteEvent(event)`：先删除该事件下所有条目（`deleteEntriesByEventId`），再删除 event 自身（保证一致性）。
    - `getPagesForEvent(eventId): Flow<List<String>>` 用于 UI 候选。

- `EventViewModel.kt`（事件创建/列表）
  - 作用：管理 `DiaryEvent` 列表与创建/删除操作，供事件列表屏使用。
  - 关键字段/方法：
    - `val events: StateFlow<List<DiaryEvent>> = diaryRepository.getAllEvents().stateIn(...)`
    - `fun createEvent(name: String)`：创建 `DiaryEvent`（使用 UUID 作为 id）。
    - `fun delete(event: DiaryEvent)`：删除并清理其条目。

- `DiaryViewModel.kt`（首页）
  - 作用：构建 `DiaryHomeUiState`，供 `DiaryHomeScreen` 显示。
  - 关键数据：`diaryHomeUiState: StateFlow<DiaryHomeUiState>`（包含 events、isLoading、error）。

- `DiaryPageViewModel.kt`（事件内页面列表）
  - 作用：为某个 `eventId` 聚合 `DiaryEvent` 与该事件下的所有 `DiaryEntry`，并抽取 distinct 的 page 名称。
  - 关键：`val uiState: StateFlow<DiaryPageListUiState>`（包含 event、diaryPageNames、entries）。
  - 方法：`deletePage(pageName)` 会调用 `diaryRepository.deletePage(eventId, pageName)`。

- `DiaryEntryListViewModel.kt`（页内条目列表）
  - 作用：通过 `getEntriesForEventAndPage(eventId, diaryPageName)` 提供 `entries: StateFlow<List<DiaryEntry>>`。
  - 方法：`delete(entry)` 删除条目。

---

## features（UI 层：Compose 屏幕与 ViewModel）

- `PhotoCaptionViewModel.kt`
  - 作用：PhotoCaption 页面主要状态与流程管理（选图、prompt、调用生成、保存、编辑）。
  - 重要状态（PhotoCaptionUiState）：
    - `selectedPhotoUris: List<Uri>`
    - `generatedCaption: String`
    - `isLoading: Boolean`
    - `userPrompt: String`（用户自定义提示）
    - `availableEvents: List<DiaryEvent>`, `availablePages: List<String>`
  - 关键方法：
    - `onPhotosSelected(uris: List<Uri>)`：尝试 `takePersistableUriPermission` 并更新 state。
    - `generateCaption()`：调用 `CaptionRepository.generateCaptionForImages(...)` 并将结果写回 `generatedCaption`。
    - `saveDiaryEntry(diaryPageName: String?, eventId: String?)`：创建或更新 `DiaryEntry`，并在新建时将 id 添加到对应 `DiaryEvent.entryIds`。
    - `provideFactory(...)`：用于在导航中以 `SavedStateHandle` 创建 ViewModel 实例并传入初始参数（eventId、diaryPageName、entryId）。

- `PhotoCaptionScreen.kt`
  - 作用：图片选择、生成文案、编辑、保存与分享的 UI。
  - 重要交互点与控件：
    - 使用 `ActivityResultContracts.PickMultipleVisualMedia()` 打开系统 Photo Picker；限制展示最多 9 张图。
    - 展示规则：单图大图；4 图为 2 列网格；其它为 3 列网格（仿朋友圈）。
    - 输入框：`OutlinedTextField` 用于用户 prompt（单行）与编辑生成文案（多行）。
    - 按钮：生成 / 重新生成（`viewModel.generateCaption()`）、保存（`viewModel.saveDiaryEntry(...)`）、分享（`ShareUtils.shareImagesWithCaption(...)`）。
    - 反馈：使用 `SnackbarHost` 展示保存/分享结果。

- `DiaryPageListScreen.kt`、`DiaryEntryListScreen.kt`、`EventListScreen.kt`、`CreateEventScreen.kt`、`DiaryHomeScreen.kt`
  - 作用：分别展示事件列表、事件内的日记页、页内条目、创建事件对话框与首页布局。
  - 关键 UI 行为：
    - `EventListScreen`：展示 `EventViewModel.events`，支持删除事件（弹确认对话框）。
    - `DiaryPageListScreen`：基于 `DiaryPageViewModel` 列出 page 名称并支持新建页面跳转。
    - `DiaryEntryListScreen`：展示页内条目卡片，条目点击进入编辑（带 entryId），支持删除与确认对话。 

---

## utils

- `ShareUtils.kt`（详述）
  - 主要职责：把任意来源的 `Uri` 内容复制到应用缓存 `cacheDir/shared_images/`，通过 `FileProvider` 生成可被外部应用读取的 `content://` URI，然后发起系统分享。为提高兼容性，函数还会把 caption 自动复制到剪贴板以便用户在目标应用粘贴（目标应用可能会忽略 Intent 的 EXTRA_TEXT）。
  - 关键函数：`shareImagesWithCaption(context: Context, sourceUris: List<Uri>, caption: String)`。
  - 错误回退：若文件复制或分享异常，会尝试只分享文本（ACTION_SEND text/plain）。

---

## 导航与入口

- `MainActivity.kt`：设置 Compose 主题并注册 `NavHost`。
  - 路由登记（示例）：
    - `Screen.EventList`（起始页）
    - `Screen.CreateEvent`（创建事件）
    - `Screen.DiaryPageList`（事件内页列表，参数 eventId）
    - `Screen.DiaryEntryList`（页内条目列表，参数 eventId, diaryPageName）
    - `Screen.PhotoCaption`（选择/生成/编辑条目，参数可选 eventId/diaryPageName/entryId）

- `Screen.kt`：集中管理路由常量与 `createRoute` 帮助函数（构造带参数的路由字符串）。

---

## 重要设计与兼容性说明（要点）

- 图片上限：客户端在 UI 层对展示限制为最多 9 张图片；后端 `POST /v1/generate-caption` 也期望不超过 9 张。
- 分享兼容性：部分第三方应用在带图片的分享中会忽略 Intent 的文本字段（EXTRA_TEXT），因此本项目把 caption 复制到剪贴板作为补偿方案；更稳妥的方法是把文字渲染到图片上（回退方案，当前仓库未实现）。
- 权限与 Uri：`PhotoCaptionViewModel.onPhotosSelected` 会尝试调用 `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` 以保留长期访问（能在某些来源上避免重启后无法访问的问题）。
- DB 存储：`imageUris` 以字符串列表形式持久化（通过 `StringListConverter` 将 List<String> 与 JSON 字符串互转）。

---

## 快速定位（文件清单）

- UI / Navigation
  - `MainActivity.kt`, `Screen.kt`
- features (Compose + ViewModel)
  - `features/PhotoCaptionScreen.kt`, `features/PhotoCaptionViewModel.kt`
  - `features/EventListScreen.kt`, `features/CreateEventScreen.kt`
  - `features/DiaryPageListScreen.kt`, `features/DiaryEntryListScreen.kt`
- diary (数据层 + screens)
  - `diary/DiaryEntry.kt`, `diary/DiaryEvent.kt`, `diary/DiaryDao.kt`, `diary/DiaryRepository.kt`
  - `diary/DiaryHomeScreen.kt`, `diary/DiaryPageScreen.kt`, `diary/DiaryPageViewModel.kt`, `diary/DiaryEntryListViewModel.kt`, `diary/EventViewModel.kt`
- data (network + db)
  - `data/ApiService.kt`, `data/RetrofitClient.kt`, `data/CaptionRepository.kt`, `data/AppDatabase.kt`, `data/StringListConverter.kt`
- utils
  - `utils/ShareUtils.kt`

---

如果你希望我把 README 再精化为：
- 带具体函数签名与示例调用（curl / Retrofit） 的 API 文档；或
- 在 `ShareUtils` 中实现把 caption 渲染到图片并分享的回退实现（代码补丁）；
请回复你要的选项（例如：A = 增加 curl/Retrofit 示例，B = 实现渲染到图片的回退）。

