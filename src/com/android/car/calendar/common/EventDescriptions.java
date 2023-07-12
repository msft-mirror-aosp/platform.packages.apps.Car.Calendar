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

import static com.google.common.base.Verify.verifyNotNull;

import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import android.util.Log;
import com.android.car.calendar.common.Dialer.NumberAndAccess;

import com.google.common.base.Strings;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Utilities to manipulate the description of a calendar event which may contain meta-data.
 * <p>
 * This class will handle the extraction of phone numbers from the event description.
 * 1. Google meet style description, which carries a predefined google meet marker.
 * 2. Plain text numbers with more than 6 digits.
 * 3. Structured phone number with '<tel:numberandaccess>' tag.
 * <p>
 * Google Meet format:
 * -::~:~::~:~:~:~:~:~:~::-
 * ...
 * Or dial ... <tel:numberandaccess>
 * ...
 * -::~:~::~:~:~:~:~:~:~::-
 */
public class EventDescriptions {
    private static final String TAG = "EventDescriptions";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Requires a phone number to include only numbers, spaces and dash, optionally a leading "+".
    // The number must be at least 6 characters and can contain " " or "-" but end with a digit.
    // The access code must be at least 3 characters.
    // The number and the access to include "pin" or "code" between the numbers.
    private static final Pattern PHONE_PIN_PATTERN =
        Pattern.compile(
            "(\\+?[\\d -]{6,}\\d)(?:.*\\b(?:PIN|code)\\b.*?([\\d,;#*]{3,}))?",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    // Hardcoded pattern from Google Meet.
    private static final String GOOGLE_MEET_MARKER =
        "-::~:~::~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~::~:~::-";
    private static final Pattern GOOGLE_MEET_SECTION_PATTERN =
        Pattern.compile(
            String.format("(.*)%s(.+)%s(.*)", GOOGLE_MEET_MARKER, GOOGLE_MEET_MARKER),
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern GOOGLE_MEET_INLINE_DIAL_PATTERN = Pattern.compile(
        "Or dial(.+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // Matches numbers in the encoded format "<tel: ... >".
    private static final Pattern TEL_PIN_PATTERN =
            Pattern.compile("<tel:(\\+?[\\d -]{6,})([\\d,;#*]{3,})?>");

    // Ensure numbers are over 5 digits to reduce false positives.
    private static final int MIN_DIGITS = 5;

    private final String mCountryIso;

    public EventDescriptions(Locale locale, TelephonyManager telephonyManager) {
        String networkCountryIso = telephonyManager.getNetworkCountryIso().toUpperCase();
        if (!Strings.isNullOrEmpty(networkCountryIso)) {
            mCountryIso = networkCountryIso;
        } else {
            mCountryIso = locale.getCountry();
        }
    }

    /** Find conference call data embedded in the description. */
    public List<NumberAndAccess> extractNumberAndPins(String descriptionText) {
        String decoded = Uri.decode(descriptionText);
        // Use a map keyed by number to act like a set and only add a single number.
        LinkedHashMap<String, NumberAndAccess> results = new LinkedHashMap<>();
        // Google Meet section has a different pattern.
        String nonGoogleMeetSections = handleGoogleMeetNumbers(decoded, results);
        if (!results.isEmpty()) {
            return ImmutableList.copyOf(results.values()).reverse();
        }

        // Add the most restrictive precise format last to replace others with the same number.
        addMatchedNumbers(nonGoogleMeetSections, results, PHONE_PIN_PATTERN);
        addMatchedNumbers(nonGoogleMeetSections, results, TEL_PIN_PATTERN);

        // Reverse order so the most precise format is first.
        return ImmutableList.copyOf(results.values()).reverse();
    }

    private String handleGoogleMeetNumbers(String decoded, LinkedHashMap<String,
        NumberAndAccess> results) {
        Matcher meetSectionMatcher = GOOGLE_MEET_SECTION_PATTERN.matcher(decoded);
        if (!meetSectionMatcher.find() || meetSectionMatcher.group(2) == null) {
            return decoded;
        }
        // Finds out a phone number from description if available.
        String meetSection = Objects.requireNonNullElse(meetSectionMatcher.group(2), "");
        Matcher inlineMeetDialMatcher = GOOGLE_MEET_INLINE_DIAL_PATTERN.matcher(meetSection);
        if (inlineMeetDialMatcher.find()) {
            addMatchedNumbers(inlineMeetDialMatcher.group(1), results, PHONE_PIN_PATTERN);
        }
        return Objects.requireNonNullElse(meetSectionMatcher.group(1), "") +
            Objects.requireNonNullElse(meetSectionMatcher.group(3), "");
    }

    private void addMatchedNumbers(String decoded, LinkedHashMap<String, NumberAndAccess> results,
        Pattern phonePinPattern) {
        Matcher phoneFormatMatcher = phonePinPattern.matcher(decoded);
        while (phoneFormatMatcher.find()) {
            NumberAndAccess numberAndAccess = validNumberAndAccess(phoneFormatMatcher);
            if (numberAndAccess != null) {
                String number = numberAndAccess.getNumber();
                // First delete this key, so that it can be at the tail.
                results.remove(number);
                results.put(number, numberAndAccess);
            }
        }
    }

    @Nullable
    private NumberAndAccess validNumberAndAccess(Matcher phoneFormatMatcher) {
        String number = verifyNotNull(phoneFormatMatcher.group(1));
        String access = phoneFormatMatcher.group(2);
        // Clear special characters that might be generated by some phone Calendar apps.
        if (access != null) {
            access = access.replaceAll("[;,]", "");
        }
        // Ensure that there are a minimum number of digits to reduce false positives.
        String onlyDigits = number.replaceAll("\\D", "");
        if (onlyDigits.length() < MIN_DIGITS) {
            return null;
        }

        // Keep local numbers in local format which the dialer can make more sense of.
        String formatted = PhoneNumberUtils.formatNumber(number, mCountryIso);
        if (formatted == null) {
            if (DEBUG) {
                Log.d(TAG, "validNumberAndAccess: cannot format the number: " + number);
            }
            return null;
        }
        return new NumberAndAccess(formatted, access);
    }
}
