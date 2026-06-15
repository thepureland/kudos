# Kudos 疑似主代碼 Bug 清單（測試補全副產物）

> 生成日期：2026-06-15
> 來源：全模塊測試補全過程中各測試代理發現、未擅自修改主代碼而記錄的疑似缺陷。
> 覆核：4 個獨立 Explore 代理對照**當前 `main`**（含 `49e53285 fix(auth): SoD bypass via Criteria aliasing; isolate test datasources`）逐條對抗式覆核。
> 結論：候選 15 條 → **確認 12 條**、**非 bug 2 條**、**已修復 1 條**。
> 紀律：以下均**未改動主代碼**，僅記錄。對應測試按「實際行為」斷言或刻意不提交遷就性斷言。
>
> ---
> **更新 2026-06-15（修復）**：確認的 12 條（B1-B9、D1、C2、C2b）**已全部修復**並補上按「正確預期行為」斷言的回歸測試。
> 受影響三模塊 `:kudos-base`、`:kudos-tools`、`:kudos-ability-cache-common` 的完整測試套件已通過。
> 修復摘要見文末「修復記錄」。

## 確認清單（12 條）

| ID | 嚴重度 | 模塊 | 位置 | 一句話 |
|----|------|------|------|--------|
| B1 | 🔴 high | base | `security/Base36Kit.kt:336-342` | base-62 編解碼不可逆 |
| B2 | 🔴 high | base | `.../constraint/validator/SeriesValidator.kt:135,172,177` | BigDecimal 浮點精度誤判 |
| B3 | 🔴 high | base | `data/json/JsonKit.kt:314-324` | 嵌套 JSON null → 字符串 "null" |
| B5 | 🔴 high | base | `.../terminal/TerminalConstraintsCreator.kt:91-98` | @Valid 集合泛型解析用錯 API |
| B6 | 🔴 high | base | `lang/string/XCharSequence.kt:588,609,997,1015,1075` | 多方法委派與名稱相反 |
| B7 | 🔴 high | base | `.../validator/ConstraintsValidator.kt:214` + `ValidatorFactory.kt:531` | Range 作子約束拋 CCE |
| B8 | 🔴 high | base | `image/ImageKit.kt:315` | 硬編碼 xerces 但未聲明依賴 |
| D1 | 🔴 high | tools | `codegen/model/vo/Config.kt:59` | getTemplateInfo() 潛在 NPE |
| B4 | 🟡 medium | base | `.../validator/DateTimeValidator.kt:22-36` | 寬鬆解析放過非法日期 |
| B9 | 🟡 medium | base | `.../validator/CustomValidator.kt:39-41` | 方法查找非確定性 |
| C2 | 🟡 medium | cache | `batch/keyvalue/BatchCacheableAspect.kt:224-238` | 數組元素類型分派死代碼 |
| C2b | 🟡 medium | cache | `batch/hash/HashBatchCacheableByPrimaryAspect.kt:200-213` | 同 C2 |

---

## 高優先（🔴）

### B1 — `Base36Kit.diyToAscii` 範圍 off-by-one，base-62 編解碼不可逆
- **位置**：`kudos-base/src/io/kudos/base/security/Base36Kit.kt:336-342`
- **依據**：`asciiToDiy` 映射 `A-Z→10..35`、`a-z→36..61`；但 `diyToAscii` 反向用 `10..36→大寫`、`37..62→小寫`，邊界錯位。diy=36 解出 `[`(ASCII 91) 而非 `a`。
- **影響**：`capitalOnly=false`（base-62）多字符 `encrypt` 後 `decrypt` 不可逆，如 `DATA123ROUNDTRIP456 → DAMA123...`。默認大寫 base-36（`capitalOnly=true`）不受影響，故潛伏至今。
- **建議**：第 338 行 `in 10..36` → `in 10..35`；第 339 行 `in 37..62` → `in 36..61`。

### B2 — `SeriesValidator` 用 `BigDecimal(double)` + `equals` 比較
- **位置**：`kudos-base/.../constraint/validator/SeriesValidator.kt:135,172,177`
- **依據**：`prev + BigDecimal(step) == curr`，double 構造器有二進制誤差（0.1 無法精確），且 `BigDecimal.equals` 連 scale 比較，`"2" ≠ "2.0"`。
- **影響**：step=0.1 的合法數列 `[0,0.1,0.2,0.3]` 被判非法；混合精度輸入也誤判。KDoc 聲稱「高精度避免浮點誤差」與實現矛盾。
- **建議**：`BigDecimal(step)` → `BigDecimal.valueOf(step)`；`==` → `compareTo(...) == 0`。

### B3 — `JsonKit.unwrap` 把嵌套 JSON null 轉成字符串 `"null"`
- **位置**：`kudos-base/src/io/kudos/base/data/json/JsonKit.kt:314-324`
- **依據**：`when` 先匹配 `is JsonPrimitive`，而 kotlinx 的 `JsonNull` 是 `JsonPrimitive` 子類，落入該分支後 `content` 返回字符串 `"null"`；`JsonNull -> null` 成死代碼。
- **影響**：`getPropertyValue("{\"obj\":{\"n\":null}}","obj")` 得到 `obj["n"] == "null"`（字符串）而非 Kotlin null。
- **建議**：把 `JsonNull -> null` 分支移到 `is JsonPrimitive` 之前。

### B5 — `TerminalConstraintsCreator` 對 `@Valid` 集合用錯泛型 API
- **位置**：`kudos-base/.../terminal/TerminalConstraintsCreator.kt:91-98`
- **依據**：用 `prop.typeParameters[0/1]` 取元素類型，但 `KProperty.typeParameters` 返回屬性自身泛型參數（普通屬性為空），應為 `prop.returnType.arguments[i].type`。
- **影響**：`@get:Valid List<Item>` 級聯校驗拋 `IndexOutOfBoundsException`。生產暫未觸發（現無 bean 對 List/Map 用 @Valid）。
- **建議**：改用 `prop.returnType.arguments.getOrNull(i)?.type`。

### B6 — `XCharSequence` 多方法委派與名稱/KDoc 相反
- **位置**：`kudos-base/src/io/kudos/base/lang/string/XCharSequence.kt:588,609,997,1015,1075`
- **依據**：`isAlphanumeric`→`StringUtils.isAlpha`、`isAlphanumericSpace`→`isAlphanumeric`、`isNumericSpace`→`isNumeric`；`splitByCharacterType` 與其 CamelCase 版實現互換。
- **影響**：`"12".isAlphanumeric()` 返回 false（應 true）、`"1 2".isNumericSpace()` 返回 false（應 true）等。
- **建議**：各方法改委派到同名 Commons 方法。

### B7 — `Range` 作 `@Constraints` 子約束拋 `ClassCastException`
- **位置**：`kudos-base/.../validator/ConstraintsValidator.kt:214` + `.../support/ValidatorFactory.kt:531-538`
- **依據**：`ValidatorFactory` 正確把 Range 展開為 Min/Max 兩個 validator，但 `ConstraintsValidator.doValidate` 用原始 Range 注解去 `initialize` Min/Max validator。`ConstraintsTest.kt:57-60` 已有 NOTE 記錄。
- **影響**：`@Constraints(range=Range(...))` 校驗期拋 CCE。
- **建議**：`doValidate` 對子約束按展開後的具體注解類型初始化對應 validator。

### B8 — `ImageKit.renderSvgToImage` 硬編碼 xerces 但未聲明依賴
- **位置**：`kudos-base/src/io/kudos/base/image/ImageKit.kt:315`
- **依據**：`SAXSVGDocumentFactory("org.apache.xerces.parsers.SAXParser")`，而 `build.gradle.kts` 只聲明 batik-dom/batik-bridge，未引 xerces。
- **影響**：調用必拋 `ClassNotFoundException`，整個 SVG 渲染路徑死代碼。
- **建議**：顯式加 `org.apache.xerces:xercesImpl`，或改用自動探測 SAX 解析器（不硬編碼）。

### D1 — `Config.getTemplateInfo()` 潛在 NPE
- **位置**：`kudos-tools/src/io/kudos/tools/codegen/model/vo/Config.kt:59`
- **依據**：`templateInfo.get().selectedItem`，`SimpleObjectProperty` 未初始化時 `.get()` 為 null。其餘 String getter 同模式但總會經 setter 賦值；`templateInfo` 在 UI 綁定前訪問會 NPE。
- **影響**：新建 `Config` 或綁定 SelectionModel 之前調用 `getTemplateInfo()` 拋 NPE。
- **建議**：`templateInfo.get()?.selectedItem`（返回可空）並更新調用點，或保證綁定後再訪問。

---

## 中優先（🟡）

### B4 — `DateTimeValidator` 寬鬆解析放過非法日期
- **位置**：`kudos-base/.../validator/DateTimeValidator.kt:22-36`
- **依據**：`SimpleDateFormat` 未 `setLenient(false)`，僅做長度校驗；`"2020-99-99"`（長度匹配）被進位解析為合法。
- **影響**：`2020-13-01`、`2020-02-30` 等非法日期被接受。
- **建議**：創建 `SimpleDateFormat` 後 `setLenient(false)`。

### B9 — `CustomValidator.validate` 方法查找非確定性
- **位置**：`kudos-base/.../validator/CustomValidator.kt:39-41`
- **依據**：`javaClass.methods.firstOrNull { name=="validate" && parameterCount==1 }`，`Class.methods` 順序無 JLS 保證。
- **影響**：實現類若有多個單參 `validate` 重載，選中項跨 JVM/字節碼版本不確定。
- **建議**：按參數類型匹配 `IBeanValidator` 泛型參數選唯一 `validate(Bean)`，或要求簽名唯一。

### C2 / C2b — 批量緩存切面數組元素類型分派為死代碼
- **位置**：`BatchCacheableAspect.kt:224-238`、`HashBatchCacheableByPrimaryAspect.kt:200-213`（cache-common `batch/`）
- **依據**：`when (clazz.kotlin) { Array<String>::class -> ...; Array<Int>::class -> ... }`，Kotlin 類型擦除使所有 `Array<*>::class` 都等於 `kotlin.Array`，只有首個 `Array<String>` 分支生效，其餘死代碼。
- **影響**：方法參數為 `Array<Int>/Array<Long>/...`（非 `Array<String>`）時會被當成 `String[]` 處理，CCE 或靜默失敗——`@BatchCacheable` 實際只支持 `Array<String>`。
- **建議**：改用已推導的 `elemType`（元素 KClass）或 `componentType` 分派，而非枚舉 `Array<X>::class`。

---

## 附錄：覆核後排除（3 條）

| ID | 結論 | 位置 | 理由 |
|----|------|------|------|
| C1 | ✅ 非 bug | `cache-common/.../aop/keyvalue/TenantCacheable.kt` 等 | `@TenantCacheable/Evict/Put` 為**自定義注解**，由自定義 AOP 切面（`DistributedCacheGuardAspect`/`TenantCachingAspect`）經 `AnnotationUtils.findAnnotation` 直接讀取，不走 Spring `@EnableCaching`；`@AliasFor` 僅作 IDE 文檔提示。現有路徑正常。 |
| D2 | ✅ 非 bug | `test-container/kit/TestContainerKit.kt:342` | `execInContainer(GenericContainer)` 為 `internal`（非 private），且有調用方 `TestContainerKitDockerIT.kt:120`，非死代碼。 |
| D3 | ✅ 已修復 | `ms-auth-core/.../AuthRoleUserService.kt:108-117` | `if(tenantId != null)` SoD 守衛**並非死分支**：DB 實體 `tenantId` 運行時可能為 null，該守衛防止 SoD 繞過，是必要的；`49e53285` 已確保 tenantId 始終提取與校驗。 |

---

## 修復建議優先級

1. **B1 Base36Kit**、**B3 JsonKit**、**B7 Range/Constraints**：影響數據正確性/拋異常，且修法明確（小改動），建議優先。
2. **B2 SeriesValidator**、**B6 XCharSequence**、**B4 DateTimeValidator**：校驗/字符串語義錯誤，潛伏中，建議盡快修。
3. **B5、B8、D1、C2/C2b**：當前生產路徑暫未觸發或為工具類，可排期。
4. 每條修復都應補一個按**正確預期行為**斷言的回歸測試（本次因不改主代碼，這些測試尚未提交）。

---

## 修復記錄（2026-06-15）

所有 12 條確認 bug 均已修復，並補上按正確預期行為斷言的回歸測試。三模塊測試套件全綠。

| ID | 修復 | 回歸測試 |
|----|------|----------|
| B1 | `diyToAscii` 範圍 `10..36/37..62` → `10..35/36..61`，與 `asciiToDiy` 互逆 | `Base36KitTest.multiChar_encryptDecrypt_capitalOnlyFalse_isReversible`（注：算法域為 base-62 字母數字，非字母數字字符不在域內） |
| B2 | `BigDecimal(double)` → `BigDecimal.valueOf`；`==/equals` → `compareTo()==0`（含 EQ 的 scale 不敏感比較） | `SeriesValidatorTest` |
| B3 | `unwrap` 的 `JsonNull -> null` 分支移到 `is JsonPrimitive` 之前 | `JsonKitMoreTest`（更新原斷言為正確行為） |
| B4 | `SimpleDateFormat.isLenient = false` | `DateTimeValidatorTest` |
| B5 | `prop.typeParameters[i]` → `prop.returnType.arguments.getOrNull(i)?.type` | `TerminalConstraintsCreatorBranchesTest` |
| B6 | 5 個方法委派改為同名 Commons 方法；`splitByCharacterType` 兩變體解交換 | `XCharSequenceExtTest`（更正鎖死錯誤行為的斷言） |
| B7 | `ConstraintsValidator` 改用 `ValidatorFactory.getValidatorsWithAnnotations` 拿到展開後的具體註解去 `initialize` | `ConstraintsTest`（NOTE 轉為正式斷言） |
| B8 | `SAXSVGDocumentFactory("org.apache.xerces...")` → `XMLResourceDescriptor.getXMLParserClassName()`（回退 JDK JAXP，無需新依賴） | `ImageKitMoreTest.renderSvgToImage_tinySvg_...` |
| B9 | 跳過 bridge/synthetic 方法 + 按參數類型匹配，確定性選 `validate` | `CustomValidatorMethodSelectionTest` |
| D1 | `getTemplateInfo()` 返回可空 + `?.`，調用點以 `!!` 斷言前置條件 | `ConfigTest.getTemplateInfoReturnsNullWhenNoModelBound` |
| C2 / C2b | 不再枚舉 `Array<X>::class`；改以 `Class.isArray` + `java.lang.reflect.Array.newInstance(elemType.javaObjectType, n)` 按真實元素類型建陣列 | `BatchCacheableArrayDispatchTest`、`HashBatchCacheableArrayDispatchTest` |
