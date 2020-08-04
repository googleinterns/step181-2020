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
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googleinterns.zoomtube.data.TranscriptLine;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.w3c.dom.Element;

@RunWith(JUnit4.class)
public final class TranscriptLineUtilTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();
  @Mock private Element node;

  private LocalDatastoreServiceTestConfig datastoreConfig =
      (new LocalDatastoreServiceTestConfig()).setNoStorage(true);
  private final LocalServiceTestHelper localServiceHelper =
      new LocalServiceTestHelper(datastoreConfig);
  private static final String TEST_CONTENT = "test content";

  @Before
  public void setUp() {
    localServiceHelper.setUp();
  }

  @After
  public void tearDown() {
    localServiceHelper.tearDown();
  }

  @Test
  public void fromEntity_transcriptLineSuccessfullyCreated() throws IOException {
    Date testDate = new Date();
    Key testLectureKey = KeyFactory.createKey(TranscriptLineUtil.PARAM_LECTURE, "Test Key Id");
    Entity lineEntity = new Entity(TranscriptLineUtil.ENTITY_KIND);
    lineEntity.setProperty(TranscriptLineUtil.LECTURE, testLectureKey);
    lineEntity.setProperty(TranscriptLineUtil.CONTENT, TEST_CONTENT);
    lineEntity.setProperty(TranscriptLineUtil.START, testDate);
    lineEntity.setProperty(TranscriptLineUtil.DURATION, testDate);
    lineEntity.setProperty(TranscriptLineUtil.END, testDate);

    TranscriptLine actualLine = TranscriptLineUtil.fromEntity(lineEntity);

    assertThat(actualLine.lectureKey()).isEqualTo(testLectureKey);
    assertThat(actualLine.start()).isEqualTo(testDate);
    assertThat(actualLine.duration()).isEqualTo(testDate);
    assertThat(actualLine.end()).isEqualTo(testDate);
    assertThat(actualLine.content()).isEqualTo(TEST_CONTENT);
  }

  @Test
  public void createEntity_entityAndPropertiesSuccessfullyCreated() throws IOException {
    String dateAsString = "23.32";
    long dateAsLong = (long) 23.32;
    long lectureId = 1;
    when(node.getTextContent()).thenReturn(TEST_CONTENT);
    when(node.getAttribute(TranscriptLineUtil.ATTR_START)).thenReturn(dateAsString);
    when(node.getAttribute(TranscriptLineUtil.ATTR_DURATION)).thenReturn(dateAsString);

    Entity actualEntity = TranscriptLineUtil.createEntity(node, lectureId);
    Key actualKey = KeyFactory.createKey(TranscriptLineUtil.PARAM_LECTURE, lectureId);
    Date actualDate = new Date(TimeUnit.SECONDS.toMillis(dateAsLong));
    Date actualStartPlusDurationDate = new Date(TimeUnit.SECONDS.toMillis(dateAsLong * 2));

    assertThat(actualEntity.getProperty(TranscriptLineUtil.LECTURE)).isEqualTo(actualKey);
    assertThat(actualEntity.getProperty(TranscriptLineUtil.CONTENT)).isEqualTo(TEST_CONTENT);
    assertThat(actualEntity.getProperty(TranscriptLineUtil.START)).isEqualTo(actualDate);
    assertThat(actualEntity.getProperty(TranscriptLineUtil.DURATION)).isEqualTo(actualDate);
    // The end time is calculated as start time + duration.
    assertThat(actualEntity.getProperty(TranscriptLineUtil.END))
        .isEqualTo(actualStartPlusDurationDate);
  }
}