package com.newrelic.jfr.toevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ValhallaVBCDetectorTest {
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
  void testApply() {
    var startTime = Instant.now();
    var threadName = "wonder";
    var expectedAttrs =
        new Attributes()
            .put("thread.name", threadName)
            .put("stackTrace", MethodSupport.empty())
            .put("boxClass", "java.lang.Integer");
    var expectedEvent = new Event("JfrValhallaVBCSync", expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var eventThread = mock(RecordedThread.class);
    var stack = mock(RecordedStackTrace.class);
    when(stack.getFrames()).thenReturn(List.of());
    var clazz = mock(RecordedClass.class);
    when(clazz.getName()).thenReturn("java.lang.Integer");

    when(event.getClass("boxClass")).thenReturn(clazz);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getStackTrace()).thenReturn(stack);
    when(event.getThread("eventThread")).thenReturn(eventThread);

    when(eventThread.getJavaName()).thenReturn(threadName);

    var mapper = new ValhallaVBCDetector();

    var result = mapper.apply(event);

    assertEquals(expected, result);
  }
}
