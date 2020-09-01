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

package com.googleinterns.zoomtube.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

/** Contains data pertaining to a single transcript language. */
@GenerateTypeAdapter
@AutoValue
public abstract class TranscriptLanguage {
  /** Returns the name of the language. */
  public abstract String languageName();

  /** Returns the ISO language code for the language. */
  public abstract String languageCode();

  /** Returns the name of the language in English. */
  public abstract String languageNameInEnglish();

  /**
   * Returns a builder instance that can be used to create TranscriptLanguages.
   */
  public static Builder builder() {
    return new AutoValue_TranscriptLanguage.Builder();
  }

  /**
   * Returns a builder instance that can be used to create TranscriptLanguages.
   */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLanguageName(String languageName);
    public abstract Builder setLanguageCode(String languageCode);
    public abstract Builder setLanguageNameInEnglish(String languageNameInEnglish);
    public abstract TranscriptLanguage build();
  }
}
