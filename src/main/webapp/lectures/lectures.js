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

const ENDPOINT_LECTURE_LIST = '/lecture-list';
const ENDPOINT_TRANSCRIPT_LANGUAGES = '/transcript-language';
const LANGUAGE_SELECTOR_CONTAINER = 'language-selector';
const NO_LANGUAGES_AVAILABLE_MESSAGE =
    'Sorry, there is no transcript available for this lecture.';
const SELECT_LANGUAGE_OPTION_MESSAGE =
    'Select the language for the transcript.';
const INPUT_LANGUAGE = 'language-input';

/* Used to gather URL parameters. */
const PARAM_ID = 'id';

const REDIRECT_PAGE = '/view/';

loadLectureList();

/**
 * Fetches avaiable Lectures from `ENDPOINT_LECTURE_LIST`
 * and sets them in the lecture selection page.
 */
async function loadLectureList() {
  const response = await fetch(ENDPOINT_LECTURE_LIST);
  const jsonData = await response.json();

  const lectureList = document.getElementById('lecture-list');
  lectureList.innerHTML = '';
  for (const lecture of jsonData) {
    lectureList.appendChild(createLectureListItem(lecture));
  }
}

/**
 * Creates and returns a `<li>` containing an `<a>` linking to
 * `lecture`'s view page.
 */
function createLectureListItem(lecture) {
  const lectureLink = document.createElement('a');

  const url = new URL(REDIRECT_PAGE, window.location.origin);
  url.searchParams.append(PARAM_ID, lecture.key.id);
  lectureLink.href = url;
  lectureLink.className = 'list-group-item list-group-item-action';

  lectureLink.innerText = lecture.lectureName;

  return lectureLink;
}

/**
 * Fetches and displays the list of available transcript languages.
 *
 * @param {Element} videoLinkInputElement The input element containing the video
 *     link.
 */
async function fetchAndDisplayTranscriptLanguages(videoLinkInputElement) {
  const languageSelectorDivElement =
      document.getElementById(LANGUAGE_SELECTOR_CONTAINER);
  languageSelectorDivElement.innerHTML = '';
  const url = new URL(ENDPOINT_TRANSCRIPT_LANGUAGES, window.location.origin);
  url.searchParams.append(
      videoLinkInputElement.name, videoLinkInputElement.value);
  let languagesJson;
  try {
    const languagesResponse = await fetch(url);
    languagesJson = await languagesResponse.json();
  } catch (error) {
    return;
  }
  displayLanguages(languagesJson);
}

/**
 * Adds each language in `languages` as an element in a
 * drop-down selector.
 */
function displayLanguages(languages) {
  const languageSelectorDivElement =
      document.getElementById(LANGUAGE_SELECTOR_CONTAINER);
  if (languages.length == 0) {
    languageSelectorDivElement.innerText = NO_LANGUAGES_AVAILABLE_MESSAGE;
    return;
  }
  const languageSelectElement = document.createElement('select');
  languageSelectElement.name = INPUT_LANGUAGE;
  languageSelectorDivElement.appendChild(languageSelectElement);
  languageSelectElement.appendChild(createDefaultDisabledLanguageOption());
  for (const language of languages) {
    const languageOptionElement = document.createElement('option');
    languageOptionElement.value = language.languageCode;
    languageOptionElement.innerText = language.languageNameInEnglish;
    languageSelectElement.appendChild(languageOptionElement);
  }
}

/**
 * Creates a disabled default option, which contains a message telling users
 * to select a language.
 */
function createDefaultDisabledLanguageOption() {
  const defaultLanguageOptionElement = document.createElement('option');
  defaultLanguageOptionElement.disabled = true;
  defaultLanguageOptionElement.selected = true;
  defaultLanguageOptionElement.innerText = SELECT_LANGUAGE_OPTION_MESSAGE;
  return defaultLanguageOptionElement;
}

window.fetchAndDisplayTranscriptLanguages = fetchAndDisplayTranscriptLanguages;
