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

import {timestampToString} from '../../timestamps.js';
import View from '../view.js';
import DiscussionArea from './discussion-area.js';

export const COMMENT_TYPE_REPLY = 'REPLY';
export const COMMENT_TYPE_QUESTION = 'QUESTION';
export const COMMENT_TYPE_NOTE = 'NOTE';

export let /** DiscussionArea */ discussion;

/**
 * Loads the lecture discussion.
 */
export async function intializeDiscussion() {
  discussion = new DiscussionArea(window.LECTURE);
  discussion.initialize();
  // This is used as the `onclick` handler of the new comment area submit
  // button. It must be set after discussion is initialized.
  window.postNewComment = discussion.postNewComment.bind(discussion);
}

/** Seeks discussion to `timeMs`. */
export function seekDiscussion(timeMs) {
  discussion.seek(timeMs);
}
