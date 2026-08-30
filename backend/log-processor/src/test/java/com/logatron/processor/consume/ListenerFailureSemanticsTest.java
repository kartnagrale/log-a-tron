package com.logatron.processor.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logatron.processor.TestFixtures;
import com.logatron.processor.observability.ProcessorMetrics;
import com.logatron.processor.store.ClickHouseStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ListenerFailureSemanticsTest {
    @Test
    void clickHouseFailureNeverAcknowledgesKafkaBatch() throws Exception {
        ProcessorPipeline pipeline = mock(ProcessorPipeline.class);
        ClickHouseStore store = mock(ClickHouseStore.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        var event = TestFixtures.canonical("persist before acknowledge");
        when(pipeline.process("wire")).thenReturn(ProcessingOutcome.event(event, false));
        doThrow(new IllegalStateException("ClickHouse persistence unavailable")).when(store).store(List.of(event));
        var listener = new RawLogBatchListener(pipeline, store, kafka, new ObjectMapper(), TestFixtures.properties(), new ProcessorMetrics(new SimpleMeterRegistry()));

        assertThatThrownBy(() -> listener.consume(List.of(new ConsumerRecord<>("raw", 0, 7L, "key", "wire")), acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ClickHouse");

        verify(acknowledgment, never()).acknowledge();
        verifyNoInteractions(kafka);
    }
}