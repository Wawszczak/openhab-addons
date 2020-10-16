/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.icalendar.internal.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;

/**
 * Tests for presentable calendar.
 *
 * @author Michael Wodniok - Initial contribution.
 *
 * @author Andrew Fiddian-Green - Tests for Command Tag code
 *
 */
public class BiweeklyPresentableCalendarTest {
    private AbstractPresentableCalendar calendar;
    private AbstractPresentableCalendar calendar2;
    private AbstractPresentableCalendar calendar3;

    @BeforeEach
    public void setUp() throws IOException, CalendarException {
        calendar = new BiweeklyPresentableCalendar(new FileInputStream("src/test/resources/test.ics"));
        calendar2 = new BiweeklyPresentableCalendar(new FileInputStream("src/test/resources/test2.ics"));
        calendar3 = new BiweeklyPresentableCalendar(new FileInputStream("src/test/resources/test3.ics"));
    }

    /**
     * Tests recurrence and whether TimeZone is interpolated in a right way.
     */
    @Test
    public void testIsEventPresent() {
        // Test series
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-08T09:04:00Z")));
        assertTrue(calendar.isEventPresent(Instant.parse("2019-09-08T09:08:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-08T09:11:00Z")));

        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-09T09:04:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-09T09:08:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-09T09:11:00Z")));

        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-10T09:04:00Z")));
        assertTrue(calendar.isEventPresent(Instant.parse("2019-09-10T09:08:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-10T09:11:00Z")));

        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-11T09:04:00Z")));
        assertTrue(calendar.isEventPresent(Instant.parse("2019-09-11T09:08:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-11T09:11:00Z")));

        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-12T09:04:00Z")));
        assertTrue(calendar.isEventPresent(Instant.parse("2019-09-12T09:08:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-12T09:11:00Z")));

        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-13T09:04:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-13T09:08:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-13T09:11:00Z")));

        // Test in CEST (UTC+2)
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-14T07:59:00Z")));
        assertTrue(calendar.isEventPresent(Instant.parse("2019-09-14T08:03:00Z")));
        assertFalse(calendar.isEventPresent(Instant.parse("2019-09-14T09:01:00Z")));

        // Test Series with cancelled event by Davdroid
        assertFalse(calendar2.isEventPresent(Instant.parse("2019-11-03T09:55:00Z")));
        assertTrue(calendar2.isEventPresent(Instant.parse("2019-11-03T10:01:00Z")));
        assertFalse(calendar2.isEventPresent(Instant.parse("2019-11-03T13:00:00Z")));

        assertFalse(calendar2.isEventPresent(Instant.parse("2019-11-24T09:55:00Z")));
        assertFalse(calendar2.isEventPresent(Instant.parse("2019-11-24T10:01:00Z")));
        assertFalse(calendar2.isEventPresent(Instant.parse("2019-11-24T13:00:00Z")));
    }

    /**
     * This test relies on a working isEventPresent and assumes the calculation
     * of recurrence is done the same way.
     */
    @Test
    public void testGetCurrentEvent() {
        Event currentEvent = calendar.getCurrentEvent(Instant.parse("2019-09-10T09:07:00Z"));
        assertNotNull(currentEvent);
        assertTrue("Test Series in UTC".contentEquals(currentEvent.title));
        assertEquals(0, Instant.parse("2019-09-10T09:05:00Z").compareTo(currentEvent.start));
        assertEquals(0, Instant.parse("2019-09-10T09:10:00Z").compareTo(currentEvent.end));

        Event nonExistingEvent = calendar.getCurrentEvent(Instant.parse("2019-09-09T09:07:00Z"));
        assertNull(nonExistingEvent);
    }

    /**
     * This test relies on a working isEventPresent and assumes the calculation
     * of recurrence is done the same way.
     */
    @Test
    public void testGetNextEvent() {
        // positive case: next event of series
        Event nextEventOfSeries = calendar.getNextEvent(Instant.parse("2019-09-10T09:07:00Z"));
        assertNotNull(nextEventOfSeries);
        assertTrue("Test Series in UTC".contentEquals(nextEventOfSeries.title));
        assertEquals(0, Instant.parse("2019-09-11T09:05:00Z").compareTo(nextEventOfSeries.start));
        assertEquals(0, Instant.parse("2019-09-11T09:10:00Z").compareTo(nextEventOfSeries.end));

        // positive case: next event after series
        Event nextEventOutsideSeries = calendar.getNextEvent(Instant.parse("2019-09-12T09:07:00Z"));
        assertNotNull(nextEventOutsideSeries);
        assertTrue("Test Event in UTC+2".contentEquals(nextEventOutsideSeries.title));
        assertEquals(0, Instant.parse("2019-09-14T08:00:00Z").compareTo(nextEventOutsideSeries.start));
        assertEquals(0, Instant.parse("2019-09-14T09:00:00Z").compareTo(nextEventOutsideSeries.end));

        // positive case: next event should be also set if currently none is present
        Event nextEventIndependent = calendar.getNextEvent(Instant.parse("2019-09-13T09:07:00Z"));
        assertTrue(nextEventOutsideSeries.equals(nextEventIndependent));

        // negative case: after last event there is no next
        Event nonExistingEvent = calendar.getNextEvent(Instant.parse("2019-09-14T12:00:00Z"));
        assertNull(nonExistingEvent);

        // mixed case: cancelled events also not show up as next
        Event nextEventAfterCancelled = calendar2.getNextEvent(Instant.parse("2019-11-24T09:55:00Z"));
        assertNotNull(nextEventAfterCancelled);
        assertEquals(0, Instant.parse("2019-12-01T10:00:00Z").compareTo(nextEventAfterCancelled.start));
    }

    /**
     * This test checks for Events that have just begun or ended, and if so it checks for Command Tags
     * and checks if these tags are valid
     */
    @Test
    public void testCommandTagCode() {
        List<Event> events = null;
        int eventCount = 2;
        int tagsPerEvent = 8;

        // test just begun events: first in the series
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T15:55:00Z"),
                Instant.parse("2020-01-28T16:05:00Z"));
        assertNotNull(events);
        assertEquals(eventCount, events.size());
        for (Event event : events) {
            List<CommandTag> cmdTags = event.commandTags;
            assertEquals(tagsPerEvent, cmdTags.size());
            int beginTags = 0;
            for (CommandTag cmdTag : cmdTags) {
                if (cmdTag.getTagType() == CommandTagType.BEGIN) {
                    assertTrue(cmdTag.isAuthorized("abc"));
                    assertTrue(cmdTag.getItemName().matches("^\\w+$"));
                    assertTrue(cmdTag.getCommand() != null);
                    beginTags++;
                }
            }
            assertEquals(tagsPerEvent / 2, beginTags);
        }

        // test just begun events: third in the series
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-30T15:55:00Z"),
                Instant.parse("2020-01-30T16:05:00Z"));
        assertNotNull(events);
        assertEquals(eventCount, events.size());
        for (Event event : events) {
            List<CommandTag> cmdTags = event.commandTags;
            assertEquals(tagsPerEvent, cmdTags.size());
            int beginTags = 0;
            for (CommandTag cmdTag : cmdTags) {
                if (cmdTag.getTagType() == CommandTagType.BEGIN) {
                    assertTrue(cmdTag.isAuthorized("abc"));
                    assertTrue(cmdTag.getItemName().matches("^\\w+$"));
                    assertTrue(cmdTag.getCommand() != null);
                    beginTags++;
                }
            }
            assertEquals(tagsPerEvent / 2, beginTags);
        }

        // test outside of window: begun events, too early
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T15:50:00Z"),
                Instant.parse("2020-01-28T15:55:00Z"));
        assertNotNull(events);
        assertEquals(0, events.size());

        // test outside of window: begun events, too late
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T16:05:00Z"),
                Instant.parse("2020-01-28T16:10:00Z"));
        assertNotNull(events);
        assertEquals(0, events.size());

        // test just ended events: first in the series
        events = calendar3.getJustEndedEvents(Instant.parse("2020-01-28T16:25:00Z"),
                Instant.parse("2020-01-28T16:35:00Z"));
        assertNotNull(events);
        assertEquals(eventCount, events.size());
        for (Event event : events) {
            List<CommandTag> cmdTags = event.commandTags;
            assertEquals(tagsPerEvent, cmdTags.size());
            int endTags = 0;
            for (CommandTag cmdTag : cmdTags) {
                if (cmdTag.getTagType() == CommandTagType.END) {
                    assertTrue(cmdTag.isAuthorized("abc"));
                    assertTrue(cmdTag.getItemName().matches("^\\w+$"));
                    assertTrue(cmdTag.getCommand() != null);
                    endTags++;
                }
            }
            assertEquals(tagsPerEvent / 2, endTags);
        }

        // test outside of window: ended events, too early
        events = calendar3.getJustEndedEvents(Instant.parse("2020-01-28T16:20:00Z"),
                Instant.parse("2020-01-28T16:25:00Z"));
        assertNotNull(events);
        assertEquals(0, events.size());

        // test outside of window: ended events, too late
        events = calendar3.getJustEndedEvents(Instant.parse("2020-01-28T16:35:00Z"),
                Instant.parse("2020-01-28T16:40:00Z"));
        assertNotNull(events);
        assertEquals(0, events.size());

        // test a valid just begun event with both good and bad authorization codes
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T15:55:00Z"),
                Instant.parse("2020-01-28T16:05:00Z"));
        assertNotNull(events);
        assertTrue(events.size() > 0);
        List<CommandTag> cmdTags = events.get(0).commandTags;
        assertTrue(cmdTags.size() > 0);
        CommandTag cmd = cmdTags.get(0);
        // accept correct, empty or null configuration codes
        assertTrue(cmd.isAuthorized("abc"));
        assertTrue(cmd.isAuthorized(""));
        assertTrue(cmd.isAuthorized(null));
        // reject incorrect configuration code
        assertFalse(cmd.isAuthorized("123"));

        // test tag syntax: Test Series #1
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T19:25:00Z"),
                Instant.parse("2020-01-28T19:35:00Z"));
        assertNotNull(events);
        assertEquals(1, events.size());
        cmdTags = events.get(0).commandTags;
        assertEquals(11, cmdTags.size());

        // BEGIN:Calendar_Test_Color:ON:abc
        assertEquals("Calendar_Test_Color", cmdTags.get(0).getItemName());
        assertTrue(cmdTags.get(0).isAuthorized("abc"));
        Command cmd0 = cmdTags.get(0).getCommand();
        assertNotNull(cmd0);
        assertEquals(OnOffType.class, cmd0.getClass());

        // BEGIN:Calendar_Test_Contact:OPEN:abc
        Command cmd1 = cmdTags.get(1).getCommand();
        assertNotNull(cmd1);
        assertEquals(OpenClosedType.class, cmd1.getClass());

        // BEGIN:Calendar_Test_Dimmer:ON:abc
        Command cmd2 = cmdTags.get(2).getCommand();
        assertNotNull(cmd2);
        assertEquals(OnOffType.class, cmd2.getClass());

        // BEGIN:Calendar_Test_Number:12.3:abc
        Command cmd3 = cmdTags.get(3).getCommand();
        assertNotNull(cmd3);
        assertEquals(QuantityType.class, cmd3.getClass());

        // BEGIN:Calendar_Test_Temperature:12.3°C:abc
        Command cmd4 = cmdTags.get(4).getCommand();
        assertNotNull(cmd4);
        assertEquals(QuantityType.class, cmd4.getClass());

        // BEGIN:Calendar_Test_Pressure:12.3hPa:abc
        Command cmd5 = cmdTags.get(5).getCommand();
        assertNotNull(cmd5);
        assertEquals(QuantityType.class, cmd5.getClass());

        // BEGIN:Calendar_Test_Speed:12.3m/s:abc
        Command cmd6 = cmdTags.get(6).getCommand();
        assertNotNull(cmd6);
        assertEquals(QuantityType.class, cmd6.getClass());

        // BEGIN:Calendar_Test_Player:PLAY:abc
        Command cmd7 = cmdTags.get(7).getCommand();
        assertNotNull(cmd7);
        assertEquals(PlayPauseType.class, cmd7.getClass());

        // BEGIN:Calendar_Test_RollerShutter:UP:abc
        Command cmd8 = cmdTags.get(8).getCommand();
        assertNotNull(cmd8);
        assertEquals(UpDownType.class, cmd8.getClass());

        // BEGIN:Calendar_Test_String:Test Series #1:abc
        Command cmd9 = cmdTags.get(9).getCommand();
        assertNotNull(cmd9);
        assertEquals(StringType.class, cmd9.getClass());

        // BEGIN:Calendar_Test_Switch:ON:abc
        Command cmd10 = cmdTags.get(10).getCommand();
        assertNotNull(cmd10);
        assertEquals(OnOffType.class, cmd10.getClass());

        // test tag syntax: Test Series #4
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T20:10:00Z"),
                Instant.parse("2020-01-28T20:20:00Z"));
        assertNotNull(events);
        assertEquals(1, events.size());
        cmdTags = events.get(0).commandTags;
        assertEquals(11, cmdTags.size());

        // BEGIN:Calendar_Test_Color:0%:abc
        cmd0 = cmdTags.get(0).getCommand();
        assertNotNull(cmd0);
        assertEquals(PercentType.class, cmd0.getClass());

        // BEGIN:Calendar_Test_Contact:CLOSED:abc
        cmd1 = cmdTags.get(1).getCommand();
        assertNotNull(cmd1);
        assertEquals(OpenClosedType.class, cmd1.getClass());

        // BEGIN:Calendar_Test_Dimmer:0%:abc
        cmd2 = cmdTags.get(2).getCommand();
        assertNotNull(cmd2);
        assertEquals(PercentType.class, cmd2.getClass());

        // BEGIN:Calendar_Test_Number:-12.3:abc
        cmd3 = cmdTags.get(3).getCommand();
        assertNotNull(cmd3);
        assertEquals(QuantityType.class, cmd3.getClass());

        // BEGIN:Calendar_Test_Temperature:-12.3°C:abc
        cmd4 = cmdTags.get(4).getCommand();
        assertNotNull(cmd4);
        assertEquals(QuantityType.class, cmd4.getClass());

        // BEGIN:Calendar_Test_Pressure:500mmHg:abc
        cmd5 = cmdTags.get(5).getCommand();
        assertNotNull(cmd5);
        assertEquals(QuantityType.class, cmd5.getClass());

        // BEGIN:Calendar_Test_Speed:12300000mm/h:abc
        cmd6 = cmdTags.get(6).getCommand();
        assertNotNull(cmd6);
        assertEquals(QuantityType.class, cmd6.getClass());

        // BEGIN:Calendar_Test_Player:REWIND:abc
        cmd7 = cmdTags.get(7).getCommand();
        assertNotNull(cmd7);
        assertEquals(RewindFastforwardType.class, cmd7.getClass());

        // BEGIN:Calendar_Test_RollerShutter:100%:abc
        cmd8 = cmdTags.get(8).getCommand();
        assertNotNull(cmd8);
        assertEquals(PercentType.class, cmd8.getClass());

        // BEGIN:Calendar_Test_String:Test Series #4:abc
        cmd9 = cmdTags.get(9).getCommand();
        assertNotNull(cmd9);
        assertEquals(StringType.class, cmd9.getClass());

        // BEGIN:Calendar_Test_Switch:OFF:abc
        cmd10 = cmdTags.get(10).getCommand();
        assertNotNull(cmd10);
        assertEquals(OnOffType.class, cmd10.getClass());

        // test tag syntax: Test Series #5
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T20:25:00Z"),
                Instant.parse("2020-01-28T20:35:00Z"));
        assertNotNull(events);
        assertEquals(1, events.size());
        cmdTags = events.get(0).commandTags;
        assertEquals(11, cmdTags.size());

        // BEGIN:Calendar_Test_Color:240,100,100:abc
        cmd0 = cmdTags.get(0).getCommand();
        assertNotNull(cmd0);
        assertEquals(HSBType.class, cmd0.getClass());

        // BEGIN:Calendar_Test_Contact:OPEN:abc
        cmd1 = cmdTags.get(1).getCommand();
        assertNotNull(cmd1);
        assertEquals(OpenClosedType.class, cmd1.getClass());

        // BEGIN:Calendar_Test_Dimmer:50%:abc
        cmd2 = cmdTags.get(2).getCommand();
        assertNotNull(cmd2);
        assertEquals(PercentType.class, cmd2.getClass());

        // BEGIN:Calendar_Test_Number:-0:abc
        cmd3 = cmdTags.get(3).getCommand();
        assertNotNull(cmd3);
        assertEquals(QuantityType.class, cmd3.getClass());

        // BEGIN:Calendar_Test_Temperature:0K:abc
        cmd4 = cmdTags.get(4).getCommand();
        assertNotNull(cmd4);
        assertEquals(QuantityType.class, cmd4.getClass());

        // BEGIN:Calendar_Test_Pressure:12.3hPa:abc
        cmd5 = cmdTags.get(5).getCommand();
        assertNotNull(cmd5);
        assertEquals(QuantityType.class, cmd5.getClass());

        // BEGIN:Calendar_Test_Speed:12.3km/h:abc
        cmd6 = cmdTags.get(6).getCommand();
        assertNotNull(cmd6);
        assertEquals(QuantityType.class, cmd6.getClass());

        // BEGIN:Calendar_Test_Player:PLAY:abc
        cmd7 = cmdTags.get(7).getCommand();
        assertNotNull(cmd7);
        assertEquals(PlayPauseType.class, cmd7.getClass());

        // BEGIN:Calendar_Test_RollerShutter:50%:abc
        cmd8 = cmdTags.get(8).getCommand();
        assertNotNull(cmd8);
        assertEquals(PercentType.class, cmd8.getClass());

        // BEGIN:Calendar_Test_String:Test Series #5:abc
        cmd9 = cmdTags.get(9).getCommand();
        assertNotNull(cmd9);
        assertEquals(StringType.class, cmd9.getClass());

        // BEGIN:Calendar_Test_Switch:ON:abc
        cmd10 = cmdTags.get(10).getCommand();
        assertNotNull(cmd10);
        assertEquals(OnOffType.class, cmd10.getClass());

        // test bad command tag syntax: Test Series #6
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T20:40:00Z"),
                Instant.parse("2020-01-28T20:50:00Z"));
        assertNotNull(events);
        assertEquals(1, events.size());
        cmdTags = events.get(0).commandTags;
        // Test Series #6 contains only "bad" command tags as follows..

        // tags with wrong case prefix..
        // begin
        // Begin
        // BEGIn

        // tags that are missing ":" field delimiters..
        // BEGIN

        // tags with too few field delimiters..
        // BEGIN:www

        // tags with too many field delimiters..
        // BEGIN:www:xxx:yyy:zzz

        // tags with an invalid prefix..
        // BEGINX:xxx:yyy:zzz
        // ENDX:xxx:yyy:zzz
        // BEGIN :xxx:yyy:zzz
        // BEGIN :xxx:yyy:zzz

        // tags with an empty Item Name
        // BEGIN::yyy:zzz
        // BEGIN: :yyy:zzz
        // BEGIN: :yyy:zzz

        // tags with bad Item Name
        // BEGIN:!:yyy:zzz
        // BEGIN:@:yyy:zzz
        // BEGIN:£:yyy:zzz

        // tags with an empty Target State value
        // BEGIN:xxx::zzz
        // BEGIN:xxx: :zzz
        // BEGIN:xxx: :zzz

        // Note: All of the above tags must be rejected! => Assert cmdTags.size() == 0 !

        assertEquals(0, cmdTags.size());

        // test HTML command tag syntax: Test Series #7
        events = calendar3.getJustBegunEvents(Instant.parse("2020-01-28T20:55:00Z"),
                Instant.parse("2020-01-28T21:05:00Z"));
        assertNotNull(events);
        assertEquals(1, events.size());
        cmdTags = events.get(0).commandTags;
        assertEquals(8, cmdTags.size());

        // <p>BEGIN:Calendar_Test_Temperature:12.3°C:abc</p>
        cmd0 = cmdTags.get(0).getCommand();
        assertNotNull(cmd0);
        assertEquals(QuantityType.class, cmd0.getClass());

        // <p>END:Calendar_Test_Temperature:23.4°C:abc</p>
        cmd1 = cmdTags.get(1).getCommand();
        assertNotNull(cmd1);
        assertEquals(QuantityType.class, cmd1.getClass());

        // <p>BEGIN:Calendar_Test_Switch:ON:abc</p>
        cmd2 = cmdTags.get(2).getCommand();
        assertNotNull(cmd2);
        assertEquals(OnOffType.class, cmd2.getClass());

        // <p>END:Calendar_Test_Switch:OFF:abc</p>
        cmd3 = cmdTags.get(3).getCommand();
        assertNotNull(cmd3);
        assertEquals(OnOffType.class, cmd3.getClass());

        // <p>BEGIN:Calendar_Test_String:the quick:abc</p>
        cmd4 = cmdTags.get(4).getCommand();
        assertNotNull(cmd4);
        assertEquals(StringType.class, cmd4.getClass());

        // <p>END:Calendar_Test_String:brown fox:abc</p>
        cmd5 = cmdTags.get(5).getCommand();
        assertNotNull(cmd5);
        assertEquals(StringType.class, cmd5.getClass());

        // </p><p>BEGIN:Calendar_Test_Number:12.3:abc</p>
        cmd6 = cmdTags.get(6).getCommand();
        assertNotNull(cmd6);
        assertEquals(QuantityType.class, cmd6.getClass());

        // <p>END:Calendar_Test_Number:23.4:abc</p>
        cmd7 = cmdTags.get(7).getCommand();
        assertNotNull(cmd7);
        assertEquals(QuantityType.class, cmd7.getClass());
    }
}
