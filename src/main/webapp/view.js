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

const PARAM_ID = 'id';
const PARAM_VIDEO_ID = 'video-id';

var timer;
var lastTime;

/* exported LECTURE_ID */
window.LECTURE_ID = getLectureId();
window.VIDEO_ID = getVideoId();

initialize();

/** Initializes the video player for the lecture view page. */
async function initialize() {
<<<<<<< HEAD
  timer = window.setInterval(getCurrentTime, 1000);
  window.loadVideoApi();
}

function getCurrentTime() {
  const currentTime = videoPlayer.getCurrentTime();
  sync(currentTime, false);
=======
  window.loadDiscussion();
  window.sendPostToTranscript();
  // TODO: Initialize the video and trancript sections once they are added.
>>>>>>> origin/transcript-dom-style
}

function sync(currentTime, syncVideo) {
  if (lastTime == currentTime) {
    return;
  }
  lastTime = currentTime;
  if (syncVideo) { seekVideo(currentTime); }
  seekDiscussion(currentTime);
  // seekTranscript(currentTime);
}
/**
 * Returns the lecture id obtained from the current page's URL parameters.
 */
function getLectureId() {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(PARAM_ID);
}

/**
 * Returns the video id obtained from the current page's URL parameters.
 */
function getVideoId() {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(PARAM_VIDEO_ID);
}
