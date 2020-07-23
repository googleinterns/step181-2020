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

/* Used to control video. */
let videoPlayer;

/** Loads YouTube iFrame API. */
async function loadApi() {
  const videoApiScript = document.createElement('script');
  const firstScriptTag = document.getElementsByTagName('script')[0];
  videoApiScript.src = 'https://www.youtube.com/iframe_api';
  firstScriptTag.parentNode.insertBefore(videoApiScript, firstScriptTag);
}

/**
 * Creates a YouTube Video iFrame playing video with id:{@code VIDEO_ID} after
 * API is loaded.
 */
function onYouTubeIframeAPIReady() {
  videoPlayer = new window.YT.Player('player', {
    height: '390',
    width: '640',
    videoId: window.VIDEO_ID,
    events: {
      onReady: onPlayerReady,
    },
  });
}

/** Plays video. Called after API and iFrame load. */
function onPlayerReady(event) {
  event.target.playVideo();
}
