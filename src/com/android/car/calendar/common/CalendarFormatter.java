/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.calendar.common;

import static android.text.format.DateUtils.FORMAT_ABBREV_ALL;
import static android.text.format.DateUtils.FORMAT_NO_YEAR;
import static android.text.format.DateUtils.FORMAT_SHOW_TIME;

import android.content.Context;
import android.icu.text.DisplayContext;
import android.icu.text.RelativeDateTimeFormatter;
import android.icu.util.ULocale;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.Locale;

/** App specific text formatting utility. */
public class CalendarFormatter {
    private static final String TAG = "CarCalendarFormatter";
    private static final String SPACED_BULLET = " \u2022 ";
    private final Context mContext;
    private final Locale mLocale;
    private final DateFormat mDateFormat;
    private final ClockProvider mClockProvider;
    private final DateTimeFormatter mDateFormatter;

    public CalendarFormatter(Context context, Locale locale, ClockProvider
        clockProvider) {
        mContext = context;
        mLocale = locale;
        mClockProvider = clockProvider;

        String pattern =
                android.text.format.DateFormat.getBestDateTimePattern(mLocale, "EEE, d MMM");
        mDateFormat = new SimpleDateFormat(pattern, mLocale);
        mDateFormatter = DateTimeFormatter.ofPattern(pattern, mLocale);
    }

    /** Formats the given date to text. */
    public String getDateText(LocalDate localDate) {
        RelativeDateTimeFormatter formatter =
                RelativeDateTimeFormatter.getInstance(
                        ULocale.forLocale(mLocale),
                        null,
                        RelativeDateTimeFormatter.Style.LONG,
                        DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);

        LocalDate today = LocalDate.now(mClockProvider.get());
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfter = tomorrow.plusDays(1);

        String relativeDay = null;
        if (localDate.equals(today)) {
            relativeDay =
                    formatter.format(
                            RelativeDateTimeFormatter.Direction.THIS,
                            RelativeDateTimeFormatter.AbsoluteUnit.DAY);
        } else if (localDate.equals(tomorrow)) {
            relativeDay =
                    formatter.format(
                            RelativeDateTimeFormatter.Direction.NEXT,
                            RelativeDateTimeFormatter.AbsoluteUnit.DAY);
        } else if (localDate.equals(dayAfter)) {
            relativeDay =
                    formatter.format(
                            RelativeDateTimeFormatter.Direction.NEXT_2,
                            RelativeDateTimeFormatter.AbsoluteUnit.DAY);
        }

        StringBuilder result = new StringBuilder();
        if (relativeDay != null) {
            result.append(relativeDay);
            result.append(SPACED_BULLET);
        }

        ZonedDateTime zonedDateTime = localDate.atStartOfDay(mClockProvider.get().getZone());
        result.append(zonedDateTime.format(mDateFormatter));

        return result.toString();
    }

    /** Formats the given time to text. */
    public String getTimeRangeText(Instant start, Instant end) {
        Formatter formatter = new Formatter(new StringBuilder(50), mLocale);
        return DateUtils.formatDateRange(
                        mContext,
                        formatter,
                        start.toEpochMilli(),
                        end.toEpochMilli(),
                        FORMAT_SHOW_TIME | FORMAT_NO_YEAR | FORMAT_ABBREV_ALL,
                        mClockProvider.get().getZone().getId())
                .toString();
    }
}
