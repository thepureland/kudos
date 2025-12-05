package io.kudos.ability.ai.agent.embabel

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.domain.io.UserInput
import com.embabel.common.ai.model.LlmOptions

@Agent(
    name = "write-and-review-agent",
    description = "根據用戶輸入寫故事並審稿的示例 Agent",
    version = "1.0.0"
)
class WriteAndReviewAgent(
    private val minWords: Int,
    private val maxWords: Int
) {

    /**
     * 第一步：根據用戶輸入寫一篇故事
     */
    @Action(description = "根據用戶輸入寫一篇故事")
    fun craftStory(input: UserInput, context: OperationContext): Story {
        val llm = LlmOptions
            .withDefaultLlm()        // 或 withModel("gpt-4.1-mini") 等
            .withTemperature(0.2)    // 偏保守，做分类

        val promptRunner = context.promptRunner()
            .withLlm(llm)
            .withSystemPrompt("你是一位有創意的中文小說家。")

        val storyText: String = promptRunner.generateText(
            """
            請用中文寫一篇小故事，主題是：${input.content}
            要求：
            - 字數大約介於 $minWords 到 $maxWords 字
            - 有清楚的開頭、發展、結尾
            """.trimIndent()
        )

        return Story(storyText)
    }

    /**
     * 第二步：審閱故事並給出評論（最終目標）
     */
    @AchievesGoal(
        description = "為使用者對指定故事進行專業審稿並生成簡短評論"
    )
    @Action(description = "審閱故事並給出評論")
    fun reviewStory(
        input: UserInput,
        story: Story,
        context: OperationContext
    ): ReviewedStory {

        val llm = LlmOptions
            .withDefaultLlm()        // 或 withModel("gpt-4.1-mini")
            .withTemperature(0.7)    // 偏活泼一点，用来生成回答
//        val llmOptions = LlmOptions.fromModel(OpenAiModels.GPT_41)

        val promptRunner = context.promptRunner()
            .withLlm(llm)

        val review: String = promptRunner.generateText(
            """
            下面是一篇故事，請以專業編輯的身份寫一段簡短評論（80 字以內）：
            - 要簡要概括故事優點
            - 點出 1–2 個可以改進的地方

            題目需求：${input.content}
            故事內容：
            ${story.text}
            """.trimIndent()
        )

        return ReviewedStory(story, review)
    }
}

/**
 * 故事內容
 */
data class Story(
    val text: String
)

/**
 * 審稿結果：包含原故事 + 評論文字
 */
data class ReviewedStory(
    val story: Story,
    val review: String
)


