loadLectureList();

/**
 * Fetches data from servlet and sets it in the lecture selection portion.
 * Called whenever lecture selection page is loaded.
 */
async function loadLectureList() {
  const response = await fetch('/lecture');
  const jsonData = await response.json();

  const lectureList = document.getElementById('lecture-list');
  lectureList.innerHTML = '';
  for (const lecture of jsonData) {
    lectureList.appendChild(createLectureListItem(lecture));
  }
}

/**
 * Creates and returns a <li> containing an <a> linking to {@code
 * lecture.videoUrl} and the {@code lecture.lectureName}.
 */
function createLectureListItem(lecture) {
  const listItem = document.createElement('li');
  const lectureLink = document.createElement('a');

  // TODO: Redirect to specific lecture page.
  lectureLink.href = lecture.videoUrl;
  lectureLink.target = '_blank';
  lectureLink.innerText = lecture.lectureName;

  listItem.appendChild(lectureLink);
  return listItem;
}
