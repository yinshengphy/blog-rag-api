package cn.yinsheng.blog.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssistantSessionMemoryTest {
  @Test
  void shouldKeepOnlyEphemeralRecentMessages() {
    AssistantSessionMemory memory = new AssistantSessionMemory();
    memory.remember("s1", "question", "answer");

    assertThat(memory.history("s1")).hasSize(2);
    memory.clear("s1");
    assertThat(memory.history("s1")).isEmpty();
  }
}
