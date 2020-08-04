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

package com.googleinterns.zoomtube.utils;

import static com.google.common.truth.Truth.assertThat;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googleinterns.zoomtube.data.Lecture;
import com.googleinterns.zoomtube.utils.LectureUtil;
import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class LectureUtilTest {
  // Needed for accessing datastore services while creating an Entity.
  private final LocalServiceTestHelper testServices = new LocalServiceTestHelper();

  @Before
  public void setUp() {
    testServices.setUp();
  }

  @After
  public void tearDown() {
    testServices.tearDown();
  }

  @Test
  public void createLecture_shouldReturnLectureFromEntity() throws IOException {
    Entity lectureEntity = new Entity(LectureUtil.KIND);
    lectureEntity.setProperty(LectureUtil.NAME, "testName");
    lectureEntity.setProperty(LectureUtil.VIDEO_URL, "testUrl");
    lectureEntity.setProperty(LectureUtil.VIDEO_ID, "testId");

    Lecture result = LectureUtil.createLecture(lectureEntity);

    assertThat(result.lectureName()).isEqualTo("testName");
    assertThat(result.videoUrl()).isEqualTo("testUrl");
    assertThat(result.videoId()).isEqualTo("testId");
  }

  @Test
  public void createEntity_shouldReturnEntityWithInputs() throws IOException {
    Entity result = LectureUtil.createEntity(
        /* lectureName= */ "testName", /* videoUrl= */ "testUrl", /* videoId= */ "testId");

    assertThat(result.getProperty(LectureUtil.NAME)).isEqualTo("testName");
    assertThat(result.getProperty(LectureUtil.VIDEO_URL)).isEqualTo("testUrl");
    assertThat(result.getProperty(LectureUtil.VIDEO_ID)).isEqualTo("testId");
  }
}
