package ca.vanzyl.ck8s.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class Ck8sDateTimeTaskTest {

    @Spy
    Ck8sDateTimeTask task;

    private static final ZonedDateTime NOW = ZonedDateTime.parse("2025-02-09T12:00:00.000+00:00");

    @Test
    public void test() {
        var t1 = task.parseIsoDateTime("2024-08-26T21:39:57.955Z");
        var t2 = task.parseIsoDateTime("2024-08-26T21:39:57Z");
        var t3 = task.parseIsoDateTime("2024-08-26T21:39:57.000Z");

        assertEquals(t2, t3);
        assertEquals(955, t1.getLong(ChronoField.MILLI_OF_SECOND));
        assertTrue(t1.compareTo(t2) > 0);
    }

    @Test
    public void testNowDateTime() {
        task.toIsoString(task.nowDateTime().plusDays(1).plus(Duration.of(10, ChronoUnit.NANOS)));
    }

    @Test
    public void isExpired_olderThan7Days_returnsTrue() {
        doReturn(NOW).when(task).nowDateTime();

        assertTrue(task.isExpired("2025-02-01T00:00:00.000+00:00", 7));
    }

    @Test
    public void isExpired_exactly7Days_returnsFalse() {
        doReturn(NOW).when(task).nowDateTime();

        assertFalse(task.isExpired("2025-02-02T12:00:00.000+00:00", 7));
    }

    @Test
    public void isExpired_lessThan7Days_returnsFalse() {
        doReturn(NOW).when(task).nowDateTime();

        assertFalse(task.isExpired("2025-02-05T00:00:00.000+00:00", 7));
    }

    @Test
    public void isExpired_differentTimezones_comparesCorrectly() {
        doReturn(NOW).when(task).nowDateTime();

        // 2025-02-01T03:00:00+03:00 = 2025-02-01T00:00:00Z — больше 7 дней назад
        assertTrue(task.isExpired("2025-02-01T03:00:00.000+03:00", 7));
    }

    @Test
    public void isExpired_zeroDays_beforeNow_returnsTrue() {
        doReturn(NOW).when(task).nowDateTime();

        assertTrue(task.isExpired("2025-02-09T11:00:00.000+00:00", 0));
    }
}
