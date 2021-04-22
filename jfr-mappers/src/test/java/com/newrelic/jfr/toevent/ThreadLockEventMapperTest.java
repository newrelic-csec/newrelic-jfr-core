package com.newrelic.jfr.toevent;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ThreadLockEventMapperTest {
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
    var threadName = "akita";
    var duration = Duration.of(21, MILLIS);
    var monitorClassName = "ooo";
    var expectedAttributes =
        new Attributes()
            .put("thread.name", threadName)
            .put("class", monitorClassName)
            .put("duration", duration.toMillis())
            .put("stackTrace", (String) null);
    var expectedEvent =
        new Event("JfrJavaMonitorWait", expectedAttributes, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var monitorClass = mock(RecordedClass.class);
    var eventThread = mock(RecordedThread.class);

    when(event.getStartTime()).thenReturn(startTime);
    when(event.getThread("eventThread")).thenReturn(eventThread);
    when(event.getDuration()).thenReturn(duration);
    when(event.getClass("monitorClass")).thenReturn(monitorClass);
    when(eventThread.getJavaName()).thenReturn(threadName);
    when(monitorClass.getName()).thenReturn(monitorClassName);

    var mapper = new ThreadLockEventMapper();

    var result = mapper.apply(event);
    assertEquals(expected, result);
  }

  @Test
  public void testApplyButDurationTooShort() throws Exception {
    var duration = Duration.of(19, MILLIS);
    var expected = List.of();

    var event = mock(RecordedEvent.class);

    when(event.getDuration()).thenReturn(duration);

    var mapper = new ThreadLockEventMapper();

    var result = mapper.apply(event);
    assertEquals(expected, result);
  }
}
