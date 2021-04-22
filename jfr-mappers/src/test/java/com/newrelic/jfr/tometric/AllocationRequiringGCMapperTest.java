package com.newrelic.jfr.tometric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class AllocationRequiringGCMapperTest {
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;

  @BeforeAll
  static void init() {
    recordedObjectValidatorsMockedStatic = Mockito.mockStatic(RecordedObjectValidators.class);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.hasField(
                    any(RecordedObject.class), anyString(), anyString()))
        .thenReturn(true);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.isRecordedObjectNull(
                    any(RecordedObject.class), anyString()))
        .thenReturn(false);
  }

  @AfterAll
  static void teardown() {
    recordedObjectValidatorsMockedStatic.close();
  }

  @Test
  void testMapper() {
    var recordedThread = mock(RecordedThread.class);
    var eventThread = "Thread-13";

    var recordedEvent = mock(RecordedEvent.class);
    var now = System.currentTimeMillis();
    var end = now + 1;
    var startTime = Instant.ofEpochMilli(now);
    var endTime = Instant.ofEpochMilli(end);
    var size = 32784L;

    var attr = new Attributes().put("thread.name", eventThread);
    var gauge = new Gauge("jfr.AllocationRequiringGC.allocationSize", size, now, attr);
    var expected = List.of(gauge);

    var testClass = new AllocationRequiringGCMapper();

    when(recordedThread.getJavaName()).thenReturn(eventThread);

    when(recordedEvent.getStartTime()).thenReturn(startTime);
    when(recordedEvent.getEndTime()).thenReturn(endTime);
    when(recordedEvent.getValue("eventThread")).thenReturn(recordedThread);
    when(recordedEvent.getLong("size")).thenReturn(size);

    var result = testClass.apply(recordedEvent);
    assertEquals(expected, result);
  }
}
