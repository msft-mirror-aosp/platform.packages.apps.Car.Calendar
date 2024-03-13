/*
 * Copyright 2023 The Android Open Source Project
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

import java.time.Clock;
import java.time.ZoneId;
import java.util.function.Supplier;

/** Factory that provides desired clock. */
public class ClockProviderFactory {

  public static ClockProvider systemClockProvider() {
    return () -> Clock.system(ZoneId.systemDefault());
  }

  public static ClockProvider fixedClockProvider(Clock clock) {
    return () -> clock;
  }
}