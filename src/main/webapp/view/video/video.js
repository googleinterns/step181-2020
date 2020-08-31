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

import Synchronizer from '../../synchronizer.js';
import TimestampUtil from '../../timestamp-util.js';

const SCRIPT = 'script';

/** Initializes and stores video player information. */
export default class Video {
  #lecture;
  #synchronizer;
  #eventController;
  #videoPlayer

  constructor(lecture, eventController) {
    this.#lecture = lecture;
    this.#eventController = eventController;
    this.#synchronizer = new Synchronizer(eventController);
  }

  /** Loads YouTube iFrame API. */
  async loadVideoApi() {
    window.onYouTubeIframeAPIReady = this.onYouTubeIframeAPIReady.bind(this);
    window.onPlayerReady = this.onPlayerReady.bind(this);
    const videoApiScript = document.createElement(SCRIPT);
    const firstScriptTag = document.getElementsByTagName(SCRIPT)[0];
    videoApiScript.src = 'https://www.youtube.com/iframe_api';
    firstScriptTag.parentNode.insertBefore(videoApiScript, firstScriptTag);
  }

  /**
   * Creates a YouTube Video iFrame that plays lecture video after
   * the API calls it. This is a required callback from the API.
   */
  // TODO: Support dynamic video height and width.
  onYouTubeIframeAPIReady() {
    this.#videoPlayer = new window.YT.Player('player', {
      height: '390',
      width: '640',
      videoId: this.#lecture.videoId,
      events: {
        onReady: window.onPlayerReady,
      },
    });
  }

  /** `event` plays the YouTube video. */
  onPlayerReady(event) {
    this.addSeekingListener();
    event.target.playVideo();
    this.#synchronizer.startVideoSyncTimer(
        this.getCurrentVideoTimeMs.bind(this));
  }

  /**
   * Adds event listener allowing seeking video
   * on event broadcast.
   */
  addSeekingListener() {
    this.#eventController.addEventListener((timeMs) => {
      this.seek(timeMs);
    }, 'seekAll');
  }

  /** Seeks video to `timeMs`. */
  seek(timeMs) {
    this.#videoPlayer.seekTo(
        TimestampUtil.millisecondsToSeconds(timeMs),
        /* allowSeekAhead= */ true);
  }

  /** Returns current video time in milliseconds. */
  getCurrentVideoTimeMs() {
    return TimestampUtil.secondsToMilliseconds(
        this.#videoPlayer.getCurrentTime());
  }
}
