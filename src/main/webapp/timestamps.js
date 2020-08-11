// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

const MINUTES_TO_SECONDS = 60;
const HOURS_TO_SECONDS = 360;

/**
 * Coverts a Date representing a video {@code timestamp} into a string.
 */
function timestampToString(timestamp) {
  const date = new Date(timestamp);
  const seconds = date.getSeconds().toString().padStart(
      /* targetLength= */ 2, /* padString= */ '0');
  const minutes = date.getMinutes().toString().padStart(
      /* targetLength= */ 2, /* padString= */ '0');
  if (date.getHours() == 0) {
    return `${minutes}:${seconds}`;
  }
  // We don't pad hours because lectures won't need two digits for hours.
  const hours = date.getHours().toString();
  return `${hours}:${minutes}:${seconds}`;
}

/**
 * Returns the total number of seconds since the start of a video at a
 * certain {@code timestamp}.
 */
function timestampToSeconds(timestamp) {
  const date = new Date(timestamp);
  const totalMilliseconds = date.getTime();
  const totalSeconds = totalMilliseconds / 1000;
  return totalSeconds;
}

/**
 * @return {number} The {@code date} in seconds.
 */
// TODO: Remove this method once the pull request updating
// Date to long is approved.
function getDateInSeconds(date) {
  return date.getSeconds() + date.getMinutes() * MINUTES_TO_SECONDS +
      date.getHours() * HOURS_TO_SECONDS;
}
