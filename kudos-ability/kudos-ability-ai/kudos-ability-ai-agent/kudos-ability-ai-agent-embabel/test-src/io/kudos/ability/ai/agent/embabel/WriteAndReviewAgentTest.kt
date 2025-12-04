package io.kudos.ability.ai.agent.embabel

import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.unit.FakeOperationContext
import com.embabel.agent.testing.unit.FakePromptRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * 用 FakeOperationContext + FakePromptRunner 單元測 Embabel Agent 的示例。
 *
 * 注意：
 * - 不會真的調用 LLM，全部通過 expectResponse() 模擬
 * - 可以驗證 prompt 文本、temperature、toolGroups、LLM 調用次數等
 */
internal class WriteAndReviewAgentTest {

    /**
     * 用例 1：測試 craftStory
     * - 模擬 LLM 回傳一篇故事
     * - 驗證 prompt 是否包含關鍵詞
     * - 驗證 temperature（需和 agent 裡一致，目前是 0.2）
     */
    @Test
    fun `craftStory should build correct prompt and use creative temperature`() {
        // Arrange
        val agent = WriteAndReviewAgent(minWords = 200, maxWords = 400)
        val context = FakeOperationContext.create()
        val promptRunner = context.promptRunner() as FakePromptRunner

        // 這裡 LLM 回傳型別是 String（agent 裡 generateText 回傳 String）
        context.expectResponse("從前有個勇敢的騎士……")

        val input = UserInput(
            content = "請寫一個關於勇敢騎士的故事",
            timestamp = Instant.now()
        )

        // Act
        val story = agent.craftStory(input, context)

        // Assert - 業務邏輯
        Assertions.assertTrue(
            story.text.isNotBlank(),
            "返回的 Story 不應該為空字串"
        )

        // Assert - 驗證 prompt 文本
        val firstInvocation = promptRunner.llmInvocations.first()
        val prompt = firstInvocation.messages.first().content

        Assertions.assertTrue(
            prompt.contains("勇敢騎士"),
            "期望 prompt 中包含用戶輸入的主題關鍵詞"
        )
        Assertions.assertTrue(
            prompt.contains("開頭、發展、結尾"),
            "期望 prompt 中包含結構要求"
        )

        // Assert - 驗證溫度（和 agent 裡 .withTemperature(0.2) 保持一致）
        val temp = firstInvocation.interaction.llm.temperature!!
        Assertions.assertEquals(
            0.2,
            temp,
            0.01,
            "期望分類/結構型輸出使用較低溫度 0.2"
        )
    }

    /**
     * 用例 2：測試 reviewStory
     * - 模擬 LLM 回傳一段審稿評論
     * - 驗證 prompt 是否帶上故事內容 + 關鍵字
     * - 驗證整個過程只做了一次 LLM 呼叫
     */
    @Test
    fun `reviewStory should build review prompt and call LLM once`() {
        // Arrange
        val agent = WriteAndReviewAgent(minWords = 200, maxWords = 400)
        val context = FakeOperationContext.create()
        val promptRunner = context.promptRunner() as FakePromptRunner

        val input = UserInput(
            content = "請寫一個太空探險故事",
            timestamp = Instant.now()
        )
        val story = Story("宇航員站在月球表面，凝望著蔚藍的地球……")

        // 模擬 LLM 給出的評論文字（reviewStory 也用 generateText -> String）
        context.expectResponse("畫面感很強，但可以增加角色的心理變化。")

        // Act
        val reviewed = agent.reviewStory(input, story, context)

        // Assert - 業務結果
        Assertions.assertEquals(story, reviewed.story)
        Assertions.assertTrue(
            reviewed.review.contains("畫面感"),
            "期望評論中包含我們預設的關鍵字，證明假數據被用到了"
        )

        // Assert - LLM 調用情況
        Assertions.assertEquals(
            1,
            promptRunner.llmInvocations.size,
            "審稿流程預期只調用一次 LLM"
        )

        val invocation = promptRunner.llmInvocations.first()
        val prompt = invocation.messages.first().content

        println(prompt)

        Assertions.assertTrue(
            prompt.contains("太空探險"),
            "review 的 prompt 應該包含原始需求主題"
        )
        Assertions.assertTrue(
            prompt.contains("宇航員站在月球表面"),
            "review 的 prompt 應該包含故事正文"
        )
        Assertions.assertTrue(
            prompt.contains("簡短評論"),
            "review 的 prompt 應該明確告訴 LLM 只寫一段短評"
        )
    }

    /**
     * 用例 3：完整流程：先寫再審
     * - 模擬兩次 LLM 回應（第一次 Story 文本，第二次 Review 文本）
     * - 驗證兩次 LLM 調用的順序
     * - 可額外驗證兩次調用的 temperature 是否不同
     */
    @Test
    fun `craft and review should result in two ordered llm calls`() {
        // Arrange
        val agent = WriteAndReviewAgent(minWords = 100, maxWords = 300)
        val context = FakeOperationContext.create()
        val promptRunner = context.promptRunner() as FakePromptRunner

        val input = UserInput(
            content = "寫一個關於人工智慧的未來城市故事",
            timestamp = Instant.now()
        )

        val story = Story("在未來的城市裡，人類與人工智慧共同生活……")
        val reviewText = "故事構想有趣，但可多描寫人物情感。"

        // 注意：FakePromptRunner 會按順序消費這兩個 response
        // 第一次：寫故事 -> LLM 回傳 String，agent 再包成 Story
        context.expectResponse(story.text)
        // 第二次：審稿 -> LLM 回傳 String（評論）
        context.expectResponse(reviewText)

        // Act
        val writtenStory = agent.craftStory(input, context)
        val reviewedStory = agent.reviewStory(input, writtenStory, context)

        // Assert - 業務層面
        Assertions.assertEquals(story, writtenStory, "第一步應該得到預期的故事")
        Assertions.assertEquals(
            reviewText,
            reviewedStory.review,
            "第二步應該得到預期的評論"
        )

        // Assert - LLM 調用次數與順序
        val invocations = promptRunner.llmInvocations
        Assertions.assertEquals(
            2,
            invocations.size,
            "整個流程應該剛好兩次 LLM 調用（寫 + 審）"
        )

        val writerCall = invocations[0]
        val reviewerCall = invocations[1]

        // 驗證「寫故事」的 prompt
        Assertions.assertTrue(
            writerCall.messages.first().content.contains("未來城市"),
            "第一個 prompt 應該是寫故事的，包含 '未來城市'"
        )

        // 驗證「審稿」的 prompt
        Assertions.assertTrue(
            reviewerCall.messages.first().content.contains("請以專業編輯的身份"),
            "第二個 prompt 應該是審稿的，包含編輯身份說明"
        )

        // （可選）驗證兩次調用使用了不同的溫度
        val writerTemp = writerCall.interaction.llm.temperature
        val reviewerTemp = reviewerCall.interaction.llm.temperature

        Assertions.assertNotEquals(
            writerTemp,
            reviewerTemp,
            "示例中我們期望寫作和審稿使用不同的溫度配置"
        )
    }
}
